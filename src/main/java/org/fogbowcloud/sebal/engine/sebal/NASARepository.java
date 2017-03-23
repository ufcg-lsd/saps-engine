package org.fogbowcloud.sebal.engine.sebal;

import java.io.IOException;

/**
 * Created by manel on 18/08/16.
 */
public interface NASARepository {

    /**
     *
     * @param imageData
     * @throws IOException
     */
    void downloadImage(final ImageData imageData) throws IOException;

}
