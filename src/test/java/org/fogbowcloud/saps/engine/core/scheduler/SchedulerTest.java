package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.scheduler.executor.JobExecutionService;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.ArrebolJobExecutionService;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class SchedulerTest {

    private static final Properties properties = new Properties();
    private static final ImageTaskState[] readyState = {ImageTaskState.READY};
    private static final ImageTaskState[] downloadedState = {ImageTaskState.DOWNLOADED};
    private static final ImageTaskState[] createdState = {ImageTaskState.CREATED};
    private static final Catalog catalog = mock(Catalog.class);
    private static final JobExecutionService jes = mock(ArrebolJobExecutionService.class);
    private static final Selector selector = new DefaultRoundRobin();
    private static final ScheduledExecutorService ses =  Executors.newScheduledThreadPool(1);
    private static final SapsImage task01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED,
            SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
            new Timestamp(1), "", "");
    private static final SapsImage task02 = new SapsImage("2", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED,
            SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
            new Timestamp(1), "", "");
    private static final SapsImage task03 = new SapsImage("3", "landsat_8", "217066", new Date(), ImageTaskState.READY,
            SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
            new Timestamp(1), "", "");

    @BeforeClass
    public static void init() {
        properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "db_ip");
        properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "db_port");
        properties.put(SapsPropertiesConstants.IMAGE_WORKER, "image_worker");
        properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR, "10000");
        properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER, "10000");
        properties.put(SapsPropertiesConstants.ARREBOL_BASE_URL, "arrebol_base_url");
    }

    @Before
    public void setUp() {
        List<SapsImage> createdTasks = Collections.singletonList(task01);
        List<SapsImage> downloadedTasks = Collections.singletonList(task02);
        List<SapsImage> readyTasks = Collections.singletonList(task03);

        when(catalog.getTasksByState(readyState)).thenReturn(readyTasks);
        when(catalog.getTasksByState(downloadedState)).thenReturn(downloadedTasks);
        when(catalog.getTasksByState(createdState)).thenReturn(createdTasks);
    }

    @Test
    public void testSelectTaskWithoutTaskOnCatalog()
            throws IOException, SapsException {
        Scheduler scheduler = new Scheduler(properties, catalog, ses, jes, selector);

        when(catalog.getTasksByState(readyState)).thenReturn(new LinkedList<>());
        when(catalog.getTasksByState(downloadedState)).thenReturn(new LinkedList<>());
        when(catalog.getTasksByState(createdState)).thenReturn(new LinkedList<>());
        when(jes.getWaitingJobs()).thenReturn(0L);

        List<SapsImage> selectedTasks = scheduler.selectTasks();
        Assert.assertEquals(new LinkedList<SapsImage>(), selectedTasks);
    }

    @Test
    public void testTasksAmountStCapacity() throws IOException, SapsException {
        Scheduler scheduler = new Scheduler(properties, catalog, ses, jes, selector);
        when(jes.getWaitingJobs()).thenReturn(15L);

        List<SapsImage> expectedTask = Arrays.asList(task03, task02, task01);
        List<SapsImage> selectedTasks = scheduler.selectTasks();
        Assert.assertEquals(expectedTask, selectedTasks);
    }

    @Test
    public void  testTasksAmountGtCapacity() throws IOException, SapsException {
        Scheduler scheduler = new Scheduler(properties, catalog, ses, jes, selector);

        when(jes.getWaitingJobs()).thenReturn(18L);

        List<SapsImage> expectedTask = Arrays.asList(task03, task02);
        List<SapsImage> selectedTasks = scheduler.selectTasks();
        Assert.assertEquals(expectedTask, selectedTasks);
    }

    @Test
    public void  testMaxCapacity() throws IOException, SapsException {
        Scheduler scheduler = new Scheduler(properties, catalog, ses, jes, selector);
        when(jes.getWaitingJobs()).thenReturn(20L);

        List<SapsImage> expectedTask = new LinkedList<>();
        List<SapsImage> selectedTasks = scheduler.selectTasks();
        Assert.assertEquals(expectedTask, selectedTasks);
    }

}
