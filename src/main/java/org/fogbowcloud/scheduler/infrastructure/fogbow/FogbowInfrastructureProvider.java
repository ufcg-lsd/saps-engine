package org.fogbowcloud.scheduler.infrastructure.fogbow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.fogbowcloud.manager.occi.request.RequestType;
import org.fogbowcloud.scheduler.core.http.HttpWrapper;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppUtil;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public class FogbowInfrastructureProvider implements InfrastructureProvider {

	//LOGGER
	private static final Logger LOGGER = Logger.getLogger(FogbowInfrastructureProvider.class);

	// ------------------ CONSTANTS ------------------//
	private static final String NULL_VALUE = "null";
	private static final String SPACE_VALUE = " ";
	private static final String AND_OPERATOR = " && ";
	private static final String OR_OPERATOR = " || ";
	private static final String CATEGORY = "Category";
	private static final String X_OCCI_ATTRIBUTE = "X-OCCI-Attribute: ";

	public static final String INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT = "org.fogbowcloud.request.ssh-public-address";
	public static final String INSTANCE_ATTRIBUTE_SSH_USERNAME_ATT = "org.fogbowcloud.request.ssh-username";
	public static final String INSTANCE_ATTRIBUTE_EXTRA_PORTS_ATT = "org.fogbowcloud.request.extra-ports";
	public static final String INSTANCE_ATTRIBUTE_MEMORY_SIZE = "occi.compute.memory";
	public static final String INSTANCE_ATTRIBUTE_VCORE = "occi.compute.cores";
	public static final String INSTANCE_ATTRIBUTE_DISKSIZE = "TODO-AlterWhenFogbowReturns";  //TODO Alter when fogbow are returning this attribute
	public static final String INSTANCE_ATTRIBUTE_MEMBER_ID = "TODO-AlterWhenFogbowReturns"; //TODO Alter when fogbow are returning this attribute

	// ------------------ ATTRIBUTES -----------------//
	private HttpWrapper httpWrapper;
	private String managerUrl;
	private Token token;
	private Map<String, Specification> pendingRequestsMap = new HashMap<String, Specification>();
	
	public FogbowInfrastructureProvider(String managerUrl, Token token){
		
		httpWrapper = new HttpWrapper();
		this.managerUrl = managerUrl;
		this.token = token;
		
	}
	
	@Override
	public String requestResource(Specification spec) throws RequestResourceException {
		
		String requestInformation;
		
		try {

			this.validateFogbowRequestRequirements(spec);
			
			List<Header> headers = (LinkedList<Header>) requestNewInstanceHeaders(spec);

			requestInformation = this.doRequest("post", managerUrl+"/"+ RequestConstants.TERM, headers);

		} catch (Exception e) {
			throw new RequestResourceException("Create Request FAILED: "+e.getMessage(), e);
		}
		String requestId =  getRequestId(requestInformation);
		pendingRequestsMap.put(requestId, spec);
		return requestId;
	}

	@Override
	public Resource getResource(String requestID) {
		
		Resource resource = getFogbowResource(requestID);
		
		if(resource == null){
			return resource;
		}
		//Testing connection.
		if(!resource.checkConnectivity()){
			return null;
		}
		
		pendingRequestsMap.remove(requestID);
		return resource;

	}

	protected Resource getFogbowResource(String requestID) {
		String fogbowInstanceId;
		String sshInformation;
		Map<String, String> requestAttributes;
		Resource resource = null;
		try {

			//Attempt's to get the Instance ID from Fogbow Manager.
			requestAttributes = getFogbowRequestAttributes(requestID);
			fogbowInstanceId = getInstanceIdByRequestAttributes(requestAttributes);


			//If has Instance ID, then verifies resource's SSH and Other Informations;
			if(fogbowInstanceId != null && !fogbowInstanceId.isEmpty()){
				
				//Recovers Instance's attributes.
				Map<String,String> instanceAttributes = getFogbowInstanceAttributes(fogbowInstanceId);

				if (this.validateInstanceAttributes(instanceAttributes)) {
					
					Specification spec = pendingRequestsMap.get(requestID);
					resource = new Resource(fogbowInstanceId, spec);
					
					//Putting all metadatas on Resource
					sshInformation = instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT);

					String[] addressInfo = sshInformation.split(":");
					String host = addressInfo[0];
					String port = addressInfo[1];
					
					resource.putMetadata(Resource.METADATA_SSH_HOST, host);
					resource.putMetadata(Resource.METADATA_SSH_PORT, port);
					resource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_USERNAME_ATT));
					resource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, instanceAttributes.get(INSTANCE_ATTRIBUTE_EXTRA_PORTS_ATT));				
					resource.putMetadata(Resource.METADATA_VCPU, instanceAttributes.get(INSTANCE_ATTRIBUTE_VCORE));
					resource.putMetadata(Resource.METADATA_MEN_SIZE, instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMORY_SIZE));
					//TODO Descomentar quando o fogbow estiver retornando este atributo
					//newResource.putMetadata(Resource.METADATA_DISK_SIZE, instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); 
					//TODO Descomentar quando o fogbow estiver retornando este atributo
					//newResource.putMetadata(Resource.METADATA_LOCATION, instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); 
					
				}
				
			}
			
		} catch (Exception e) {
			
			LOGGER.error("Error while getting ["+requestID+"]", e);
			resource = null;
		}
		return resource;
	}

	@Override
	public void deleteResource(Resource resource) throws InfrastructureException {
		try{
			this.doRequest("delete", managerUrl + "/compute/" + resource.getId(), new ArrayList<Header>());
		}catch(Exception e){
			throw new InfrastructureException("Error when tries to delete resource with IntanceId ["+resource.getId()+"]", e);
		}
	}
	
	// ----------------------- Private methods ----------------------- //

	private String getInstanceIdByRequestAttributes(Map<String, String> requestAttributes) throws RequestResourceException{
		
		String instanceId = null;
		try {
			instanceId = requestAttributes.get(RequestAttribute.INSTANCE_ID.getValue());
			String requestState = requestAttributes.get(RequestAttribute.STATE.getValue());
			if (getRequestState(requestState).notIn(RequestState.FULFILLED)) {
				instanceId = null;
			}
		} catch (Exception e) {
			throw new RequestResourceException("Get Instance Id FAILED: "+e.getMessage(), e);
		}
		
		return instanceId;
	}
	
	private Map<String, String> getFogbowRequestAttributes(String requestId) throws Exception {
		
		String endpoint = managerUrl + "/" + RequestConstants.TERM + "/"+ requestId;
		String requestResponse = doRequest("get", endpoint, new ArrayList<Header>());
		
		Map<String, String> attrs = parseRequestAttributes(requestResponse);
		return attrs;
	}
	
	private Map<String, String> getFogbowInstanceAttributes(String instanceId) throws Exception {
		
		String endpoint = managerUrl + "/compute/" + instanceId;
		String instanceInformation = doRequest("get", endpoint, new ArrayList<Header>() );
		
		Map<String, String> attrs = parseAttributes(instanceInformation);
		return attrs;
	}
	
	private void validateFogbowRequestRequirements(Specification  requirements) throws RequestResourceException{
		
		if(!FogbowRequirementsHelper.validateFogbowRequirementsSyntax(requirements.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS))){
			throw new RequestResourceException("FogbowRequirements is not in valid format. e.g: [Glue2vCPU >= 1 && Glue2RAM >= 1024 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"]");
		}
		
	}
	
	private List<Header> requestNewInstanceHeaders(Specification requirements) {
		
		String fogbowImage = requirements.getImage();
		String fogbowRequirements = requirements.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);
		
        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader(CATEGORY, RequestConstants.TERM+ "; scheme=\"" + RequestConstants.SCHEME + "\"; class=\""+ RequestConstants.KIND_CLASS + "\""));
        headers.add(new BasicHeader(X_OCCI_ATTRIBUTE,RequestAttribute.INSTANCE_COUNT.getValue() + "=" + 1));
        headers.add(new BasicHeader(X_OCCI_ATTRIBUTE,RequestAttribute.TYPE.getValue() + "=" + RequestType.ONE_TIME.getValue()));
        headers.add(new BasicHeader(X_OCCI_ATTRIBUTE,RequestAttribute.REQUIREMENTS.getValue() + "=" + fogbowRequirements));
        headers.add(new BasicHeader(CATEGORY, fogbowImage + "; scheme=\""+ RequestConstants.TEMPLATE_OS_SCHEME + "\"; class=\""+ RequestConstants.MIXIN_CLASS + "\""));
        if (requirements.getPublicKey() != null && !requirements.getPublicKey().isEmpty()) {
            headers.add(new BasicHeader(CATEGORY,RequestConstants.PUBLIC_KEY_TERM + "; scheme=\""+ 
            		RequestConstants.CREDENTIALS_RESOURCE_SCHEME + "\"; class=\"" + RequestConstants.MIXIN_CLASS+ "\""));
            headers.add(new BasicHeader(X_OCCI_ATTRIBUTE, RequestAttribute.DATA_PUBLIC_KEY.getValue() + "=" + requirements.getPublicKey()));
        }
        return headers;

    }
	
	private String doRequest(String method, String endpoint, List<Header> headers)
			throws Exception {
		return httpWrapper.doRequest(method, endpoint, token.getAccessId(), headers);
	}
	
	private String getRequestId(String requestInformation) {
        String[] requestRes = requestInformation.split(":");
        String[] requestId = requestRes[requestRes.length - 1].split("/");
        return requestId[requestId.length - 1];
    }
	
	private boolean validateInstanceAttributes(Map<String,String> instanceAttributes){
		
		boolean isValid = true;
		
		if(instanceAttributes != null && !instanceAttributes.isEmpty()){

			String sshInformation = instanceAttributes.get(INSTANCE_ATTRIBUTE_SSH_PUBLIC_ADDRESS_ATT);
			String vcore = instanceAttributes.get(INSTANCE_ATTRIBUTE_VCORE);
			String memorySize = instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMORY_SIZE);
			String diskSize = instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE);
			String memberId = instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID);
			
			//If any of these attributes are empty, then return invalid. 
			isValid = !AppUtil.isStringEmpty(sshInformation, vcore, memorySize /*, diskSize, memberId TODO descomentar esse trecho quando tiver sido implementado no fogbow o retorno destes atributos*/);
			
			String[] addressInfo = sshInformation.split(":");
			if(addressInfo!=null && addressInfo.length > 1){
				String host = addressInfo[0];
				String port = addressInfo[1];
				isValid = !AppUtil.isStringEmpty(host, port);
			}else{
				
			}
			
		}else{
			isValid = false;
		}
		
		return isValid;
	}
	
	private Map<String, String> parseRequestAttributes(String response) {
        Map<String, String> atts = new HashMap<String, String>();
        for (String responseLine : response.split("\n")) {
            if (responseLine.contains(X_OCCI_ATTRIBUTE)) {
                String[] responseLineSplit = responseLine.substring(
                        X_OCCI_ATTRIBUTE.length()).split("=");
                String valueStr = responseLineSplit[1]
                        .trim().replace("\"", "");
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
            if (responseLine.contains(X_OCCI_ATTRIBUTE)) {
                String[] responseLineSplit = responseLine.substring(
                        X_OCCI_ATTRIBUTE.length()).split("=");
                String valueStr = responseLineSplit[1]
                        .trim().replace("\"", "");
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
