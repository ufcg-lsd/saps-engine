package org.fogbowcloud.scheduler.core.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionCommandHelper;
import org.fogbowcloud.scheduler.core.TaskExecutionResult;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;

//TODO this class should be abstract???
public class Resource {
	
	// Environment variables to be replaced at prologue and epilogue scripts
	//TODO how we should treat them?
	public static final String ENV_HOST = "HOST";
	public static final String ENV_SSH_PORT = "SSH_PORT";
	public static final String ENV_SSH_USER = "SSH_USER";
	public static final String ENV_PRIVATE_KEY_FILE = "PRIVATE_KEY_FILE";
	
	public static final String METADATA_SSH_HOST 		 = "metadataSSHHost";
    public static final String METADATA_SSH_PORT 		 = "metadataSSHPort";
	public static final String METADATA_SSH_USERNAME_ATT = "metadateSshUsername";
	public static final String METADATA_EXTRA_PORTS_ATT  = "metadateExtraPorts";
    
	public static final String METADATA_IMAGE 		     = "metadataImage";
	public static final String METADATA_PUBLIC_KEY 		 = "metadataPublicKey";
	
    public static final String METADATA_VCPU 			 = "metadataVcpu";
    public static final String METADATA_MEN_SIZE   		 = "metadataMenSize";
    public static final String METADATA_DISK_SIZE 		 = "metadataDiskSize";
    public static final String METADATA_LOCATION 		 = "metadataLocation";
    
    public static final String METADATA_REQUEST_TYPE 	 = "metadataRequestType";
	
    private String id;
	private Map<String, String> metadata = new HashMap<String, String>();
	private Task task;
	private TaskExecutionResult taskExecutionResult;
	private ExecutionCommandHelper executionCommandHelper;
	
//	private Specification specification;
//	private String outputFolder;
//    private String userName;
    private SshClientWrapper sshClientWrapper = new SshClientWrapper();
    
	private static final Logger LOGGER = Logger.getLogger(Resource.class);
	
    public Resource(String id, Properties properties) {
    	this.id = id;    	
		// TODO we need to check if Resource needs to have a specification or
		// translate some spec attributes into resources attributes
//    	this.specification = spec;
    	executionCommandHelper = new ExecutionCommandHelper(properties);
    }

    /**
     * This method receives a wanted specification and verifies if this resource matches with it. 
     * <br>Is used to match the Fogbow requirements 
     * (VM.Cores >= Specs.Cores, VM.MenSize >= Specs.MenSize, VM.DiskSize >= Specs.DiskSize, VM.Location >= Specs.Location)
     *  and the Image (VM.image == Specs.image)
     * @param spec
     * @return
     */
	public boolean match(Specification spec) {

		//TODO
		//Do we need to try connect to resource using spec username and privateKey?
		String fogbowRequirement = spec
				.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);
		String image = spec.getImage();
		String publicKey = spec.getPublicKey();
		if (fogbowRequirement != null && image != null) {

			if (!FogbowRequirementsHelper.matches(this, fogbowRequirement)) {
				return false;
			}
			if (!image.equalsIgnoreCase(metadata.get(METADATA_IMAGE))) {
				return false;
			}
			if (!publicKey.equalsIgnoreCase(metadata.get(METADATA_PUBLIC_KEY))) {
				return false;
			}
		} else {
			return false;
		}
    	
