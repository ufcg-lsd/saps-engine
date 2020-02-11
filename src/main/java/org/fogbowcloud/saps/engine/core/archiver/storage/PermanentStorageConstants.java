package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;

public class PermanentStorageConstants {

    // SAPS_EXPORT / FOLDER / TASK ID / INPUT

    public static final String INPUTDOWNLOADING_FOLDER = "inputdownloading";
    public static final String PREPROCESSING_FOLDER = "preprocessing";
    public static final String PROCESSING_FOLDER = "processing";

    public static final String SAPS_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s";

    public static final String SWIFT_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s";

    public static final String PERMANENT_STORAGE_TASK_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s";
    public static final String PERMANENT_STORAGE_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s" + File.separator + "%s";
}
