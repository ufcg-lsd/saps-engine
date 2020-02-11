package org.fogbowcloud.saps.engine.core.catalog.jdbc;

public class JDBCCatalogConstants {
    public final class Database {
        public static final String USERNAME = "datastore_username";
        public static final String PASSWORD = "datastore_password";
        public static final String DRIVER = "datastore_driver";
        public static final String URL_PREFIX = "datastore_url_prefix";
        public static final String NAME = "datastore_name";
        public static final String IP = "datastore_ip";
        public static final String PORT = "datastore_port";
    }

    public final class TablesName {
        public static final String TASKS = "TASKS";
        public static final String TIMESTAMPS = "TIMESTAMPS";
        public static final String USERS = "USERS";
        public static final String NOTIFY = "NOTIFY";
        public static final String DEPLOY_CONFIG = "DEPLOY_CONFIG";
        public static final String PROVENANCE_DATA = "PROVENANCE_DATA";
    }

    public final class Tables {
        public final class Task {
            public static final String ID = "task_id";
            public static final String PRIORITY = "priority";
            public static final String FEDERATION_MEMBER = "federation_member";
            public static final String STATE = "state";
            public static final String CREATION_TIME = "creation_time";
            public static final String UPDATED_TIME = "updated_time";
            public static final String STATUS = "status";
            public static final String ERROR_MSG = "error_msg";
            public static final String ARREBOL_JOB_ID = "arrebol_job_id";

            public final class Image {
                public static final String DATASET = "dataset";
                public static final String REGION = "region";
                public static final String DATE = "image_date";
            }

            public final class Algorithms {
                public final class Inputdownloading {
                    public static final String TAG = "inputdownloading_tag";
                    public static final String DIGEST = "inputdownloading_digest";
                }

                public final class Preprocessing {
                    public static final String TAG = "preprocessing_tag";
                    public static final String DIGEST = "preprocessing_digest";
                }

                public final class Processing {
                    public static final String TAG = "processing_tag";
                    public static final String DIGEST = "processing_digest";
                }
            }
        }

        public final class User {
            public static final String EMAIL = "user_email";
            public static final String NAME = "user_name";
            public static final String PASSWORD = "user_password";
            public static final String ENABLE = "active";
            public static final String NOTIFY = "user_notify";
            public static final String ADMIN_ROLE = "admin_role";
        }

        public final class Notify {
            public static final String SUBMISSION_ID = "submission_id";
        }

        public final class DeployConfig {
            public static final String NFS_SERVER_IP = "nfs_ip";
            public static final String NFS_SERVER_SSH_PORT = "nfs_ssh_port";
            public static final String NFS_SERVER_PORT = "nfs_port";
        }

        public final class ProvenanceData {
            public static final String INPUT_METADATA = "input_metadata";
            public static final String INPUT_OPERATING_SYSTEM = "input_operating_system";
            public static final String INPUT_KERNEL_VERSION = "input_kernel_version";
            public static final String PREPROCESSING_METADATA = "preprocessing_metadata";
            public static final String PREPROCESSING_OPERATING_SYSTEM = "preprocessing_operating_system";
            public static final String PREPROCESSING_KERNEL_VERSION = "preprocessing_kernel_version";
            public static final String OUTPUT_METADATA = "output_metadata";
            public static final String OUTPUT_OPERATING_SYSTEM = "output_operating_system";
            public static final String OUTPUT_KERNEL_VERSION = "output_kernel_version";
        }
    }

    public final class Queries {

        public final class Insert {
            public static final String TASK = "INSERT INTO " + JDBCCatalogConstants.TablesName.TASKS
                    + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            public static final String TIMESTAMP = "INSERT INTO " + JDBCCatalogConstants.TablesName.TIMESTAMPS
                    + " VALUES(?, ?, now())";

            public static final String USER = "INSERT INTO " + JDBCCatalogConstants.TablesName.USERS + " VALUES(?, ?, ?, ?, ?, ?)";
        }

        public final class Update {
            public static final String TASK = "UPDATE " + JDBCCatalogConstants.TablesName.TASKS + " SET " + JDBCCatalogConstants.Tables.Task.STATE + " = ?, "
                    + JDBCCatalogConstants.Tables.Task.UPDATED_TIME + " = now(), " + JDBCCatalogConstants.Tables.Task.STATUS + " = ?, " + JDBCCatalogConstants.Tables.Task.ERROR_MSG + " = ?, " + JDBCCatalogConstants.Tables.Task.ARREBOL_JOB_ID
                    + " = ? " + "WHERE " + JDBCCatalogConstants.Tables.Task.ID + " = ?";
        }

        public final class Select {
            public static final String TASKS = "SELECT * FROM " + JDBCCatalogConstants.TablesName.TASKS;

            public static final String USER = "SELECT * FROM " + JDBCCatalogConstants.TablesName.USERS + " WHERE " + JDBCCatalogConstants.Tables.User.EMAIL
                    + " = ?";

            public static final String TASKS_BY_STATE_ORDER_BY_PRIORITY_ASC = "SELECT * FROM " + JDBCCatalogConstants.TablesName.TASKS + " WHERE ? ORDER BY "
                    + JDBCCatalogConstants.Tables.Task.PRIORITY + " ASC";

            public static final String TASK = "SELECT * FROM " + JDBCCatalogConstants.TablesName.TASKS + " WHERE " + JDBCCatalogConstants.Tables.Task.ID
                    + " = ?";

