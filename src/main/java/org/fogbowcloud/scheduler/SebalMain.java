package org.fogbowcloud.scheduler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;

public class SebalMain {
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		try {
			Job job = new Job();

			InfrastructureManager infraManager;
			infraManager = new InfrastructureManager(properties);

			Scheduler scheduler = new Scheduler(job, infraManager);

			ExecutionMonitor execMonitor = new ExecutionMonitor(job, scheduler);

			executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0, 0);

			schedulerTimer.scheduleAtFixedRate(scheduler, 0, 0);

			//		while (true) {
			//			TODO imagens prontas para executar
			//		 	Cria tasks para imagem
			//		
			//		}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
