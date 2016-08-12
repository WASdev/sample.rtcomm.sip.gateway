/**
 * (C) Copyright IBM Corporation 2016.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.rtcommsipgateway.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.PropertyInfo;


public class JSR309Utils {

	private static Logger log = Logger.getLogger(JSR309Utils.class.getName());

	public static final String NETWORK_CONNECTION_ATTRIBUTE = "NETWORK_CONNECTION";
	public static final String SIP_SESSION_ATTRIBUTE = "SIP_SESSION";
	public static final String NETWORKCONNECTION_PROPERTY_PREFIX = "networkconnection.";

	private static MsControlFactory s_msFactory;
	private static Properties s_factoryOverrideProperties;

	public static void initMsControlFactory(MsControlFactory msFactory, Properties factoryOverrideProperties){
		s_msFactory = msFactory;
		s_factoryOverrideProperties = factoryOverrideProperties;
	}

	public static Properties createFactoryProperties(Driver driver, Properties factoryOverrideProperties){
		// gets default driver properties for MsControlFactory creation
		PropertyInfo[] defaultProperyInfoArray = driver.getFactoryPropertyInfo();
		Properties driverProperties = new Properties();

		//        Enumeration<Object> keys = factoryOverrideProperties.keys();
		//        
		//        while (keys.hasMoreElements()){
		//        	String key = (String)keys.nextElement();
		//            if (log.isLoggable(Level.FINE)) {
		//                log.fine("createFactoryProperties: Overide key: " + key + " value:" + factoryOverrideProperties.get(key));
		//            }
		//        }

		// merge configuration properties with default driver properties
		for (PropertyInfo pinfo : defaultProperyInfoArray) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("createFactoryProperties: Property info " + pinfo.name + "," + pinfo.defaultValue);
			}
			if(pinfo.name != null){ 
				String valueToUse;
				String overrideValue = (String) factoryOverrideProperties.get(pinfo.name);
				if( overrideValue != null){
					if (log.isLoggable(Level.FINE)) {
						log.fine("createFactoryProperties: Override with this value: " + overrideValue);
					}

					valueToUse = overrideValue;
				}
				else{
					valueToUse = pinfo.defaultValue;
				}

				if (valueToUse != null)
					driverProperties.setProperty(pinfo.name, valueToUse);  
			}
		}

		return driverProperties;
	}

	public static NetworkConnection createBasicNetworkConnection() throws MsControlException{
		MediaSession mediaSession = s_msFactory.createMediaSession();
		NetworkConnection networkConnection = mediaSession.createNetworkConnection(NetworkConnection.BASIC);

		return networkConnection;
	}

	public static NetworkConnection createCustomNetworkConnection(boolean isWebRTC) throws MsControlException{
		MediaSession mediaSession = s_msFactory.createMediaSession();
		log.fine("Create Media Session");
		NetworkConnection networkConnection = mediaSession.createNetworkConnection(NetworkConnection.BASIC);
		log.fine("createNetwork Connection");
		// networkConnection of webrtc should have configuration of webrtc=yes
		// to direct the dialogic media server generate sdp with DTLS and ICE candidates. 
		// Note: webrtc=yes paramater it's specific vendor configuration of dialogic.
		if(isWebRTC){
			
			Parameters sdpConfiguration = mediaSession.createParameters();
			Map<String,String>  configurationData = new HashMap<String,String>();
			for(Object ncProperty : s_factoryOverrideProperties.keySet()){
				String ncPropertyStr = (String)ncProperty;
				if(ncPropertyStr.startsWith(NETWORKCONNECTION_PROPERTY_PREFIX)){
					String propertyName = ncPropertyStr.split(NETWORKCONNECTION_PROPERTY_PREFIX)[1];
					String propertyValue = (String)s_factoryOverrideProperties.getProperty(ncPropertyStr);

					if (log.isLoggable(Level.FINE)) {
						log.fine("createCustomNetworkConnection: Setting property: name: " + propertyName + " value:" + propertyValue);
					}

					configurationData.put(propertyName, propertyValue);   
				}
			}

			sdpConfiguration.put(SdpPortManager.SIP_HEADERS, configurationData);
			networkConnection.setParameters(sdpConfiguration);	
		} else {
			log.fine("Not WEBRTC");
			Parameters sdpConfiguration = mediaSession.createParameters();
			Map<String,String>  configurationData = new HashMap<String,String>();
//			configurationData.put("RTP-SECURITY", "SDES");
			sdpConfiguration.put(SdpPortManager.SIP_HEADERS, configurationData);
			networkConnection.setParameters(sdpConfiguration);
			log.fine("Done Setting parameters");

		}

		/*else{ // To use with scenario.type property
			/// SIP_TO_WEBRTC, WEBRTC_TO_SIP_RTP, WEBRTC_TO_SIP_SRTP_SDES_NO_ICE, WEBRTC_TO_SIP_SRTP_SDES_with_ICE
			String scenarioType = s_factoryOverrideProperties.getProperty("scenario.type");
			if(scenarioType.equals("WEBRTC_TO_SIP_RTP")){
				// do nothing - no need to set parameters network connections
			}
			else if(scenarioType.equals("WEBRTC_TO_SIP_SRTP_SDES_NO_ICE")|| 
					scenarioType.equals("WEBRTC_TO_SIP_SRTP_SDES_with_ICE")){

				Parameters sdpConfiguration = mediaSession.createParameters();
				Map<String,String>  configurationData = new HashMap<String,String>();
				configurationData.put("RTP-SECURITY", "SDES"); 

				if(scenarioType.equals("WEBRTC_TO_SIP_SRTP_SDES_with_ICE")){
					configurationData.put("webrtc", "yes");   
				}

				sdpConfiguration.put(SdpPortManager.SIP_HEADERS, configurationData);
				networkConnection.setParameters(sdpConfiguration);	
			}
		}*/

		return networkConnection;
	}

	public static void releaseNetworkConnection(NetworkConnection nc){
		try {
			if(nc != null){
				nc.release();
				MediaSession ms = nc.getMediaSession();
				if(ms != null){
					ms.release();
				}
			}	
		} catch (Exception e) {
			log.warning("releaseNetworkConnection failed");
			e.printStackTrace();
		}
	}
}
