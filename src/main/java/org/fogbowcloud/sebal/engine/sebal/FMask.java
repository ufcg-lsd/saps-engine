package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class FMask {
	
	public static final Logger LOGGER = Logger.getLogger(FMask.class);
	
	public int runFmask(final ImageData imageData, String fmaskScriptPath, String fmaskToolsPath, String sebalExportPath) throws IOException,
			FileNotFoundException, InterruptedException {
		
		LOGGER.debug("fmaskScriptPath " + fmaskScriptPath + " fmaskToolsPath " + fmaskToolsPath + " sebalExportPath " + sebalExportPath);

		File tempFile = File.createTempFile("temp-" + imageData.getName(),
				".sh");
		FileOutputStream fos = new FileOutputStream(tempFile);

		FileInputStream fis = new FileInputStream(fmaskScriptPath);
		String origExec = IOUtils.toString(fis);

		IOUtils.write(replaceVariables(origExec, imageData, fmaskToolsPath, sebalExportPath), fos);
		fos.close();
		
		LOGGER.debug("temp file absolute path " + tempFile.getAbsolutePath());

		ProcessBuilder builder = new ProcessBuilder("chmod", "+x",
				tempFile.getAbsolutePath());
		Process p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running chmod +x command. Process exit value= " + String.valueOf(p.exitValue()) + " Message="
					+ ProcessUtil.getError(p));
		}
		LOGGER.debug("exitValue=" + String.valueOf(p.exitValue()));
		LOGGER.debug("chmod +x command output=" + ProcessUtil.getOutput(p));

		builder = new ProcessBuilder("bash", tempFile.getAbsolutePath());
		p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running fmask command. Process exit value= " + String.valueOf(p.exitValue()) + " Message="
					+ ProcessUtil.getError(p));
		}
		LOGGER.debug("exitValue=" + String.valueOf(p.exitValue()));
		LOGGER.debug("run-fmask command output=" + ProcessUtil.getOutput(p));

		return p.exitValue();
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
