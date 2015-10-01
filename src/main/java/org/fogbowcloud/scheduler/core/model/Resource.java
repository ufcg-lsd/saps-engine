package org.fogbowcloud.scheduler.core.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionCommandHelper;
import org.fogbowcloud.scheduler.core.TaskExecutionResult;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;

//TODO this class should be abstract???
public class Resource {
	
	// Environment variables to be replaced at prologue and epilogue scripts
	//TODO how we should treat them?
	public static final String ENV_HOST = "${HOST}";
	public static final String ENV_SSH_PORT = "${SSH_PORT}";
	
	public static final String METADATA_SSH_HOST 		 = "metadataSSHHost";
    public static final String METADATA_SSH_PORT 		 = "metadataSSHPort";
	public static final String METADATA_SSH_USERNAME_ATT = "metadateSshUsername";
	public static final String METADATA_EXTRA_PORTS_ATT  = "metadateExtraPorts";
    
    public static final String METADATA_VCPU 			 = "metadataVcpu";
    public static final String METADATA_MEN_SIZE   		 = "metadataMenSize";
    public static final String METADATA_DISK_SIZE 		 = "metadataDiskSize";
    public static final String METADATA_LOCATION 		 = "metadataLocation";
	
    private String id;
	private Map<String, String> metadata = new HashMap<String, String>();
	private Task task;
	private TaskExecutionResult taskExecutionResult;
	private ExecutionCommandHelper executionCommandHelper = new ExecutionCommandHelper();
	
	private Specification specification;
	private String outputFolder;
//    private String userName;
    private SshClientWrapper sshClientWrapper;
    
	private static final Logger LOGGER = Logger.getLogger(Resource.class);
	
    public Resource(String id, Specification spec) {
    	this.id = id;
    	
		// TODO we need to check if Resource needs to have a specification or
		// translate some spec attributes into resources attributes
    	this.specification = spec;
    	sshClientWrapper = new SshClientWrapper();
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

		//TODO We need check fogbow requirements with resource values, image and public key
		//Do we need to try connect to resource using spec username and privateKey?
		String fogbowRequirement = spec
				.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);
		String image = spec.getImage();
		String publicKey = spec.getPublicKey();
		if (fogbowRequirement != null && image != null) {

			if (!FogbowRequirementsHelper.matches(this, fogbowRequirement)) {
				return false;
			}
			if (!image.equalsIgnoreCase(this.specification.getImage())) {
				return false;
			}
			if (!publicKey.equalsIgnoreCase(this.specification.getPublicKey())) {
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
	
	private void finish(int exitValue) {
		LOGGER.debug("Finishing task " + task.getId() + " with exit value = " + exitValue);
		taskExecutionResult.finish(exitValue);
	}
	
	private boolean executePrologue() {
		List<Command> commands = task.getCommandsByType(Command.Type.PROLOGUE);
		return executePrologueCommands(commands) == TaskExecutionResult.OK;
	}
	
	private int executePrologueCommands(List<Command> prologueCommands) {
		LOGGER.debug("Executing prologue commands on " + getId());
		int executionResult = TaskExecutionResult.OK;
		if (prologueCommands != null && !prologueCommands.isEmpty()) {
			executionResult = executionCommandHelper.execLocalCommands(prologueCommands);
		}
		LOGGER.debug("Prologue commands finished on " + getId() + " with result " + executionResult);
		return executionResult;
	}

	private boolean executeRemote() {
		List<Command> commands = task.getCommandsByType(Command.Type.REMOTE);
		return executeRemoteCommands(commands) == TaskExecutionResult.OK;
	}

	private int executeRemoteCommands(List<Command> remoteCommands) {
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

	private boolean executeEpilogue() {
		List<Command> commands = task.getCommandsByType(Command.Type.EPILOGUE);
		return executeEpilogueCommands(commands) == TaskExecutionResult.OK;
	}
	
	private int executeEpilogueCommands(List<Command> epilogueCommands) {
		LOGGER.debug("Executing epilogue commands on " + getId());
		int executionResult = TaskExecutionResult.OK;
		if (epilogueCommands != null && !epilogueCommands.isEmpty()) {
			executionResult = executionCommandHelper.execLocalCommands(epilogueCommands);
		}
		LOGGER.debug("Epilogue commands finished on " + getId() + " with result " + executionResult);
		return executionResult;
	}

	//----------------------------------- GETTERS and SETTERS -----------------------------------//
	public String getId() {
		return id;
	}

	public Specification getSpecification() {
		return specification;
	}

	protected SshClientWrapper getSshClientWrapper() {
		return sshClientWrapper;
	}
	
	protected ExecutionCommandHelper getExecutionCommandHelper() {
		return this.executionCommandHelper;
	}
	
	protected TaskExecutionResult getTaskExecutionResult() {
		return this.taskExecutionResult;
	}

	protected void setSshClientWrapper(SshClientWrapper sshClientWrapper) {
		this.sshClientWrapper = sshClientWrapper;
	}
}
