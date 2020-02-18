package org.fogbowcloud.saps.engine.core.archiver.storage;

public enum PermanentStorageType {
    NFS("nfs"),
    SWIFT("swift");

    private final String type;

    PermanentStorageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
