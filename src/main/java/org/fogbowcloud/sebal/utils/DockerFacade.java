package org.fogbowcloud.sebal.utils;

import java.util.List;

public interface DockerFacade {

    /**
     * Get, update (if in earlier version), or ignore (if in the latest version) a Docker image.
     * @param repository The repository name.
     * @param tag The tag name.
     * @return True, if it was successfully pulled, if raised and error, False.
     */
    public boolean pullImage(String repository, String tag);

    /**
     * Lists all Docker images.
     * @return All images ID.
     */
    public List<String> listAllImages();

    /**
     * Retrieve a specific image ID to a given repository and tag name.
     * @param repository The repository name.
     * @param tag The tag name.
     * @return Image ID.
     */
    public String getImageId(String repository, String tag);

    /**
     * Runs a container of a given image repository and tag.
     * @param repository The Docker image repository name.
     * @param tag The Docker tag name.
     * @return The running container ID.
     */
    public String runContainer(String repository, String tag);

    /**
     * Runs a container of a given image repository and tag, but mapping a host directory to a container's one.
     * @param repository The Docker image repository name.
     * @param tag The Docker tag name.
     * @param hostPath A directory path in host.
     * @param containerPath A directory path into the container.
     * @return The running container ID.
     */
    public String runMappedContainer(String repository, String tag, String hostPath, String containerPath);

    /**
     * List all containers
     * @return All containers ID
     */
    public List<String> listAllContainers();

    /**
     * This may return all containers ID that matches the repository name.
     * @param repositoryName The repository name to be filtered.
     * @return All containers ID.
     */
    public List<String> getContainersId(String repositoryName);

    /**
     * Get all N latest created containers.
     * @param n number of containers to be retrieved.
     * @return The containers ID.
     */
    public List<String> getNLatestCreatedContainers(int n);

    /**
     * Get all Docker containers currently running.
     * @return Containers ID.
     */
    public List<String> getRunningContainer();

    /**
     * Kill all containers.
     */
    public void killAllContainers();

    /**
     * Kill a specific container.
     * @param containerId The container ID to be killed.
     * @return True, if it was successfully removed, otherwise, False.
     */
    public boolean killContainer(String containerId);

}
