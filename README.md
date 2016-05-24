# WebRTCGateway Application
Sample application that uses JSR 289 and JSR 309 to Connect a SIP soft phone and the Rtcomm WebRTC endpoint using a media server as a mediator and translator for the media.


### Live Demo Example

[Browser Call](https://browser-call.wasdev.developer.ibm.com/#/) - This sample application demonstrates how you can use a telephone or mobile phone number to connect to a web or mobile application.

## Set Up
This project requires [Maven 3.x](https://maven.apache.org/).

You will also need to setup the Rtcomm Client, MQTT Broker  and Linphone for testing:

+ Setup the Rtcomm Client/MQTT Broker [here](docs/setting-up-rtcomm-client.md)
+ Setup Linphone [here](docs/setup-linphone.md)


## Building and Running
You can run the sample through the command line or Eclipse WDT, but you will need to **configure** the application properties. (View [Configuration](#configuration))

- [Command Line](/docs/command-line.md)
- [Eclipse and Websphere Developer Tools](/docs/eclipse-wdt.md)

Every time you modify the property files you will need to re-deploy the application.

<!-- For deployment as a [Docker Container](https://www.docker.com/) click [here](/docs/docker-deployment.md). -->

## <a name="configuration"></a>Configuration

In order for the application to work, you need to setup the  properties for the JSR309 Driver, the rtcomm feature and for the SIP Endpoint. Set the properties by editing the following property files:

```
rtcomm-sip-gateway
+ rtcomm-sip-gateway-application
+ - src/main/webapp
+ -- WEB-INF/
+ --- JSR309Driver.properties <----- Configure
+ --- sip.properties <----- Configure
```

In each file you will see more details for each property.

#### Media Server Requirement

In order for the Rtcomm Gateway to work a Media Server is required for transcoding.
For more information on the Dialogic JSR309 and XMS, configuration details can be found [here](http://www.dialogic.com/manuals/xms/xms3.1.aspx).

You will also need to configure the *server.xml* located in <code>rtcomm-sip-gateway-wlpcfg/servers/rtcommSipGatewayServer/server.xml</code>. For more information on each section, click the following links:

+ [Rtcomm Configuration](https://www.ibm.com/support/knowledgecenter/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/twlp_config_rtcomm.html)
  + [Rtcomm Gateway](https://www.ibm.com/support/knowledgecenter/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/twlp_config_webrtc_gateway.html)
+ [SIP in Liberty](https://www.ibm.com/support/knowledgecenter/was_beta_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/cwlp_sip_sipwlp.html?lang=en)


## Notice

© Copyright IBM Corporation 2016.

## License

```text
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
