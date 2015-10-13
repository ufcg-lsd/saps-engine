package org.fogbowcloud.scheduler.core.model;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.occi.request.RequestType;
import org.mockito.Mockito;

public class TestResourceHelper {

	public static Resource generateMockResource(String resourceId, Map<String, String> resourceMetadata, boolean connectivity){

		Resource fakeResource = mock(Resource.class);

		// Environment
		doReturn(connectivity).when(fakeResource).checkConnectivity();
		doReturn(resourceMetadata).when(fakeResource).getAllMetadata();
		doReturn(resourceId).when(fakeResource).getId();
		doReturn(resourceMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn(resourceMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn(resourceMetadata.get(Resource.METADATA_SSH_PORT)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_SSH_PORT));
		doReturn(resourceMetadata.get(Resource.METADATA_SSH_USERNAME_ATT)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_SSH_USERNAME_ATT));
		doReturn(resourceMetadata.get(Resource.METADATA_EXTRA_PORTS_ATT)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_EXTRA_PORTS_ATT));
		// Flavor
		doReturn(resourceMetadata.get(Resource.METADATA_IMAGE)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn(resourceMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn(resourceMetadata.get(Resource.METADATA_VCPU)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn(resourceMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		doReturn(resourceMetadata.get(Resource.METADATA_DISK_SIZE)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_DISK_SIZE));
		doReturn(resourceMetadata.get(Resource.METADATA_LOCATION)).when(fakeResource)
		.getMetadataValue(Mockito.eq(Resource.METADATA_LOCATION));

		when(fakeResource.match(Mockito.any(Specification.class))).thenCallRealMethod();
		
		return fakeResource;
	}
	
	public static Map<String, String> generateResourceMetadata(String host, String port, String userName,
			String extraPorts, RequestType requestType, String image, String publicKey, String cpuSize, String menSize,
			String diskSize, String location) {

		Map<String, String> resourceMetadata = new HashMap<String, String>();
		resourceMetadata.put(Resource.METADATA_SSH_HOST, host);
		resourceMetadata.put(Resource.METADATA_SSH_PORT, port);
		resourceMetadata.put(Resource.METADATA_SSH_USERNAME_ATT, userName);
		resourceMetadata.put(Resource.METADATA_EXTRA_PORTS_ATT, extraPorts);
		resourceMetadata.put(Resource.METADATA_REQUEST_TYPE, requestType.getValue());
		resourceMetadata.put(Resource.METADATA_IMAGE, image);
		resourceMetadata.put(Resource.METADATA_PUBLIC_KEY, publicKey);
		resourceMetadata.put(Resource.METADATA_VCPU, cpuSize);
		resourceMetadata.put(Resource.METADATA_MEN_SIZE, menSize);
		resourceMetadata.put(Resource.METADATA_DISK_SIZE, diskSize);
		resourceMetadata.put(Resource.METADATA_LOCATION, location);

		return resourceMetadata;
	}

}
