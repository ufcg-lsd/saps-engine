package org.fogbowcloud.saps.engine.core.util;

import java.util.*;

import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;

import org.junit.Test;

public class DatasetUtilTests {

    public Set<String> toSet(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

    @Test
    public void getSatsInOperationByYearTest() {
        Set<String> expected;
        Set<String> actual;

        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1983));
        Assert.assertEquals(0, actual.size());

        expected = new HashSet<String>(Arrays.asList(new String[]{SapsPropertiesConstants.DATASET_LT5_TYPE}));
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1984));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.DATASET_LT5_TYPE,
                SapsPropertiesConstants.DATASET_LE7_TYPE);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1999));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.DATASET_LT5_TYPE,
                SapsPropertiesConstants.DATASET_LE7_TYPE,
                SapsPropertiesConstants.DATASET_LC8_TYPE);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(2013));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.DATASET_LE7_TYPE,
                SapsPropertiesConstants.DATASET_LC8_TYPE);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(Integer.MAX_VALUE - 1));
        Assert.assertEquals(expected, actual);
        
        expected = new HashSet<String>();
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1980));
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void getMostRecentDataSetInOperationTest() {
    	String expected;
    	String actual;
    	
    	expected = SapsPropertiesConstants.DATASET_LC8_TYPE;
    	actual = DatasetUtil.getMostRecentDataSetInOperation(2013);
    	Assert.assertEquals(expected, actual);
    	
    	expected = SapsPropertiesConstants.DATASET_LE7_TYPE;
    	actual = DatasetUtil.getMostRecentDataSetInOperation(1999);
    	Assert.assertEquals(expected, actual);
    	
    	expected = SapsPropertiesConstants.DATASET_LT5_TYPE;
    	actual = DatasetUtil.getMostRecentDataSetInOperation(1984);
    	Assert.assertEquals(expected, actual);
    	
    	Assert.assertNull(DatasetUtil.getMostRecentDataSetInOperation(1980));
    }

}
