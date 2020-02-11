package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;

public class PermanentStorageConstants {

    public static final String TASK_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator;
    public static final String INPUTDOWNLOADING_DIR_PATTERN = TASK_DIR_PATTERN + "inputdownloading";
    public static final String PREPROCESSING_DIR_PATTERN = TASK_DIR_PATTERN + "preprocessing";
    public static final String PROCESSING_DIR_PATTERN = TASK_DIR_PATTERN + "processing";
}
