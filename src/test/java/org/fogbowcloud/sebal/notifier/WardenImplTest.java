package org.fogbowcloud.sebal.notifier;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.sebal.engine.sebal.bootstrap.DBUtilsImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class WardenImplTest {
	
	// TODO: TESTS
	// 1) getPending() returning correct ward list
	// 2) getImageData() fail
	// 3) reached() fail
	// 4) reached() fail
	// 5) reached() behaving correctly
	// 6) doNotify() fail
	// 7) doNotify() sending email correctly
	// 8) removeNotified() fail
	// 9) removeNotified() behaving correctly
	
	private Properties properties;
	private DBUtilsImpl dbUtilsImpl;
	private WardenImpl wardenImpl;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = Mockito.mock(Properties.class);
		dbUtilsImpl = Mockito.mock(DBUtilsImpl.class);
		wardenImpl = Mockito.mock(WardenImpl.class);
	}
	
	@Test
	public void testSQLExceptionWhileGetPending() throws SQLException {
		Mockito.doThrow(new SQLException()).when(dbUtilsImpl).getUsersToNotify();		
		
		Assert.assertTrue(wardenImpl.getPending().isEmpty());
	}
	
	@Test
	public void testWardListWithCorrectContent() throws SQLException {
		Map<String, String> mapUsersImages = new HashMap<String, String>();
		mapUsersImages.put("image-1", "email-1");
		mapUsersImages.put("image-2", "email-2");
		mapUsersImages.put("image-3", "email-3");
		
		Mockito.doReturn(mapUsersImages).when(dbUtilsImpl).getUsersToNotify();
		
		List<Ward> wards = wardenImpl.getPending();
		
		Assert.assertEquals("email-1", wards.get(0).getEmail());
		Assert.assertEquals("email-2", wards.get(1).getEmail());
		Assert.assertEquals("email-3", wards.get(2).getEmail());
		
		Assert.assertEquals("image-1", wards.get(0).getImageName());
		Assert.assertEquals("image-2", wards.get(1).getImageName());
		Assert.assertEquals("image-3", wards.get(2).getImageName());
	}

}