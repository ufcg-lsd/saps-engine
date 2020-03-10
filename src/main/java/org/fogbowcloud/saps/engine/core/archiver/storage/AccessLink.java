package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.util.Objects;

public class AccessLink {

    private final String name;
    private final String url;

    public AccessLink(String name, String url) {
        if(Objects.isNull(name) || name.isEmpty()) {
            throw new IllegalArgumentException("The name field of Access Link may be not empty or null.");
        }
        if(Objects.isNull(url) || url.trim().isEmpty()) {
            throw new IllegalArgumentException("The url field of Access Link may be not empty or null.");
        }
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