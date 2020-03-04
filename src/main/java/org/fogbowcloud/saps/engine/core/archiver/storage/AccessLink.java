package org.fogbowcloud.saps.engine.core.archiver.storage;

public class AccessLink {
    private final String name;
    private final String url;

    public AccessLink(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}