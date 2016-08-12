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

import java.util.logging.Logger;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import net.wasdev.rtcommsipgateway.utils.JSR309Utils;
import net.wasdev.rtcommsipgateway.utils.SipUtils;

public class CallRecord {
	private String m_callRecordId;
	private CallParticipant m_caller;
	private CallParticipant m_callee;
	private SipServletRequest m_initialInvite;
	private static Logger log = Logger.getLogger(CallRecord.class.getName());

	public CallRecord(SipServletRequest callerInitialInviteRequest) {
		String callerUser = ((SipURI)callerInitialInviteRequest.getFrom().getURI()).getUser();
		m_callRecordId = SipUtils.generateRandomCallRecordId(callerUser);
		m_caller = extractCaller(callerInitialInviteRequest);
		m_callee = extractCallee(callerInitialInviteRequest);
		m_initialInvite = callerInitialInviteRequest;
	}

	private CallParticipant extractCaller(SipServletRequest initialInviteRequest){
		SipURI hostIdSipURI  = ((SipURI)initialInviteRequest.getFrom().getURI());
		String hostId = hostIdSipURI.getUser();
		boolean isWebRTC = SipUtils.isWebRTCOffer(initialInviteRequest);
		CallParticipant caller = new CallParticipant(hostId, isWebRTC, hostIdSipURI.toString());

		return caller;
	}

	private CallParticipant extractCallee(SipServletRequest initialInviteRequest){
		SipURI hostIdSipURI = ((SipURI)initialInviteRequest.getRequestURI());
		log.fine(hostIdSipURI.toString());
		String hostId = hostIdSipURI.getUser();
		log.fine(hostId);
		// assume that if the offer isn't webrtc, so the callee is webrtc
		// FIX: we really need to introspect the registration to determine this properly.
		//		This is a hack for now.
		boolean isWebRTC = !SipUtils.isWebRTCOffer(initialInviteRequest);

		SipURI sipURI = ((SipURI)initialInviteRequest.getTo().getURI());
		String sipURIString = sipURI.toString();

//		if (hostId == null) {
//			// Sametime calls to WebRTC
//			hostId = sipURI.getUser();
//			log.fine(hostId);
//			SipURI newSipURI = (SipURI)sipURI.clone();
//			SipURI registeredURI = CallsManager.getInstance().getURI(hostId);
//			log.fine(registeredURI.toString());
//
//			newSipURI.setHost(registeredURI.getHost());
//			sipURIString = newSipURI.toString().split(";")[0];
//		}

		CallParticipant callee = new CallParticipant(hostId, isWebRTC, sipURIString);

		return callee;
	}

	public SipServletRequest getInitialInviteRequest(){
		return m_initialInvite;
	}

	public SipSession getCallerSipSession(){
		return m_initialInvite.getSession();
	}

	public String getCallRecordId(){
		return m_callRecordId;
	}

	public CallParticipant getCaller(){
		return m_caller;
	}

	public CallParticipant getCallee(){
		return m_callee;
	}

	public void addCaller(NetworkConnection nc) throws MsControlException{
		m_caller.setNetworkConnection(nc);
	}

	public void addCallee(NetworkConnection nc){
		m_callee.setNetworkConnection(nc);
	}

	public void releaseNetworkConnections(){
		NetworkConnection callerNc = m_caller.getNetworkConnection();
		NetworkConnection calleeNc = m_callee.getNetworkConnection();

		JSR309Utils.releaseNetworkConnection(callerNc);
		JSR309Utils.releaseNetworkConnection(calleeNc);

		// This does not work with Dialogic XMS
		//JSR309Utils.unjoinNetworkConnections(callerNc, calleeNc);

		/// prevent redundant release
		callerNc = null;
		calleeNc = null;
	}
}
