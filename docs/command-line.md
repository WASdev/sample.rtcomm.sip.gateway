## Building and running the sample using the command line

### Clone Git Repo

Clone the application project to your working directory
```bash
$ git clone https://github.com/WASdev/sample.rtcomm.sip.gateway.git
```

### Building the sample with [Apache Maven](http://maven.apache.org/) commands

```bash
$ mvn install
```

In addition to publishing the war to the local maven repository, the built war file is copied into the apps directory of the server configuration located in the rtcomm-sip-gateway-wlpcfg directory:

```text
webrtc-gateway-wlpcfg
 +- servers
     +- webrtcGatewayServer                    <-- specific server configuration
        +- server.xml                          <-- server configuration
        +- apps                             <- directory for applications
           +- rtcomm-sip-gateway-application.war           <- sample application
        +- logs                                <- created by running the server locally
        +- workarea                            <- created by running the server locally
```

### Running the application locally

You can run the sample by using Maven:

```bash
mvn -f rtcomm-sip-gateway-wlpcfg/pom.xml liberty:run-server

This will run the server. You can check the logs in the directory <code>rtcomm-sip-gateway-wlpcfg/servers/rtcommSipGatewayServer/logs</code>.

Once the application is configured and running, register on the Rtcomm Client as a user and place a call to them using Linphone.
