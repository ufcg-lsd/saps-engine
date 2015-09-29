package org.fogbowcloud.scheduler.core.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;

//TODO this class should be abstract???
public class Resource {

	
	public static final String METADATA_SSH_HOST 		 = "metadataSSHHost";
    public static final String METADATA_SSH_PORT 		 = "metadataSSHPort";
	public static final String METADATA_SSH_USERNAME_ATT = "metadateSshUsername";
	public static final String METADATA_EXTRA_PORTS_ATT  = "metadateExtraPorts";
    
    public static final String METADATA_VCPU 			 = "metadataVcpu";
    public static final String METADATA_MEN_SIZE   		 = "metadataMenSize";
    public static final String METADATA_DISK_SIZE 		 = "metadataDiskSize";
    public static final String METADATA_LOCATION 		 = "metadataLocation";
	
    private String id;
	
	private Map<String, String> resourceMetadata = new HashMap<String, String>();
	
	private Specification specification;
	private String outputFolder;
    private String userName;
    private SshClientWrapper sshClientWrapper;
	
    public Resource(String id, Specification spec) {
    	this.id = id;
    	this.specification = spec;
    	sshClientWrapper = new SshClientWrapper();
    }

    /**
     * This method receives a wanted specification and verifies if this resource matches with it. 
     * <br>Is used to match the Fogbow requirements 
     * (VM.Cores >= Specs.Cores, VM.MenSize >= Specs.MenSize, VM.DiskSize >= Specs.DiskSize, VM.Location >= Specs.Location)
     *  and the Image (VM.image == Specs.image)
     * @param wantedSpecification
     * @return
     */
    public boolean match(Specification wantedSpecification){
		
    	String fogbowRequirement = wantedSpecification.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS);
    	String image = wantedSpecification.getImage();
    	String publicKey = wantedSpecification.getPublicKey();
    	if(fogbowRequirement != null && image != null){
    		
    		if(!FogbowRequirementsHelper.matches(this,fogbowRequirement)){
    			return false;
    		}
    		if(!image.equalsIgnoreCase(this.specification.getImage())){
    			return false;
    		}
    		if(!publicKey.equalsIgnoreCase(this.specification.getPublicKey())){
    			return false;
    		}
    		
    	}else{
    		return false;
    	}
    	
		return true;
	}

	public boolean checkConnectivity(){
		
		String host = this.getMetadataValue(METADATA_SSH_HOST);
		String port = this.getMetadataValue(METADATA_SSH_PORT);
		try {
			sshClientWrapper.connect(host, Integer.parseInt(port));
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public void putMetadata(String attributeName, String value){
		resourceMetadata.put(attributeName, value);
	}

	public void putAllMetadatas(Map<String, String> instanceAttributes) {
		for(Entry<String,String> entry : instanceAttributes.entrySet()){
			this.putMetadata(entry.getKey(), entry.getValue());
		}
	}
	
	public String getMetadataValue(String attributeName){
		return resourceMetadata.get(attributeName);
	}
	
	//----------------------------------- GETTERS and SETTERS -----------------------------------//
	
	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getId() {
		return id;
	}

	public Specification getSpecification() {
		return specification;
	}

	public SshClientWrapper getSshClientWrapper() {
		return sshClientWrapper;
	}

	protected void setSshClientWrapper(SshClientWrapper sshClientWrapper) {
		this.sshClientWrapper = sshClientWrapper;
	}
	
}
