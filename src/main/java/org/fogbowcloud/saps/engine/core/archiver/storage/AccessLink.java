package org.fogbowcloud.saps.engine.core.archiver.storage;

public class AccessLink {
    private String name;
    private String url;

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