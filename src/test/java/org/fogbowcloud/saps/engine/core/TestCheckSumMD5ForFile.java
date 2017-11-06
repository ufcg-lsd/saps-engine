package org.fogbowcloud.saps.engine.core;

import static org.mockito.Mockito.mock;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.fogbowcloud.saps.engine.core.util.CheckSumMD5ForFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class TestCheckSumMD5ForFile {

	@SuppressWarnings("unused")
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
		// setup
		File tempFile1 = folder.newFile("file1.nc");
		File tempFile2 = folder.newFile("file2.nc");
		
		FileInputStream file1InputStream = new FileInputStream(tempFile1);
		FileInputStream file2InputStream = new FileInputStream(tempFile2);
		
		String checkSum1 = DigestUtils.md5Hex(IOUtils
				.toByteArray(file1InputStream));
		String checkSum2 = DigestUtils.md5Hex(IOUtils
				.toByteArray(file2InputStream));
		
		file1InputStream.close();
		file2InputStream.close();
		
		folder.newFile(tempFile1.getName() + "." + checkSum1 + ".md5");
		folder.newFile(tempFile2.getName() + "." + checkSum2 + ".md5");
		
		// exercise
		boolean isFileCorrupted = CheckSumMD5ForFile.isFileCorrupted(folder.getRoot());
		
		// expect
		Assert.assertEquals(false, isFileCorrupted);
	}
	
	@Ignore
	@Test
	public void testChecksumForBigFiles() throws IOException {
		// setup
		byte []arr = new byte [379584512];
		File tempFile1 = folder.newFile("file1.nc");
		
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile1));
        output.write(arr);		
        output.close();
        
		FileInputStream file1InputStream = new FileInputStream(tempFile1);		
		
		String checkSum1 = DigestUtils.md5Hex(IOUtils
				.toByteArray(file1InputStream));
		
		file1InputStream.close();
		
		folder.newFile(tempFile1.getName() + "." + checkSum1 + ".md5");
		
		// exercise
		boolean isFileCorrupted = CheckSumMD5ForFile.isFileCorrupted(folder.getRoot());
		
		// expect
		Assert.assertEquals(false, isFileCorrupted);
	}

}
