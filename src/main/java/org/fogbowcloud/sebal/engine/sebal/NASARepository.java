package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

    /**
     * It gets a imageName:downloadURL for a batch of image names. It gets
     * only downloadURLs for those images that are present in the remote repository.
     *
     * @param imageListFile
     * @return
     * @throws IOException
     */
    Map<String, String> getDownloadLinks(File imageListFile) throws IOException;

}
