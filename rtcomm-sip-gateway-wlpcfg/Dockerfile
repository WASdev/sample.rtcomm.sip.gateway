# (C) Copyright IBM Corporation 2015.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM websphere-liberty:kernel

ENV JSR309_FEATURE=com.vendor.dialogic.javax.media.mscontrol.LIBERTY.snapshot_5.0.1.esa
ENV JSR309_FEATURE_URL=https://www.dialogic.com/files/jsr-309/5.2GA/${JSR309_FEATURE}

# Install Ubuntu packages
RUN apt-get update \
    && apt-get -y install vim \
		&& apt-get clean \
		&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

## Install the Liberty features

RUN installUtility install --acceptLicense rtcomm-1.0 rtcommGateway-1.0 sipServlet-1.1 localConnector-1.0 mediaServerControl-1.0 wab-1.0 jndi-1.0

# Install the JSR309 Feature

RUN wget ${JSR309_FEATURE_URL} -P /tmp/

RUN installUtility install --acceptLicense /tmp/${JSR309_FEATURE}

COPY servers/rtcommSipGatewayServer/server.xml /config/

ADD servers/rtcommSipGatewayServer/apps/ /config/apps/

# ADD rtcomm-sip-gateway-application/target/rtcomm-sip-gateway-application.war /config/apps

EXPOSE 5060
EXPOSE 5060/udp
