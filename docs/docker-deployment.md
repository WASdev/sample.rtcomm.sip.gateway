### Deploy as a [Docker Container](https://www.docker.com)

Once you have configured the Liberty server with the correct Media server, MQTT and SIP details you can build the <code>docker</code> image:

```bash
docker build -t rtcomm-sip-gateway
```
This will build the image and you can either run it locally, push it to a repository or run it on Bluemix.

##### Run the Docker Image Locally

This will launch a docker instance on your machine named 'myRtcommSipGateway'
```bash
docker run -p 9080:9080 -p 9443:9443 -p 5060:5060 --name myRtcommSipGateway rtcomm-sip-gateway
```

##### Run the Docker in [Bluemix](https://bluemix.net)

1. Login to Cloud Foundry and IBM Containers
<pre>
cf login -u [username] -o [organization] -s [space] && \
cf ic login
</pre>
2. Tag the Image for Bluemix
<pre>
  docker tag -f rtcomm-sip-gateway registry.ng.bluemix.net/[container_space]/rtcomm-sip-gateway
</pre>
3. Push the Image to bluemix
<pre>
  docker push registry.ng.bluemix.net/[container_space]/rtcomm-sip-gateway
</pre>
4. Use the Bluemix UI to deploy the docker container instance or use the [command line tool](https://console.ng.bluemix.net/docs/containers/container_cli_reference_cfic.html#container_cli_reference_cf)
