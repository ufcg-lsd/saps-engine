package org.fogbowcloud.scheduler.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.naming.directory.InvalidAttributeValueException;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;
import org.glite.jdl.CollectionAd;
import org.glite.jdl.Jdl;
import org.ourgrid.common.specification.job.IOEntry;
import org.ourgrid.common.specification.job.JobSpecification;
import org.ourgrid.common.specification.job.TaskSpecification;
import org.ourgrid.common.specification.main.CommonCompiler;
import org.ourgrid.common.specification.main.CommonCompiler.FileType;
import org.ourgrid.common.specification.main.CompilerException;
import org.ourgrid.common.specification.main.JdlOGExtension;

import condor.classad.Constant;
import condor.classad.ListExpr;
import condor.classad.RecordExpr;

/**
 * This class works as a translator. It receives a JDF file as an input
 * and creates a JDL file as the output.
 * @author Ricardo Araujo Santos - ricardo@lsd.ufcg.edu.br
 */
public class JDFMain {

	public static final Logger LOGGER = Logger.getLogger(JDFMain.class);

	private final static String METADATA_REMOTE_OUTPUT_FOLDER = "/tmp/";
	private final static String SPEC_FILE_PATH = "/home/igorvcs/Dev/sebalScheduleEnv/initialSpec";
	private final static String SANDBOX = "/tmp/sandbox";
	private final static String SSH_SCP_PRECOMMAND = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";
	private final static String PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDG2U8rz4I31LIyDBPpe01WJdGt0JBowZ0Zq7Nxq7mol3G4cW5OJt9v3aQLRU8zanceXXSagNg8O4v2ppFzROYlIOgg2KN3Zu6Tj7Evmfp++J160dwshnP3aQCSLIDSBnMsZyPRprIbaL2LifVmrKcOfG3QcRQHZx2HRWJp+lty0IqP+FBaobB7nXzF58ibOJ84Fk9QpQmS5JK3AXdwCISmN8bgfcjoUJB2FMB5OU8ilkIyG4HDZmI82z+6hUS2sVd/ss8biIN6qGfRVxEDhVlDw3o+XqL+HQ7udd2Q61oHs8iBa711SWG64Eie6HAm8SIOsL7dvPx1rBfBsp3Dq3gjnIpTZqwluiTE8q9S6rTiDQndCGWvAnSU01BePD51ZnMEckluYTOhNLgCMtNTXZJgYSHPVsLWXa5xdGSffL73a4gIupE36tnZlNyiAQGDJUrWh+ygEc2ALdQfpOVWo+CMkTBswvrHYSJdFC7r1U8ACrOlsLE02/uqqBbp7fTUuuMk77J8t0ocxuz48tVKOlog0ajS5nphPLfPGnP2PVTh7GXNTLOnqGVwMrjFIAHj7ukd+l36wUAIHR7Y4YWKVaIBvTZS/fQNn0cOGon2DnNL3wNAUc6pthhXlNY33aU2ky55mZR4drAdbRGRdEZQF0YHEFnzP0x2GucHwg6ZtMJ2Aw== igorvcs@bobo";
	private final static String PRIVATE_KEY_PATH = "/home/igorvcs/.ssh/id_rsa";

	private static final String JDL_EXTENSION = ".jdl";

	private static final String JDF_EXTENSION = ".jdf";

	private static int jobID = 0;

	private static int taskID = 0;

	private static Pattern pattern;

	private static String standardImage = "fogbow-ubuntu";


