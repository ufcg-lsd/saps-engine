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

    @Override
    public String toString() {
        return "AccessLink{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessLink that = (AccessLink) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(url, that.url);
    }

}