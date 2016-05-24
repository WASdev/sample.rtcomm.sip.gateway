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

Pre-requisite: [Download WAS Liberty](https://developer.ibm.com/wasdev/getstarted/)

We recommend that you set an environment variable that points to your Liberty installation:
```bash
export WLP_DIR=/path/to/wlp/
```

You will also need to install the Liberty features required:

```bash
$ $WLP_DIR/bin/installUtility install --acceptLicense rtcomm-1.0 rtcommGateway-1.0 sipServlet-1.1 localConnector-1.0 mediaServerControl-1.0 jndi-1.0
```
As well as the temporary dialogic feature for Liberty that's downloaded from this repository, you must also set the Liberty user directory:
```bash
$ export WLP_USER_DIR=/path/to/webrtc-gateway/webrtc-gateway-wlpcfg
$ $WLP_DIR/bin/installUtility install /path/to/webrtc-gateway/com.vendor.dialogic.javax.media.mscontrol.LIBERTY.snapshot_5.0.1.esa
```

Use the following to start the server and run the application:

```bash
$ export WLP_USER_DIR=/path/to/webrtc-gateway/webrtc-gateway-wlpcfg
$ $WLP_DIR/bin/server run webrtcGatewayServer
```

* `run` runs the server in the foreground.
* `start` runs the server in the background. Look in the logs directory for console.log to see what's going on, e.g.

```bash
$ tail -f ${WLP_USER_DIR}/servers/rtcommSipGatewayServer/logs/console.log
```
