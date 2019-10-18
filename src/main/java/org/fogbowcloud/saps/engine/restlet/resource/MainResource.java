package org.fogbowcloud.saps.engine.restlet.resource;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class MainResource extends BaseResource {
	private static final Logger LOGGER = Logger.getLogger(MainResource.class);
	private Configuration cfg;

	private void setUpTemplatesConfiguration() throws IOException {
		if (this.cfg == null) {
			this.cfg = new Configuration();
			
			// Specify the source where the template files come from. Here I set a
			// plain directory for it, but non-file-system sources are possible too:
			this.cfg.setDirectoryForTemplateLoading(new File("./dbWebHtml/"));
			
			// Set the preferred charset template files are stored in. UTF-8 is
			// a good choice in most applications:
			this.cfg.setDefaultEncoding("UTF-8");
			
			// Sets how errors will appear.
			this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		}
	}

	@Get
	public TemplateRepresentation homeUi() {
		String requestPath = null;
		if (getRequestAttributes().containsKey("requestPath")) {
			requestPath = getRequestAttributes().get("requestPath").toString();
		}
		try {
			setUpTemplatesConfiguration();
			String templateName = null;
			if (requestPath == null || requestPath.isEmpty()) {
				templateName = "main.tpl";
			} else if (requestPath.equalsIgnoreCase("createAccount")) {
				templateName = "register.tpl";
			} else if (requestPath.equalsIgnoreCase("addJob")) {
				templateName = "addJob.tpl";
			} else if (requestPath.equalsIgnoreCase("listImages")) {
				templateName = "listImages.tpl";
			}
			return new TemplateRepresentation(templateName, this.cfg, MediaType.TEXT_HTML);
		} catch (IOException e) {
			LOGGER.error("Could not load main UI", e);
			throw new ResourceException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

}