            public static final String FILTER_TASKS = "SELECT * FROM " + JDBCCatalogConstants.TablesName.TASKS + " WHERE " + JDBCCatalogConstants.Tables.Task.STATE
                    + " = ? AND " + JDBCCatalogConstants.Tables.Task.Image.REGION + " = ? AND " + JDBCCatalogConstants.Tables.Task.Image.DATE + " BETWEEN ? AND ? AND " + JDBCCatalogConstants.Tables.Task.Algorithms.Preprocessing.TAG
                    + " = ? AND " + JDBCCatalogConstants.Tables.Task.Algorithms.Inputdownloading.TAG + " = ? AND " + JDBCCatalogConstants.Tables.Task.Algorithms.Processing.TAG + " = ?";
        }

        public final class Delete {
            public static final String TIMESTAMP = "DELETE FROM " + JDBCCatalogConstants.TablesName.TIMESTAMPS + " WHERE " + JDBCCatalogConstants.Tables.Task.ID
                    + " = ? AND " + JDBCCatalogConstants.Tables.Task.STATE + " = ? AND " + JDBCCatalogConstants.Tables.Task.UPDATED_TIME + " = ?";
        }
    }

    public final class CreateTable {
        public static final String USERS = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.USERS + "(" +
                JDBCCatalogConstants.Tables.User.EMAIL + " VARCHAR(255) PRIMARY KEY, " +
                JDBCCatalogConstants.Tables.User.NAME + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.User.PASSWORD + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.User.ENABLE + " BOOLEAN, " +
                JDBCCatalogConstants.Tables.User.NOTIFY + " BOOLEAN, " +
                JDBCCatalogConstants.Tables.User.ADMIN_ROLE + " BOOLEAN )";

        public static final String TASKS = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.TASKS + "(" +
                JDBCCatalogConstants.Tables.Task.ID + " VARCHAR(255) PRIMARY KEY, " +
                JDBCCatalogConstants.Tables.Task.Image.DATASET + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.Image.REGION + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.Image.DATE + " DATE, " +
                JDBCCatalogConstants.Tables.Task.STATE + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.ARREBOL_JOB_ID + " VARCHAR(100)," +
                JDBCCatalogConstants.Tables.Task.FEDERATION_MEMBER + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.PRIORITY + " INTEGER, " +
                JDBCCatalogConstants.Tables.User.EMAIL + " VARCHAR(255) REFERENCES " + JDBCCatalogConstants.TablesName.USERS + "(" + JDBCCatalogConstants.Tables.User.EMAIL + "), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Inputdownloading.TAG + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Inputdownloading.DIGEST + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Preprocessing.TAG + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Preprocessing.DIGEST + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Processing.TAG + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.Algorithms.Processing.DIGEST + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.CREATION_TIME + " TIMESTAMP, " +
                JDBCCatalogConstants.Tables.Task.UPDATED_TIME + " TIMESTAMP, " +
                JDBCCatalogConstants.Tables.Task.STATUS + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.ERROR_MSG + " VARCHAR(255) )";

        public static final String TIMESTAMPS = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.TIMESTAMPS + "(" +
                JDBCCatalogConstants.Tables.Task.ID + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.STATE + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.UPDATED_TIME + " TIMESTAMP )";

        public static final String NOTIFY = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.NOTIFY + "(" +
                JDBCCatalogConstants.Tables.Notify.SUBMISSION_ID + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.Task.ID + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.User.EMAIL + " VARCHAR(255), " +
                " PRIMARY KEY(" + JDBCCatalogConstants.Tables.Notify.SUBMISSION_ID + ", " + JDBCCatalogConstants.Tables.Task.ID + ", " + JDBCCatalogConstants.Tables.User.EMAIL + ") )";

        public static final String DEPLOY_CONFIG = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.DEPLOY_CONFIG + "(" +
                JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_IP + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_SSH_PORT + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_PORT + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.Task.FEDERATION_MEMBER + " VARCHAR(255), " +
                " PRIMARY KEY(" + JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_IP + ", " + JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_SSH_PORT + ", " + JDBCCatalogConstants.Tables.DeployConfig.NFS_SERVER_PORT + ", " + JDBCCatalogConstants.Tables.Task.FEDERATION_MEMBER + ") )";

        public static final String PROVENANCE_DATA = "CREATE TABLE IF NOT EXISTS " + JDBCCatalogConstants.TablesName.PROVENANCE_DATA + "(" +
                JDBCCatalogConstants.Tables.Task.ID + " VARCHAR(255) PRIMARY KEY, " +
                JDBCCatalogConstants.Tables.ProvenanceData.INPUT_METADATA + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.ProvenanceData.INPUT_OPERATING_SYSTEM + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.ProvenanceData.INPUT_KERNEL_VERSION + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.ProvenanceData.PREPROCESSING_METADATA + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.ProvenanceData.PREPROCESSING_OPERATING_SYSTEM + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.ProvenanceData.PREPROCESSING_KERNEL_VERSION + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.ProvenanceData.OUTPUT_METADATA + " VARCHAR(255), " +
                JDBCCatalogConstants.Tables.ProvenanceData.OUTPUT_OPERATING_SYSTEM + " VARCHAR(100), " +
                JDBCCatalogConstants.Tables.ProvenanceData.OUTPUT_KERNEL_VERSION + " VARCHAR(100) )";
    }
}

