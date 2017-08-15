package org.fogbowcloud.saps.engine.core.repository;

import java.io.IOException;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

/**
 * Created by manel on 18/08/16.
 */
public interface INPERepository {

    /**
     *
     * @param imageData
     * @throws IOException
     */
    void downloadImage(final ImageTask imageData) throws IOException;

}
