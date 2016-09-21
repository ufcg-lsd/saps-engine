package org.fogbowcloud.sebal.notifier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class WardenImpl implements Warden {

	public static final Logger LOGGER = Logger.getLogger(WardenImpl.class);


	public void init() {
		while (true) {
			Collection<Ward> notified = new LinkedList<Ward>();
			for (Ward ward : getPending()) {
				if (reached(ward)) {
					ImageData imageData = getImageData(ward.getImageName());
					try {
						doNotify(ward.getEmail(), ward.getJobId(), imageData);
						notified.add(ward);
					} catch (Throwable e) {
						LOGGER.error("Could not notify the user on: " + ward.getEmail() + " about " + ward, e);
					}
				}
			}
			removeNotified(notified);
		}
	}

	@Override
	public void doNotify(String email, String jobId, ImageData context) {
	}
	
	private void removeNotified(Collection<Ward> notified) {

	}

	private ImageData getImageData(String imageName) {
		return null;
	}

	private List<Ward> getPending() {
		return null;
	}

	private boolean reached(Ward ward) {
		return false;
	}

	private class Ward {

		private final String imageName;
		private final ImageState state;
		private final String jobId;
		private final String email;

		public Ward(String imageName, ImageState state, String jobId, String email) {
			this.imageName = imageName;
			this.state = state;
			this.email = email;
			this.jobId = jobId;
		}

		@Override
		public String toString() {
			return "Ward [imageName=" + imageName + ", state=" + state + ", jobId=" + jobId + ", email=" + email + "]";
		}

		public String getJobId() {
			return jobId;
		}

		public String getEmail() {
			return email;
		}

		public String getImageName() {
			return imageName;
		}

		public ImageState getState() {
			return state;
		}

	}
}
