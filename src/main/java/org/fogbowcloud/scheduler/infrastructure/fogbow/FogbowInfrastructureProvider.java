package org.fogbowcloud.scheduler.infrastructure.fogbow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.PassingLogRecordFilter;
import org.fogbowcloud.manager.core.plugins.identity.voms.VomsIdentityPlugin;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.fogbowcloud.scheduler.core.http.HttpWrapper;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.AppUtil;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public class FogbowInfrastructureProvider implements InfrastructureProvider {

	private static final int MEMORY_1Gbit = 1024;

	private static final Logger LOGGER = Logger.getLogger(FogbowInfrastructureProvider.class);

	// ------------------ CONSTANTS ------------------//
	private static final String NULL_VALUE = "null";
	private static final String CATEGORY = "Category";
	private static final String X_OCCI_ATTRIBUTE = "X-OCCI-Attribute";
	private static final String DEFAULT_USER = "user";

	public static final String REQUEST_ATTRIBUTE_MEMBER_ID = "org.fogbowcloud.request.providing-member";

	public static final String INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT = "org.fogbowcloud.request.ssh-public-address";
	public static final String INSTANCE_ATTRIBUTE_SSH_USERNAME_ATT = "org.fogbowcloud.request.ssh-username";
	public static final String INSTANCE_ATTRIBUTE_EXTRA_PORTS_ATT = "org.fogbowcloud.request.extra-ports";
	public static final String INSTANCE_ATTRIBUTE_MEMORY_SIZE = "occi.compute.memory";
	public static final String INSTANCE_ATTRIBUTE_VCORE = "occi.compute.cores";
	// TODO Alter when fogbow are returning this attribute
	public static final String INSTANCE_ATTRIBUTE_DISKSIZE = "TODO-AlterWhenFogbowReturns"; 
	public static final String INSTANCE_ATTRIBUTE_REQUEST_TYPE = "org.fogbowcloud.request.type";

	// ------------------ ATTRIBUTES -----------------//
	private HttpWrapper httpWrapper;
	private String managerUrl;
	private Token token;
	private Properties properties;
	private Map<String, Specification> pendingRequestsMap = new HashMap<String, Specification>();

	public FogbowInfrastructureProvider(Properties properties) throws Exception {

		httpWrapper = new HttpWrapper();
		this.properties = properties;
		this.managerUrl = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL);
		this.token = createNewTokenFromFile(
				properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH));

		ScheduledExecutorService handleTokenUpdateExecutor = Executors.newScheduledThreadPool(1);
		handleTokenUpdate(handleTokenUpdateExecutor, properties);
	}	
	
	private void handleTokenUpdate(ScheduledExecutorService handleTokenUpdateExecutor,
			final Properties props) {
		LOGGER.debug("Turning on handle token update.");
		handleTokenUpdateExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					setToken(createToken(props));
				} catch (Throwable e) {
					LOGGER.error("Error while setting token.", e);
					try {
						setToken(createNewTokenFromFile(
								properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH)));
					} catch (IOException e1) {
						LOGGER.error("Error while getting token from file.", e);
					}
				}
			}
		}, 6, 6, TimeUnit.HOURS);
	}

	private Token createToken(final Properties props) {
		VomsIdentityPlugin vomsIdentityPlugin = new VomsIdentityPlugin(new Properties());

		HashMap<String, String> credentials = new HashMap<String, String>();
		credentials.put("password", props.getProperty("fogbow.voms.certificate.password"));
		credentials.put("serverName", props.getProperty("fogbow.voms.server"));
		LOGGER.debug("Creating token update with serverName="
				+ props.getProperty("fogbow.voms.server") + " and password="
				+ props.getProperty("fogbow.voms.certificate.password"));

		Token token = vomsIdentityPlugin.createToken(credentials);
		LOGGER.debug("VOMS proxy updated. New proxy is " + token.toString());

		return token;
	}

	private void setToken(Token token) {
		LOGGER.debug("Setting token to " + token);
		this.token = token;
	}
	
	@Override
	public String requestResource(Specification spec) throws RequestResourceException {

		LOGGER.debug("Requesting resource on Fogbow with specifications: " + spec.toString());

		String requestInformation;

		try {

			this.validateSpecification(spec);

			List<Header> headers = (LinkedList<Header>) requestNewInstanceHeaders(spec);

			requestInformation = this.doRequest("post", managerUrl + "/" + RequestConstants.TERM, headers);

		} catch (Exception e) {
			LOGGER.error("Error while requesting resource on Fogbow", e);
			throw new RequestResourceException("Request for Fogbow Resource has FAILED: " + e.getMessage(), e);
		}
		String requestId = getRequestId(requestInformation);
		pendingRequestsMap.put(requestId, spec);

		LOGGER.debug("Request for Fogbow Resource was Successful. Request ID: [" + requestId + "]");
		return requestId;
	}

	@Override
	public Resource getResource(String requestID) {

		LOGGER.debug("Getting resource from request id: [" + requestID + "]");

		Resource resource = getFogbowResource(requestID);

		if (resource == null) {
			return null;
		}
//		// Testing connection.
//		if (!resource.checkConnectivity()) {
//			LOGGER.info("Resource from request id: [" + requestID + "] is not responding to connection attempt");
//			return null;
//		}

		pendingRequestsMap.remove(requestID);

		LOGGER.debug(
				"Returning Resource from request id: [" + requestID + "] - Resource ID : [" + resource.getId() + "]");
		return resource;

	}

	protected Resource getFogbowResource(String requestID) {

		LOGGER.debug("Initiating Resource Instanciation - Request id: [" + requestID + "]");
		String instanceId;
		String sshInformation;
		Map<String, String> requestAttributes;
		Resource resource = null;

		try {
			LOGGER.debug("Getting request attributes - Retrieve Instace ID.");
			// Attempt's to get the Instance ID from Fogbow Manager.
			requestAttributes = getFogbowRequestAttributes(requestID);

			instanceId = requestAttributes.get(RequestAttribute.INSTANCE_ID.getValue());

			// If has Instance ID, then verifies resource's SSH and Other
			// Informations;
			if (instanceId != null && !instanceId.isEmpty()) {
				LOGGER.debug("Instance ID returned: "+instanceId);

				// Recovers Instance's attributes.
				Map<String, String> instanceAttributes = getFogbowInstanceAttributes(instanceId);

				if (this.validateInstanceAttributes(instanceAttributes)) {
					
					Specification spec = pendingRequestsMap.get(requestID);
					resource = new Resource(requestID, properties);
					
					String requestType = spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE);

					LOGGER.debug("Getting Instance attributes.");
					// Putting all metadatas on Resource
					sshInformation = instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT);

					String[] addressInfo = sshInformation.split(":");
					String host = addressInfo[0];
					String port = addressInfo[1];

					resource.putMetadata(Resource.METADATA_REQUEST_TYPE, requestType);
					resource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
					resource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
					resource.putMetadata(Resource.METADATA_SSH_HOST, host);
					resource.putMetadata(Resource.METADATA_SSH_PORT, port);
					resource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT,
							instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_USERNAME_ATT));
					resource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT,
							instanceAttributes.get(INSTANCE_ATTRIBUTE_EXTRA_PORTS_ATT));
					resource.putMetadata(Resource.METADATA_VCPU, instanceAttributes.get(INSTANCE_ATTRIBUTE_VCORE));
					float menSize = Float.parseFloat(instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMORY_SIZE));
					String menSizeFormated = String.valueOf(menSize*MEMORY_1Gbit);
					resource.putMetadata(Resource.METADATA_MEN_SIZE,menSizeFormated);
					resource.putMetadata(Resource.METADATA_LOCATION, "\"" + requestAttributes.get(REQUEST_ATTRIBUTE_MEMBER_ID) + "\"");
					// TODO Descomentar quando o fogbow estiver retornando este
					// atributo
					// newResource.putMetadata(Resource.METADATA_DISK_SIZE,
					// instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE));

					LOGGER.debug("New Fogbow Resource created - Instace ID: [" + instanceId + "]");
				}else{
					LOGGER.debug("Instance attributes not yet ready for instance: ["+instanceId+"]");
					return null;
				}

			}

		} catch (Exception e) {

			LOGGER.error("Error while getting resource from request id: [" + requestID + "]", e);
			resource = null;
		}
		return resource;
	}

	@Override
	public void deleteResource(String resourceId) throws InfrastructureException {
		try {
			Map<String, String> requestAttributes = getFogbowRequestAttributes(resourceId);

			String instanceId = requestAttributes.get(RequestAttribute.INSTANCE_ID.getValue());
			if (instanceId != null) {
				this.doRequest("delete", managerUrl + "/compute/" + instanceId, new ArrayList<Header>());
			}
			this.doRequest("delete", managerUrl + "/" + RequestConstants.TERM + "/" + resourceId,
					new ArrayList<Header>());

		} catch (Exception e) {
			throw new InfrastructureException("Error when trying to delete resource id[" + resourceId + "]", e);
		}
	}

	// ----------------------- Private methods ----------------------- //

	private Token createNewTokenFromFile(String certificateFilePath) throws FileNotFoundException, IOException {

		String certificate = IOUtils.toString(new FileInputStream(certificateFilePath)).replaceAll("\n", "");
		Date date = new Date(System.currentTimeMillis() + (long) Math.pow(10, 9));

		return new Token(certificate, DEFAULT_USER, date, new HashMap<String, String>());
	}

	private Map<String, String> getFogbowRequestAttributes(String requestId) throws Exception {

		String endpoint = managerUrl + "/" + RequestConstants.TERM + "/" + requestId;
		String requestResponse = doRequest("get", endpoint, new ArrayList<Header>());

		Map<String, String> attrs = parseRequestAttributes(requestResponse);
		return attrs;
	}

	private Map<String, String> getFogbowInstanceAttributes(String instanceId) throws Exception {

		String endpoint = managerUrl + "/compute/" + instanceId;
		String instanceInformation = doRequest("get", endpoint, new ArrayList<Header>());

		Map<String, String> attrs = parseAttributes(instanceInformation);
		return attrs;
	}

	private void validateSpecification(Specification specification) throws RequestResourceException {

		if (specification.getImage() == null || specification.getImage().isEmpty()) {

			throw new RequestResourceException("");
		}
		if (specification.getPublicKey() == null || specification.getPublicKey().isEmpty()) {

			throw new RequestResourceException("");
		}

		String fogbowRequirements = specification
				.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);

		if (!FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirements)) {
			LOGGER.debug("FogbowRequirements [" + fogbowRequirements
					+ "] is not in valid format. e.g: [Glue2vCPU >= 1 && Glue2RAM >= 1024 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"]");
			throw new RequestResourceException("FogbowRequirements [" + fogbowRequirements
					+ "] is not in valid format. e.g: [Glue2vCPU >= 1 && Glue2RAM >= 1024 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"]");
		}

	}

	private List<Header> requestNewInstanceHeaders(Specification specs) {

		String fogbowImage = specs.getImage();
		String fogbowRequirements = specs.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);
		String fogbowRequestType = specs.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE);

		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader(CATEGORY, RequestConstants.TERM + "; scheme=\"" + RequestConstants.SCHEME
				+ "\"; class=\"" + RequestConstants.KIND_CLASS + "\""));
		headers.add(new BasicHeader(X_OCCI_ATTRIBUTE, RequestAttribute.INSTANCE_COUNT.getValue() + "=" + 1));
		headers.add(new BasicHeader(X_OCCI_ATTRIBUTE, RequestAttribute.TYPE.getValue() + "=" + fogbowRequestType));
		headers.add(
				new BasicHeader(X_OCCI_ATTRIBUTE, RequestAttribute.REQUIREMENTS.getValue() + "=" + fogbowRequirements));
		headers.add(new BasicHeader(CATEGORY, fogbowImage + "; scheme=\"" + RequestConstants.TEMPLATE_OS_SCHEME
				+ "\"; class=\"" + RequestConstants.MIXIN_CLASS + "\""));
		if (specs.getPublicKey() != null && !specs.getPublicKey().isEmpty()) {
			headers.add(
					new BasicHeader(CATEGORY,
							RequestConstants.PUBLIC_KEY_TERM + "; scheme=\""
									+ RequestConstants.CREDENTIALS_RESOURCE_SCHEME + "\"; class=\""
									+ RequestConstants.MIXIN_CLASS + "\""));
			headers.add(new BasicHeader(X_OCCI_ATTRIBUTE,
					RequestAttribute.DATA_PUBLIC_KEY.getValue() + "=" + specs.getPublicKey()));
		}
		return headers;

	}

	private String doRequest(String method, String endpoint, List<Header> headers) throws Exception {
		return httpWrapper.doRequest(method, endpoint, token.getAccessId(), headers);
	}

	private String getRequestId(String requestInformation) {
		String[] requestRes = requestInformation.split(":");
		String[] requestId = requestRes[requestRes.length - 1].split("/");
		return requestId[requestId.length - 1];
	}

	private boolean validateInstanceAttributes(Map<String, String> instanceAttributes) {

		LOGGER.debug("Validating instance attributes.");
		
		boolean isValid = true;

		if (instanceAttributes != null && !instanceAttributes.isEmpty()) {

			String sshInformation = instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT);
			String vcore = instanceAttributes.get(INSTANCE_ATTRIBUTE_VCORE);
			String memorySize = instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMORY_SIZE);
			String diskSize = instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE);
			String memberId = instanceAttributes.get(REQUEST_ATTRIBUTE_MEMBER_ID);

			// If any of these attributes are empty, then return invalid.
			//TODO: add to "isStringEmpty diskSize and memberId when fogbow being returning this two attributes.
			isValid = !AppUtil.isStringEmpty(sshInformation, vcore,
					memorySize );
			if(!isValid){
				LOGGER.debug("Instance attributes invalids.");
				return false;
			}

			String[] addressInfo = sshInformation.split(":");
			if (addressInfo != null && addressInfo.length > 1) {
				String host = addressInfo[0];
				String port = addressInfo[1];
				isValid = !AppUtil.isStringEmpty(host, port);
			}else{
				LOGGER.debug("Instance attributes invalids.");
				isValid = false;
			}

		} else {
			LOGGER.debug("Instance attributes invalids.");
			isValid = false;
		}

		return isValid;
	}

	private Map<String, String> parseRequestAttributes(String response) {
		Map<String, String> atts = new HashMap<String, String>();
		for (String responseLine : response.split("\n")) {
			if (responseLine.contains(X_OCCI_ATTRIBUTE+": ")) {
				String[] responseLineSplit = responseLine.substring((X_OCCI_ATTRIBUTE+": ").length()).split("=");
				String valueStr = responseLineSplit[1].trim().replace("\"", "");
				if (!valueStr.equals(NULL_VALUE)) {
					atts.put(responseLineSplit[0].trim(), valueStr);
				}
			}
		}
		return atts;
	}

	private Map<String, String> parseAttributes(String response) {
		Map<String, String> atts = new HashMap<String, String>();
		for (String responseLine : response.split("\n")) {
			if (responseLine.contains(X_OCCI_ATTRIBUTE+": ")) {
				String[] responseLineSplit = responseLine.substring((X_OCCI_ATTRIBUTE+": ").length()).split("=");
				String valueStr = responseLineSplit[1].trim().replace("\"", "");
				if (!valueStr.equals(NULL_VALUE)) {
					atts.put(responseLineSplit[0].trim(), valueStr);
				}
			}
		}
		return atts;
	}

	private RequestState getRequestState(String requestValue) {
		for (RequestState state : RequestState.values()) {
			if (state.getValue().equals(requestValue)) {
				return state;
			}
		}
		return null;
	}

	public HttpWrapper getHttpWrapper() {
		return httpWrapper;
	}

	public void setHttpWrapper(HttpWrapper httpWrapper) {
		this.httpWrapper = httpWrapper;
	}

	public String getManagerUrl() {
		return managerUrl;
	}

	public void setManagerUrl(String managerUrl) {
		this.managerUrl = managerUrl;
	}

}
