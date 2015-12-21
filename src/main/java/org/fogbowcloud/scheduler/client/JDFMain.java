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

import com.amazonaws.services.opsworks.model.App;

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
	private final static String SANDBOX = "/tmp/sandbox";
	private final static String SSH_SCP_PRECOMMAND = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";

	private static int jobID = 0;

	private static int taskID = 0;

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


		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
		InfrastructureManager infraManager = new InfrastructureManager(null, isElastic, infraProvider,
				properties);
		infraManager.start(blockWhileInitializing);

		JDFJob job = new JDFJob();
		
		String jdfFilePath = args[0];

		List<Task> taskList = JDFTasks.getTasksFromJDFFile(job.getId(), jdfFilePath, properties);
		
		for (Task task : taskList) {
			job.addTask(task);
		}
		
		Scheduler scheduler = new Scheduler(infraManager, job);
		ExecutionMonitor execMonitor = new ExecutionMonitor(scheduler, job);



		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: " + properties.getProperty("execution_monitor_period"));
		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));
		schedulerTimer.scheduleAtFixedRate(scheduler, 0, 30000);
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