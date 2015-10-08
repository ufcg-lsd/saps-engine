package org.fogbowcloud.scheduler.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;
import org.fogbowcloud.scheduler.core.util.AppUtil;

public class ExecutionCommandHelper {

	private static final int EXIT_CODE_SUCCESS = 0;
	private static final Logger LOGGER = Logger.getLogger(ExecutionCommandHelper.class);
	private SshClientWrapper sshClientWrapper;
	private final String LOCAL_COMMAND_INTERPRETER;
	
	public ExecutionCommandHelper(Properties properties, SshClientWrapper sshClientWrapper) {
		LOCAL_COMMAND_INTERPRETER = properties.getProperty("local_command_interpreter");
		this.sshClientWrapper = sshClientWrapper;
	}
	
	public int execLocalCommands(List<Command> localCommands, Map<String, String> additionalEnvVariables) {
		for (Command command : localCommands) {
			LOGGER.debug("Executin local command: [Type: "+command.getType().name()+" - Command: "+command.getCommand()+"]");
			command.setState(Command.State.RUNNING);
			try {
				Process pr = starLocalProcess(command, additionalEnvVariables);
				int exitValue = pr.waitFor();
				LOGGER.debug("Local process [cmdLine=" + command + "] output was: \n"
						+ getOutout(pr));
				if (exitValue != TaskExecutionResult.OK) {
					LOGGER.error("Error while executing local process. Process err output was: \n "
							+ getErrOutput(pr));
					command.setState(Command.State.FAILED);
					return TaskExecutionResult.NOK;
				}

			} catch (InterruptedException e) {
				LOGGER.error("Error while executing local process [cmdLine=" + command + "]", e);
				return TaskExecutionResult.NOK;
			} catch (IOException e) {
				LOGGER.error("Error while starting or getting output of process [cmdLine="
						+ command + "]", e);
				return TaskExecutionResult.NOK;
			}
		}
		return TaskExecutionResult.OK;
	}

	private Process starLocalProcess(Command command, Map<String, String> additionalEnvVariables)
			throws IOException {
		ProcessBuilder builder = new ProcessBuilder(LOCAL_COMMAND_INTERPRETER, "-c",
				command.getCommand());
		if (additionalEnvVariables == null || additionalEnvVariables.isEmpty()) {
			return builder.start();	
		}
		
		// adding additional environment variables related to resource and/or task
		for (String envVariable : additionalEnvVariables.keySet()) {
			builder.environment().put(envVariable, additionalEnvVariables.get(envVariable));
		}
		return builder.start();
	}
	
	private String getOutout(Process pr) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		StringBuilder out = new StringBuilder();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			out.append(line);
		}
		return out.toString();
	}

	private String getErrOutput(Process pr) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		StringBuilder err = new StringBuilder();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			err.append(line);
		}
		return err.toString();
	}

	public int execRemoteCommands(String address, int sshPort, String username,
			String privateKeyFilePath, List<Command> remoteCommands) {
		try {
			sshClientWrapper.connect(address, sshPort, username, privateKeyFilePath);
			for (Command command : remoteCommands) {
				command.setState(Command.State.RUNNING);
				Integer exitStatus = sshClientWrapper.doSshExecution(command.getCommand());
				if (exitStatus != TaskExecutionResult.OK) {
					LOGGER.error("Error while executing command line '" + command.getCommand() + "'.");
					command.setState(Command.State.FAILED);
					return TaskExecutionResult.NOK;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error while connecting resource.", e);
			return TaskExecutionResult.NOK;
		} finally {
			try {
				sshClientWrapper.disconnect();
			} catch (Throwable e) {
			}
		}
		return TaskExecutionResult.OK;
	}

	public Integer getRemoteCommandExitValue(String address, int sshPort, String username,
			String privateKeyFilePath, String remoteFileExit) {
		if(AppUtil.isStringEmpty(remoteFileExit)){
			return EXIT_CODE_SUCCESS;
		}
        String exitFileExistsCommand = "cat " + remoteFileExit;
        try {
			sshClientWrapper.connect(address, sshPort, username, privateKeyFilePath);
			int exitFileExists = sshClientWrapper.doSshExecution(exitFileExistsCommand);
			if (exitFileExists != TaskExecutionResult.OK) {
				return null;
			}
			String exitStatusCommand = "cat " + remoteFileExit + " | grep -w 0";
			int exitStatus = sshClientWrapper.doSshExecution(exitStatusCommand);
			return exitStatus;
		} catch (IOException e) {
			LOGGER.error("Error while connecting resource.", e);
			return null;
		} finally {
			try {
				sshClientWrapper.disconnect();
			} catch (Throwable e) {
			}
		}
   }
}
