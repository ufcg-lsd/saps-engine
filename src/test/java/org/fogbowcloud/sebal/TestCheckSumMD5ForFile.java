package org.fogbowcloud.sebal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class TestCheckSumMD5ForFile {

	private FileInputStream fileInputStreamMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		fileInputStreamMock = mock(FileInputStream.class);
	}
	
	@Test
	public void testIsFileCorrupted() throws IOException {
		exception.expect(Exception.class);
		
		File localFileMock = mock(File.class);
		String localChecksum = "fake-local-checksum";
		
		DigestUtils digestUtilsMock = mock(DigestUtils.class);
		
		doReturn(true).when(localFileMock.getName()).endsWith(eq(".md5"));
		
		doReturn(localChecksum).when(digestUtilsMock).md5Hex(IOUtils
				.toByteArray(fileInputStreamMock));
		
		File remoteFileMock = mock(File.class);
		String remoteChecksum = null;
		String[] pieces = {"fake-name", "fake-local-checksum", ".md5"};
		
		doReturn(true).when(remoteFileMock).getName().startsWith(eq(remoteFileMock.getName()));
		doReturn(true).when(remoteFileMock).getName().endsWith(eq(".md5"));
		doReturn(pieces).when(remoteFileMock).getName().split(".");
		remoteChecksum = pieces[1];
		
		doReturn(true).when(localChecksum).equals(eq(remoteChecksum));
	}

}
