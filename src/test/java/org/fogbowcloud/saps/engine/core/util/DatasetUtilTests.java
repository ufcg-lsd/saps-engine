package org.fogbowcloud.saps.engine.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;

import org.junit.Test;

public class DatasetUtilTests {

	@Test
	public void getSatsInOperationByYearTest() {
		Assert.assertEquals(0, DatasetUtil.getSatsInOperationByYear(1983).size());

		String[] sats = new String[] { SapsPropertiesConstants.DATASET_LT5_TYPE };
		Assert.assertEquals(Arrays.toString(sats),
				DatasetUtil.getSatsInOperationByYear(1984).toString());

		sats = new String[] { SapsPropertiesConstants.DATASET_LT5_TYPE,
				SapsPropertiesConstants.DATASET_LE7_TYPE };
		ArrayList<String> ans = DatasetUtil.getSatsInOperationByYear(1999);
		Collections.sort(ans);
		Assert.assertEquals(Arrays.toString(sats), ans.toString());

		sats = new String[] { SapsPropertiesConstants.DATASET_LT5_TYPE,
				SapsPropertiesConstants.DATASET_LE7_TYPE,
				SapsPropertiesConstants.DATASET_LC8_TYPE };
		ans = DatasetUtil.getSatsInOperationByYear(2013);
		Collections.sort(ans);
		Assert.assertEquals(Arrays.toString(sats), ans.toString());
		
		sats = DatasetUtil.satsInPresentOperation;
		Arrays.sort(sats);
		ans = DatasetUtil.getSatsInOperationByYear(Integer.MAX_VALUE - 1);
		Collections.sort(ans);
		Assert.assertEquals(Arrays.toString(sats), ans.toString());
	}

}
