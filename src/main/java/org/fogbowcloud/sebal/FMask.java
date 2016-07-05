package org.fogbowcloud.sebal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class FMask {
	
	public static final Logger LOGGER = Logger.getLogger(FMask.class);
	
	public int runFmask(final ImageData imageData, String fmaskScriptPath, String fmaskToolsPath, String sebalExportPath) throws IOException,
			FileNotFoundException, InterruptedException {

		File tempFile = File.createTempFile("temp-" + imageData.getName(),
				".sh");
		FileOutputStream fos = new FileOutputStream(tempFile);

		FileInputStream fis = new FileInputStream(fmaskScriptPath);
		String origExec = IOUtils.toString(fis);

		IOUtils.write(replaceVariables(origExec, imageData, fmaskToolsPath, sebalExportPath), fos);
		fos.close();

		ProcessBuilder builder = new ProcessBuilder("chmod", "+x",
				tempFile.getAbsolutePath());
		Process p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running chmod +x command. Message="
					+ getError(p));
		}
		LOGGER.debug("chmod +x command output=" + getOutput(p));

		builder = new ProcessBuilder("bash", tempFile.getAbsolutePath());
		p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running fmask command. Message="
					+ getError(p));
		}
		LOGGER.debug("run-fmask command output=" + getOutput(p));

		return p.exitValue();
	}

	private static String getOutput(Process p) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String out = new String();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			out += line;
		}
		return out;
	}

	private static String getError(Process p) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String error = new String();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			error += line;
		}
		return error;
	}
	
	private String replaceVariables(String command, ImageData imageData, String fmaskToolsPath, String sebalExportPath) {
		command = command.replaceAll(Pattern.quote("${IMAGE_NAME}"),
				imageData.getName());
		command = command.replaceAll(Pattern.quote("${IMAGES_MOUNT_POINT}"),
				sebalExportPath);
		command = command.replaceAll(Pattern.quote("${FMASK_TOOL}"),
				fmaskToolsPath);
		return command;
	}

}