		return true;
	}


	public boolean checkConnectivity() {
		return this.checkConnectivity(0);
	}
	
	public boolean checkConnectivity(int timeOut) {
		String host = this.getMetadataValue(METADATA_SSH_HOST);
		String port = this.getMetadataValue(METADATA_SSH_PORT);
		try {
			sshClientWrapper.connect(host, Integer.parseInt(port), timeOut);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public void putMetadata(String attributeName, String value) {
		metadata.put(attributeName, value);
	}

	public void putAllMetadatas(Map<String, String> instanceAttributes) {
		for(Entry<String,String> entry : instanceAttributes.entrySet()){
			this.putMetadata(entry.getKey(), entry.getValue());
		}
	}
	
	public String getMetadataValue(String attributeName) {
		return metadata.get(attributeName);
	}
	
	public Map<String, String> getAllMetadata(){
		return metadata;
	}

	public void executeTask(Task task) {
		LOGGER.debug("Executing task " + task.getId());
        this.task = task;
        this.taskExecutionResult = new TaskExecutionResult();
        
		if (!executePrologue() || !executeRemote() || !executeEpilogue()) {
			finish(TaskExecutionResult.NOK);
			return;
		}
		finish(TaskExecutionResult.OK);
	}
	
	protected void finish(int exitValue) {
		LOGGER.debug("Finishing task " + task.getId() + " with exit value = " + exitValue);
		taskExecutionResult.finish(exitValue);
	}
	
	protected boolean executePrologue() {
		List<Command> commands = task.getCommandsByType(Command.Type.PROLOGUE);
		return executePrologueCommands(commands) == TaskExecutionResult.OK;
	}
	
	protected int executePrologueCommands(List<Command> prologueCommands) {
		LOGGER.debug("Executing prologue commands on " + getId());
		int executionResult = TaskExecutionResult.OK;
		if (prologueCommands != null && !prologueCommands.isEmpty()) {
			executionResult = executionCommandHelper.execLocalCommands(prologueCommands, getAdditionalEnvVariables());
		}
		LOGGER.debug("Prologue commands finished on " + getId() + " with result " + executionResult);
		return executionResult;
	}

	protected Map<String, String> getAdditionalEnvVariables() {
		Map<String, String> additionalEnvVar = new HashMap<String, String>();
		additionalEnvVar.put(ENV_HOST, getMetadataValue(METADATA_SSH_HOST));
		additionalEnvVar.put(ENV_SSH_PORT, getMetadataValue(METADATA_SSH_PORT));
		additionalEnvVar.put(ENV_SSH_USER, task.getSpecification().getUsername());
		additionalEnvVar.put(ENV_PRIVATE_KEY_FILE, task.getSpecification().getPrivateKeyFilePath());
		return additionalEnvVar;
	}

	protected boolean executeRemote() {
		List<Command> commands = task.getCommandsByType(Command.Type.REMOTE);
		return executeRemoteCommands(commands) == TaskExecutionResult.OK;
	}

	protected int executeRemoteCommands(List<Command> remoteCommands) {
		LOGGER.debug("Executing remote commands on " + getId());
		int executionResult = TaskExecutionResult.OK;
		if (remoteCommands != null && !remoteCommands.isEmpty()) {
			String host = getMetadataValue(METADATA_SSH_HOST);
			int sshPort = Integer.parseInt(getMetadataValue(METADATA_SSH_PORT));
			executionResult = executionCommandHelper.execRemoteCommands(host, sshPort, task
					.getSpecification().getUsername(), task.getSpecification()
					.getPrivateKeyFilePath(), remoteCommands);
		}
		LOGGER.debug("Remote commands finished on " + getId() + " with result " + executionResult);
		return executionResult;
	}

	protected boolean executeEpilogue() {
		List<Command> commands = task.getCommandsByType(Command.Type.EPILOGUE);
		return executeEpilogueCommands(commands) == TaskExecutionResult.OK;
	}
	
	protected int executeEpilogueCommands(List<Command> epilogueCommands) {
		LOGGER.debug("Executing epilogue commands on " + getId());
		int executionResult = TaskExecutionResult.OK;
		if (epilogueCommands != null && !epilogueCommands.isEmpty()) {
			executionResult = executionCommandHelper.execLocalCommands(epilogueCommands,
					getAdditionalEnvVariables());
		}
		LOGGER.debug("Epilogue commands finished on " + getId() + " with result " + executionResult);
		return executionResult;
	}

	public void copyInformations(Resource resource){
		this.metadata.clear();
		this.metadata.putAll(resource.getAllMetadata());
	}
	

	//----------------------------------- GETTERS and SETTERS -----------------------------------//
	public String getId() {
		return id;
	}

	protected SshClientWrapper getSshClientWrapper() {
		return sshClientWrapper;
	}
	
	protected ExecutionCommandHelper getExecutionCommandHelper() {
		return this.executionCommandHelper;
	}

	protected void setExecutionCommandHelper(ExecutionCommandHelper executionCommandHelper) {
		this.executionCommandHelper = executionCommandHelper;
	}

	protected TaskExecutionResult getTaskExecutionResult() {
		return this.taskExecutionResult;
	}

	protected void setTaskExecutionResult(TaskExecutionResult taskExecutionResult) {
		this.taskExecutionResult = taskExecutionResult;
	}

	protected void setSshClientWrapper(SshClientWrapper sshClientWrapper) {
		this.sshClientWrapper = sshClientWrapper;
	}
}
