package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;

public class PermanentStorageConstants {

    //FIXME Change public to protected
    public static final String INPUTDOWNLOADING_DIR = "inputdownloading";
    public static final String PREPROCESSING_DIR = "preprocessing";
    public static final String PROCESSING_DIR = "processing";

    public static final String SAPS_TASK_STAGE_DIR_PATTERN =
        "%s" + File.separator + "%s" + File.separator + "%s";
}