	private static boolean blockWhileInitializing;
	private static boolean isElastic;
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	
	private static Properties properties;
	/**
	 * This method receives a JDF file as input and requests the mapping of
	 * its attributes to JDL attributes, generating a JDL file at the end
	 * @param args 
	 * @throws Exception 
	 */
	public static void main( String[ ] args ) throws Exception {
		properties = new Properties();
		FileInputStream input = new FileInputStream(args[1]);
		properties.load(input);
		
		loadConfigFromProperties();
		
		String initialSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		
		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
//		InfrastructureManager infraManager = new InfrastructureManager(initialSpecs, isElastic, infraProvider,
//				properties);
		InfrastructureManager infraManager = new InfrastructureManager(null, isElastic, infraProvider,
				properties);
		infraManager.start(blockWhileInitializing);
		
		
		JDFJob job = new JDFJob();

		if ( args.length >= 1 ) {

			String jdfFileName = args[0];

			if(jdfFileName == null){
				System.err.println("Invalid jdf file: "+jdfFileName);
				return;
			}

			File file = new File( jdfFileName );
			if ( file.exists() ) {

				if ( file.canRead() ) {

					//Compiling JDF
					CommonCompiler commonCompiler = new CommonCompiler();
					try {
						commonCompiler.compile( jdfFileName, FileType.JDF );

						JobSpecification jobSpec = (JobSpecification) commonCompiler.getResult().get( 0 );
						CollectionAd collection = new CollectionAd();
						ListExpr listExpr = new ListExpr();


						//Mapping attributes
						taskID = 0;
						String jobRequirementes = jobSpec.getRequirements();
						for ( TaskSpecification taskSpec : jobSpec.getTaskSpecs() ) {
							jobRequirementes = jobRequirementes.replace("(", "").replace(")", "");
							
							
							String image = standardImage;


							for (String req : jobRequirementes.split("and")){
								if (req.trim().startsWith("image")) {
									image = req.split("==")[1].trim();
								}
							}

							Specification spec = new Specification(image, properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_USERNAME),PUBLIC_KEY , PRIVATE_KEY_PATH);
							LOGGER.debug("===============================================================");
							LOGGER.debug(properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_USERNAME));
							LOGGER.debug(properties.getProperty("local.output"));
							int i = 0;
							for (String req : jobRequirementes.split("and") ){
								if (i == 0 && !req.trim().startsWith("image")) {
									i++;
									spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, req);

								} else if (!req.trim().startsWith("image")) {
									spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) + " && " + req);
								}
							} 

							spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE, "one-time");

							Task task = new TaskImpl("TaskNumber"+taskID, spec);
							task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER, METADATA_REMOTE_OUTPUT_FOLDER);
							task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER, properties.getProperty("local.output"));
							task.putMetadata(TaskImpl.METADATA_SANDBOX, SANDBOX);
							task.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH, METADATA_REMOTE_OUTPUT_FOLDER + "/exit");

							parseInputBlocks( taskSpec, task );

							parseExecutable( taskSpec, task );

							parseOutputBlocks( taskSpec, task );

							parseEpilogue( taskSpec, task );
							

							job.addTask(task);
							LOGGER.debug("Task specs: " +task.getSpecification().toString());
							taskID++;
						}
						jobID++;
					} catch ( CompilerException e ) {
						System.err.println( "Problems with your JDF file. See errors below:");
						e.printStackTrace();
					} catch ( Exception e ) {
						e.printStackTrace();
					}
				}else{
					System.err.println( "Check your permissions for file: " + file.getAbsolutePath() );
				}
			}else{
				System.err.println( "File: " + file.getAbsolutePath() + " does not exists." );
			}
		}else{
			System.err.println("Missing arguments.");
		}
		Scheduler scheduler = new Scheduler(infraManager, job);
		ExecutionMonitor execMonitor = new ExecutionMonitor(scheduler, job);

		
		
		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: " + properties.getProperty("execution_monitor_period"));
		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));
		schedulerTimer.scheduleAtFixedRate(scheduler, 0, 30000);
	}


	/**
	 * This method translates the JDF remote executable command into the JDL format
	 * @param taskSpec The task specification {@link TaskSpecification}
	 * @param task The output expression containing the JDL job
	 * @throws Exception 
	 */
	private static void parseExecutable( TaskSpecification taskSpec, Task task ) throws Exception {

		String exec = taskSpec.getRemoteExec();
		if( exec.contains( ";" ) ){
			System.err.println( "Task \n-------\n" + taskSpec + " \n-------\ncould not be parsed as it contains more than one executable command." );
			throw new Exception();
		}

		exec = parseEnvironmentVariables(exec);

			Command command = new Command("\""+exec  + " ; echo 0 > " + task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH) + "\"", Command.Type.REMOTE);
			LOGGER.debug("Remote command:" + exec);
			task.addCommand(command);
