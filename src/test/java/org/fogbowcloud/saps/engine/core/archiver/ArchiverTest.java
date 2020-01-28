package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ArchiverTest {
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	private Properties createArchiverDefaultProperties() {
		Properties properties = new Properties();
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER, "");
		properties.put(SapsPropertiesConstants.SAPS_EXPORT_PATH, "");
		properties.put(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE, "swift");

		return properties;
	}

	private Properties createArchiverSwiftDefaultProperties() {
		Properties properties = createArchiverDefaultProperties();
		properties.put(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX, "");
		properties.put(SapsPropertiesConstants.SWIFT_CONTAINER_NAME, "");

		return properties;
	}

	private Properties createDefaultAllProperties() {
		Properties properties = createArchiverSwiftDefaultProperties();
		properties.put(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID, "");
		properties.put(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID, "");
		properties.put(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD, "");
		properties.put(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL, "");
		properties.put(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL, "");

		return properties;
	}

	@Test(expected = SapsException.class)
	public void testNoSetProperty() throws SapsException, SQLException {
		Properties properties = new Properties();
		new Archiver(properties);
	}

	@Test(expected = SapsException.class)
	public void testNoSetPropertyForSwiftPermanentStorage() throws SapsException {
		Catalog imageStore = mock(Catalog.class);
		Properties properties = createArchiverDefaultProperties();
		new Archiver(properties, imageStore, new ArchiverHelper());
	}

	@Test(expected = SapsException.class)
	public void testNoSetPropertyForSwiftAPIClient() throws SapsException {
		Catalog imageStore = mock(Catalog.class);
		Properties properties = createArchiverSwiftDefaultProperties();
		new Archiver(properties, imageStore, new ArchiverHelper());
	}

	@Test(expected = SapsException.class)
	public void testAllSetProperty() throws SapsException {
		Catalog imageStore = mock(Catalog.class);
		Properties properties = createDefaultAllProperties();
		new Archiver(properties, imageStore, new ArchiverHelper());
	}
}
