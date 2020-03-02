package org.fogbowcloud.saps.engine.core.archiver;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.fogbowcloud.saps.engine.core.archiver.storage.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageType;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.jdbc.JDBCCatalog;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

public class ArchiverMain {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(args[0]);
        properties.load(input);

        Archiver Fetcher = createArchiver(properties);
        Fetcher.start();
    }

    private static Archiver createArchiver(Properties properties)
        throws PermanentStorageException, SapsException {
        PermanentStorage permanentStorage = createPermanentStorage(properties);
        Catalog catalog = new JDBCCatalog(properties);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Archiver archiver = new Archiver(properties, catalog, permanentStorage, executor);
        return archiver;
    }

    private static PermanentStorage createPermanentStorage(Properties properties)
        throws PermanentStorageException, SapsException {
        String permanentStorageType = properties
            .getProperty(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE);
        if (PermanentStorageType.SWIFT.toString().equalsIgnoreCase(permanentStorageType)) {
            return new SwiftPermanentStorage(properties);
        } else if (PermanentStorageType.NFS.toString().equalsIgnoreCase(permanentStorageType)) {
            return new NfsPermanentStorage(properties);
        }
        throw new SapsException("Failed to recognize type of permanent storage");
    }
}
