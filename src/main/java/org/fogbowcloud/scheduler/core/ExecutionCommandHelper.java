package org.fogbowcloud.scheduler.core;

import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;

public class ExecutionCommandHelper {

	private static final Logger LOGGER = Logger.getLogger(ExecutionCommandHelper.class);
	private SshClientWrapper sshClientWrapper;
	
	public int execLocalCommands(List<Command> localCommands) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int execRemoteCommands(List<Command> remoteCommands) {
		// TODO Auto-generated method stub
		return 0;
	}

}
