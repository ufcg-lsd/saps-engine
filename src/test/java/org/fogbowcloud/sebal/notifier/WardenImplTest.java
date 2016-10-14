package org.fogbowcloud.sebal.notifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
		wardenImpl = new WardenImpl(properties, dbUtilsImpl);
	}
	
	@Test
	public void testSQLExceptionWhileGetPending() throws SQLException {				
		Mockito.doThrow(new SQLException()).when(dbUtilsImpl).getUsersToNotify();		
		
		Assert.assertTrue(wardenImpl.getPending().isEmpty());
	}
	
	@Test
	public void testWardListWithCorrectContent() throws SQLException {
		List<Ward> listWards = new ArrayList<Ward>();
		
		Ward ward1 = Mockito.mock(Ward.class);
		Ward ward2 = Mockito.mock(Ward.class);
		Ward ward3 = Mockito.mock(Ward.class);
		
		listWards.add(ward1);
		listWards.add(ward2);
		listWards.add(ward3);
		
		Mockito.doReturn(listWards).when(dbUtilsImpl).getUsersToNotify();
		
		List<Ward> wards = wardenImpl.getPending();
		
		Assert.assertEquals(listWards, wards);
	}

}