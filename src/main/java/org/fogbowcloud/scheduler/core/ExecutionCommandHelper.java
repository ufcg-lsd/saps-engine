package org.fogbowcloud.scheduler.core;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;

public class ExecutionCommandHelper {

	private static final Logger LOGGER = Logger.getLogger(ExecutionCommandHelper.class);
	private SshClientWrapper sshClientWrapper =new SshClientWrapper();
	
	public int execLocalCommands(List<Command> localCommands) {
		// TODO Auto-generated method stub
		return 0;
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
	
//	public int doSsh(String address, int port, String command, String userName,
//			String privateKeyFilePath) {
//		try {
//			sshClientWrapper.connect(address, port, userName,
//					privateKeyFilePath);
//			return sshClientWrapper.doSshExecution(command);
//		} catch (Exception e) {
//			return TaskExecutionResult.NOK;
//		} finally {
//			try {
//				sshClientWrapper.disconnect();
//			} catch (Throwable e) {
//			}
//		}
//	}

}
