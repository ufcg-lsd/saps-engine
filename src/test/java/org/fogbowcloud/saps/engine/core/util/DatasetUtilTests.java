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

        expected = new HashSet<String>(Arrays.asList(new String[]{SapsPropertiesConstants.LANDSAT_5_DATASET}));
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1984));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.LANDSAT_5_DATASET,
                SapsPropertiesConstants.LANDSAT_7_DATASET);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(1999));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.LANDSAT_5_DATASET,
                SapsPropertiesConstants.LANDSAT_7_DATASET,
                SapsPropertiesConstants.LANDSAT_8_DATASET);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(2013));
        Assert.assertEquals(expected, actual);

        expected = toSet(SapsPropertiesConstants.LANDSAT_7_DATASET,
                SapsPropertiesConstants.LANDSAT_8_DATASET);
        actual = new HashSet<String>(DatasetUtil.getSatsInOperationByYear(Integer.MAX_VALUE - 1));
        Assert.assertEquals(expected, actual);
    }

}
