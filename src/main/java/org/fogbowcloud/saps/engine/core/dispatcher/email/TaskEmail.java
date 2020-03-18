package org.fogbowcloud.saps.engine.core.dispatcher.email;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

import java.util.Date;
import java.util.List;

public class TaskEmail {

    @SerializedName("id")
    private final String id;

    @SerializedName("image_region")
    private final String imageRegion;

    @SerializedName("image_collection_name")
    private final String imageCollectionName;

    @SerializedName("image_date")
    private final Date imageDate;

    @SerializedName("access_links")
    private final List<AccessLink> accessLinks;

    public TaskEmail(String id, String imageRegion, String imageCollectionName, Date imageDate, List<AccessLink> accesslinks) {
        this.id = id;
        this.imageRegion = imageRegion;
        this.imageCollectionName = imageCollectionName;
        this.imageDate = imageDate;
        this.accessLinks = accesslinks;
    }

    public TaskEmail(SapsImage sapsTask, List<AccessLink> accessLinks) {
        this(sapsTask.getTaskId(), sapsTask.getRegion(), sapsTask.getCollectionTierName(), sapsTask.getImageDate(),
                accessLinks);
    }

}
