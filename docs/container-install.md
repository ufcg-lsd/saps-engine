# Install and configure Container

SAPS componentes are deployed as Docker containers. Thus, before proper installing them, Docker needs to be installed in the virtual machines provisioned to run the SAPS service.

To install Docker in a Debian based virtual machine, the following commands should be executed:

  ```
  1. apt-get remove docker docker-engine docker.io
  2. apt-get update
  3. apt-get install linux-image-extra-$(uname -r) linux-image-extra-virtual
  4. apt-get update
  5. apt-get install apt-transport-https ca-certificates curl software-properties-common
  6. curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  7. apt-key fingerprint 0EBFCD88
  8. add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
  9. apt-get update
  10. apt-get install docker-ce
  11. apt-cache madison docker-ce
  12. apt-get install docker-ce=<VERSION>
  ```
