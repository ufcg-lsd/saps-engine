# Install and Configure Arrebol worker

The Arrebol worker is a job processing resource that is given one or more tasks to perform making Arrebol manage each of its steps and update important information for its users such as the state of the job, which command is being executed and the exitcode of the commands. The Scheduler component uses Arrebol to send the same task 3 times, each time in the respective phases of the SAPS pipeline (inputdownloading, preprocessing and processing).
  
## Dependencies

In an apt-based Linux distribution, enter the commands below to install the Docker software.

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo apt-key fingerprint 0EBFCD88
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io
```


In addition to Docker, you will need to install the NFS client with the following command:

```bash
sudo apt-get update
sudo apt-get install nfs-common
```

## Configure

### Docker

First, need to configure Docker to make Arrebol communicate with the worker’s VM as follows:

```
sudo vim /lib/systemd/system/docker.service
```

Change the value of the `ExecStart` variable to `/usr/bin/dockerd -H fd:// -H=tcp://0.0.0.0:5555 --containerd=/run/containerd/containerd.sock`, 
getting something like:

```
...
ExecStart=/usr/bin/dockerd -H fd:// -H=tcp://0.0.0.0:5555 --containerd=/run/containerd/containerd.sock
...
```

Restart the Docker service to update to the new configuration

```
sudo systemctl daemon-reload
sudo service docker restart
```

### NFS Client

It is necessary to mount the NFS temp storage on the host for the purpose of SAPS operation, to do this, use the name of the root folder `/nfs` **(there should not be another name for this folder)** for the mapping occurring, being done as follows:

```
sudo mkdir -p /nfs
sudo mount -t nfs <nfs-temp-storage-ip>:<nfs-temp-storage-path> /nfs
```

Where:
- **nfs-temp-storage-ip** is the IP of the VM that contains the NFS temp storage
- **nfs-temp-storage-path** is the same path used by SAPS for NFS temp storage as mentioned [here](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/archiver-install.md#temporary-storage)

## Test

### NFS Client

Go to the VM that is NFS temp storage and run the following command:
```bash
touch <path-nfs-temp-storage>/test
```

Then, go to the worker VM and run this:
```
ls /nfs
```

If the empty file `test` was viewed by the worker, the sharing was successful.

