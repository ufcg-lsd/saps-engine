package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
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

	private Properties createDefaultProperties() {
		Properties properties = new Properties();
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "");
		properties.put(SapsPropertiesConstants.IMAGE_WORKER, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER, "");
		properties.put(SapsPropertiesConstants.ARREBOL_BASE_URL, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER, "");
		properties.put(SapsPropertiesConstants.SAPS_EXPORT_PATH, "");
		properties.put(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE, "swift");
		
		return properties;
	}

	@Test (expected = SapsException.class)
	public void testNoSetProperty() throws SapsException, SQLException {
		Properties properties = new Properties();
		Archiver archiver = new Archiver(properties);
	}
	
	@Test (expected = SapsException.class)
	public void testNoSetPropertyForSwiftAPIClient()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Properties properties = createDefaultProperties();
		Archiver archiver = new Archiver(properties, imageStore, new ArchiverHelper());
	}
}
