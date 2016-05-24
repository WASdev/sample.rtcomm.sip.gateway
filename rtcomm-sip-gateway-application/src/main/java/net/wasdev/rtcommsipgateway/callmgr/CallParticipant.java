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
package net.wasdev.rtcommsipgateway.callmgr;

import javax.media.mscontrol.networkconnection.NetworkConnection;

public class CallParticipant {

	private String m_hostId;
	private boolean m_isWebRTC;
	private String m_sipURI;
	private NetworkConnection m_networkConnection;

	public CallParticipant(String hostId, boolean isWebRTC, String sipURI) {
		m_hostId = hostId;
		m_isWebRTC = isWebRTC;
		m_sipURI = sipURI;
	}

	public NetworkConnection getNetworkConnection(){
		return m_networkConnection;
	}

	public String getHostId(){
		return m_hostId;
	}

	public boolean isWebRTC(){
		return m_isWebRTC;
	}

	public String getSipURI(){
		return m_sipURI;
	}

	public void setNetworkConnection(NetworkConnection nc){
		m_networkConnection = nc;
	}
}
