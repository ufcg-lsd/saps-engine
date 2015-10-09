package org.fogbowcloud.scheduler.core.ssh;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;


public class TestSshClientWrapper {

	
	private SSHClient clientMock;
	private Session sessionMock;
	private SshClientWrapper sshClientWrapper;
	
	@Before
	public void setUp() throws Exception {
		clientMock = mock(SSHClient.class);
		sessionMock = mock(Session.class);
		sshClientWrapper = spy(new SshClientWrapper(clientMock));
		doNothing().when(sshClientWrapper).addBlankHostKeyVerifier(Mockito.any(SSHClient.class));
	}

	@After
	public void setDown() throws Exception {
		clientMock = null;
		sessionMock = null;
		sshClientWrapper = null;
	}
	
	@Test
	public void doSshExecutionTest() throws IOException{
		String command = "test command";
		
		Command cmd = mock(Command.class);
		
		doReturn(sessionMock).when(clientMock).startSession();
		doReturn(cmd).when(sessionMock).exec(command);
		sshClientWrapper.doSshExecution(command);
		verify(clientMock, times(1)).startSession();
		verify(sessionMock, times(1)).exec(command);
		verify(cmd, times(1)).join();
	}
	 
	@Test
	public void disconnectTest() throws IOException{
		
		sshClientWrapper.setClient(clientMock);
		sshClientWrapper.setSession(sessionMock);
		
		sshClientWrapper.disconnect();
		verify(clientMock, times(1)).disconnect();
		verify(clientMock, times(1)).close();
		verify(sessionMock, times(1)).close();
	}

}
