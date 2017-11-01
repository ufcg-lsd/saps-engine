# Install and configure Docker

To install Docker in a Debian based virtual machine, the following commands should be executed:

  ```
  # Removal of older Docker versions (if it exist)
  apt-get remove docker docker-engine docker.io
  
  # Update
  apt-get update
  
  # Install linux-image-extra-* packages, which allow Docker to use the aufs storage drivers
  apt-get install linux-image-extra-$(uname -r) linux-image-extra-virtual
  
  # Update
  apt-get update
  
  # Install packages to allow apt to use a repository over HTTPS
  apt-get install apt-transport-https ca-certificates curl software-properties-common
  
  # Add Docker’s official GPG key
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  
  # Verify that you now have the key with the fingerprint 9DC8 5822 9FC7 DD38 854A E2D8 8D81 803C 0EBF CD88
  apt-key fingerprint 0EBFCD88
  
  # Set up the stable repository
  add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
  
  # Update
  apt-get update
  
  # Install the latest version of Docker CE
  apt-get install docker-ce
  
  # List the available Docker CE versions
  apt-cache madison docker-ce
  
  # Install Docker CE version obtained from previous command
  apt-get install docker-ce=<VERSION>
  ```

In the previous listing, the values indicated by names within the symbols <>, such as \<VERSION\>, should be chosen and assigned appropriately by the system administrator during installation. For the specific case of \<VERSION\>, it should be replaced by the string that appears in the second column of the output of the command 11 in the previous listing.
