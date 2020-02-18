package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;

public class PermanentStorageConstants {

    public static final String INPUTDOWNLOADING_FOLDER = "inputdownloading";
    public static final String PREPROCESSING_FOLDER = "preprocessing";
    public static final String PROCESSING_FOLDER = "processing";

    public static final String SAPS_TASK_STAGE_DIR_PATTERN =
            "%s" + File.separator + "%s" + File.separator + "%s";
}