//		}

	}

	/**
	 * This method replaces environment variables defined in the JDF to its
	 * values.
	 * @param string A string representing the remote executable command of the JDF job
	 * @return A string with the environment variables replaced
	 */
	private static String parseEnvironmentVariables( String string ) {

		return string.replaceAll( "\\$JOB", Integer.toString( jobID ) ).replaceAll( "\\$TASK",
				Integer.toString( taskID ) ).replaceAll( "\\$PLAYPEN", "." ).replaceAll( "\\$STORAGE", ".");
	}

	/**
	 * This method translates the JDF sabotage check command to the
	 * JDL epilogue command
	 * @param task The task specification {@link TaskSpecification}
	 * @param recordExpr The output expression containing the JDL job 
	 */
	private static void parseEpilogue( TaskSpecification task, Task recordExpr ) {

		String sabotageCheck = task.getSabotageCheck();
		if ( sabotageCheck == null || (sabotageCheck.trim().length() == 0) ) {
			return;
		}
		sabotageCheck = parseEnvironmentVariables( sabotageCheck );
		LOGGER.debug("Epilogue command:" + sabotageCheck);

		Command command = new Command(sabotageCheck, Command.Type.EPILOGUE);
		recordExpr.addCommand(command);
	}

	/**
	 * This method translates the Ourgrid input IOBlocks to JDL InputSandbox
	 * @param taskSpec The task specification {@link TaskSpecification}
	 * @param task The output expression containing the JDL job
	 */
	private static void parseInputBlocks( TaskSpecification taskSpec, Task task ) {

		List<IOEntry> initBlocks = taskSpec.getInitBlock().getEntry( "" );
		if ( initBlocks == null ) {
			return;
		}
		for ( IOEntry ioEntry : initBlocks ) {
			String sourceFile = parseEnvironmentVariables( ioEntry.getSourceFile() );
			String destination = parseEnvironmentVariables(ioEntry.getDestination());
			task.addCommand(mkdirRemoteFolder(ioEntry.getDestination()));
			task.addCommand(stageInCommand(sourceFile, destination));
			LOGGER.debug("Input Command:" + mkdirRemoteFolder(ioEntry.getDestination()).getCommand());
			LOGGER.debug("Input command:" + stageInCommand(sourceFile, destination).getCommand());
		}
	}

	private static Command stageInCommand(String localFile, String remoteFile) {
		String scpCommand = "scp "+SSH_SCP_PRECOMMAND+" -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE +" "+localFile+ " $"
				+ Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile;
		return new Command(scpCommand, Command.Type.PROLOGUE);
	}
	
	private static Command cleanPreviousExecution(String folder) {
		String mkdirCommand = "ssh " + SSH_SCP_PRECOMMAND + " -p $" + Resource.ENV_SSH_PORT + " -i $"
				+ Resource.ENV_PRIVATE_KEY_FILE + " $" + Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST
				+ " rm -rfv " + folder;
		return new Command(mkdirCommand, Command.Type.PROLOGUE);
	}

	private static Command mkdirRemoteFolder(String folder) {
		String mkdirCommand = "ssh " + SSH_SCP_PRECOMMAND + " -p $" + Resource.ENV_SSH_PORT + " -i $"
				+ Resource.ENV_PRIVATE_KEY_FILE + " $" + Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST
				+ " mkdir " + folder;
		return new Command(mkdirCommand, Command.Type.PROLOGUE);
	}

	/**
	 * This method translates the Ourgrid output IOBlocks to JDL InputSandbox
	 * @param taskSpec The task specification {@link TaskSpecification}
	 * @param task The output expression containing the JDL job
	 */
	private static void parseOutputBlocks( TaskSpecification taskSpec, Task task ) {

		List<IOEntry> finalBlocks = taskSpec.getFinalBlock().getEntry( "" );
		if ( finalBlocks == null ) {
			return;
		}
		ListExpr osbList = new ListExpr();
		ListExpr osbDest = new ListExpr();
		for ( IOEntry ioEntry : finalBlocks ) {
			String sourceFile = parseEnvironmentVariables( ioEntry.getSourceFile());
			String destination = parseEnvironmentVariables( ioEntry.getDestination());
			task.addCommand(stageOutCommand(sourceFile, destination));
			LOGGER.debug("Output command:" + stageOutCommand(sourceFile, destination).getCommand());
			
		}
	}

	private static Command stageOutCommand(String remoteFile, String localFile) {

		String scpCommand = "scp "+SSH_SCP_PRECOMMAND+" -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE + " $"
				+ Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile + " " + localFile+"_[$"+Resource.ENV_SSH_PORT+"]";
		return new Command(scpCommand, Command.Type.EPILOGUE);
	}
	
	private static void loadConfigFromProperties() {

		blockWhileInitializing = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();
		isElastic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();

	}
	
	private static InfrastructureProvider createInfraProvaiderInstance() throws Exception {

		String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
}