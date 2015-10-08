package org.fogbowcloud.scheduler.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;

import com.google.gson.Gson;

public class PrimesMain {

	private final static String METADATA_REMOTE_OUTPUT_FOLDER = "/tmp/";
	private final static String SPEC_FILE_PATH = "/home/gustavorag/Dev/sebalScheduleEnv/initialSpec";
	private final static String SANDBOX = "/tmp/sandbox";
	private final static String SSH_SCP_PRECOMMAND = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";
	
	private static boolean blockWhileInitializing;
	private static boolean isElastic;
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));


	private static Properties properties;

	private static final Logger LOGGER = Logger.getLogger(PrimesMain.class);

	public static void main(String[] args) throws Exception {

		LOGGER.debug("Starting ExampleMain");
		
		properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		loadConfigFromProperties();

		String initialSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		List<Specification> initialSpecs = getInitialSpecs(initialSpecsFilePath);

		List<Specification> taskSpecs = getInitialSpecs(SPEC_FILE_PATH);
		
		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
		InfrastructureManager infraManager = new InfrastructureManager(initialSpecs, isElastic, infraProvider,
				properties);
		infraManager.start(blockWhileInitializing);
		
		Specification spec = taskSpecs.get(0);
		LOGGER.debug("Task spec: "+spec.toString());
		
		Job primeJob = new PrimeJob();
		for(int count=0; count < 3; count++){
			primeJob.addTask(getPrimeTask(spec, count*1000, (count+1)*1000));
		}
		
		Scheduler scheduler = new Scheduler(primeJob, infraManager);
		ExecutionMonitor execMonitor = new ExecutionMonitor(primeJob, scheduler);

		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));

		LOGGER.debug("Starting Scheduler");
		scheduler.run();

	}

	private static void loadConfigFromProperties() {

		blockWhileInitializing = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();
		isElastic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();

	}

	private static List<Specification> getInitialSpecs(String specsFilePath) throws FileNotFoundException {
		
		LOGGER.info("Getting initial spec from file " + specsFilePath);

		List<Specification> specifications = new ArrayList<Specification>();
		if (specsFilePath != null && new File(specsFilePath).exists()) {
			BufferedReader br = new BufferedReader(new FileReader(specsFilePath));

			Gson gson = new Gson();
			specifications = Arrays.asList(gson.fromJson(br, Specification[].class));
		}
		return specifications;
	}

	private static InfrastructureProvider createInfraProvaiderInstance() throws Exception {

		String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}

	private static Task getPrimeTask(Specification spec, int init, int end) {
		TaskImpl task = new TaskImpl(UUID.randomUUID().toString(), spec);
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER, METADATA_REMOTE_OUTPUT_FOLDER);
		task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER, properties.getProperty("local.output"));
		task.putMetadata(TaskImpl.METADATA_SANDBOX, SANDBOX);
		//task.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH, METADATA_REMOTE_OUTPUT_FOLDER+"/exit");
		task.addCommand(mkdirRemoteFolder(task.getMetadata(TaskImpl.METADATA_SANDBOX)));
		task.addCommand(stageInCommand("isprime.py", task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/isprime.py"));
		task.addCommand(remoteCommand("python /"+task.getMetadata(TaskImpl.METADATA_SANDBOX)+"/isprime.py "+init+" "+end));
		//task.addCommand(remoteCommand("echo 0 > "+task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH)));
		task.addCommand(stageOutCommand("/tmp/primeresult",
				properties.getProperty("local.output") + "/primeresult-" + task.getId()));
		return task;
	}
	
	private static Command remoteCommand(String remoteCommand){
		return new Command(remoteCommand, Command.Type.REMOTE);
	}
	
	private static Command mkdirRemoteFolder(String folder){
		String mkdirCommand = "ssh "+SSH_SCP_PRECOMMAND+" -p $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE +" $"
				+ Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + " mkdir " + folder;
		return new Command(mkdirCommand, Command.Type.PROLOGUE);
	}
	
	private static Command stageInCommand(String localFile, String remoteFile) {
		String scpCommand = "scp "+SSH_SCP_PRECOMMAND+" -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE +" "+localFile+ " $"
				+ Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile;
		return new Command(scpCommand, Command.Type.PROLOGUE);
	}

	private static Command stageOutCommand(String remoteFile, String localFile) {

		String scpCommand = "scp "+SSH_SCP_PRECOMMAND+" -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE + " $"
				+ Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile + " " + localFile+"_[$"+Resource.ENV_SSH_PORT+"]";
		return new Command(scpCommand, Command.Type.EPILOGUE);
	}
	
	static class PrimeJob extends Job{

		@Override
		public void run(Task task) {
			tasksReady.remove(task);
			tasksRunning.add(task);

		}

		@Override
		public void finish(Task task) {
			tasksRunning.remove(task);
			tasksCompleted.add(task);
		}

		@Override
		public void fail(Task task) {
			tasksRunning.remove(task);
			tasksFailed.add(task);
		}

	}

}
