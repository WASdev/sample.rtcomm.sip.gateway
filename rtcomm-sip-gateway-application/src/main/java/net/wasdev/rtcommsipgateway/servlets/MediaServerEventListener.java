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
package net.wasdev.rtcommsipgateway.servlets;

import java.io.IOException;
import java.io.Serializable;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import javax.servlet.ServletException;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.wasdev.rtcommsipgateway.callmgr.CallRecord;
import net.wasdev.rtcommsipgateway.callmgr.CallsManager;
import net.wasdev.rtcommsipgateway.utils.JSR309Utils;
import net.wasdev.rtcommsipgateway.utils.SipUtils;

public class MediaServerEventListener implements MediaEventListener<SdpPortManagerEvent>, Serializable	{

	private static final long serialVersionUID = 5742674704860593132L;
	private static Logger log = Logger.getLogger(MediaServerEventListener.class.getName());

	private CallRecord m_callRecord;

	public MediaServerEventListener(CallRecord callRecord) {
		m_callRecord = callRecord;
	}

	@Override
	public void onEvent(SdpPortManagerEvent event) {	
		SdpPortManager sdpPortMgr = (SdpPortManager) event.getSource();
		NetworkConnection nc = (NetworkConnection)sdpPortMgr.getContainer();
		byte[] sdp = event.getMediaServerSdp();

		if (log.isLoggable(Level.FINE)) {
			log.fine("onEvent from Media Server: event.getEventType() = " + event.getEventType());
		}

		EventType eventType = event.getEventType();
		try {
			if ( eventType.equals(SdpPortManagerEvent.OFFER_GENERATED) ||
					eventType.equals(SdpPortManagerEvent.UNSOLICITED_OFFER_GENERATED)) {
				offerGeneratedByMS(nc, sdp);
			}
			else if (eventType.equals(SdpPortManagerEvent.ANSWER_PROCESSED)){       
				answerProcessedByMS(nc);
			}
			else if (eventType.equals(SdpPortManagerEvent.ANSWER_GENERATED)){
				answerGeneratedByMS(nc, sdp);
			}
			else if (eventType.equals(SdpPortManagerEvent.NETWORK_STREAM_FAILURE)){
				if (log.isLoggable(Level.WARNING)) {
					log.warning("onEvent: NETWORK_STREAM_FAILURE");
				}
				cleanupCall();
			}
			else if (eventType.equals(SdpPortManagerEvent.RESOURCE_UNAVAILABLE)){
				if (log.isLoggable(Level.WARNING)) {
					log.warning("onEvent: RESOURCE_UNAVAILABLE");
				}
				cleanupCall();
			}
			else if (eventType.equals(SdpPortManagerEvent.SDP_NOT_ACCEPTABLE)){
				if (log.isLoggable(Level.WARNING)) {
					log.warning("onEvent: SDP_NOT_ACCEPTABLE");
				}
				cleanupCall();
			}
		} catch (Exception e ) {
			log.warning("onEvent throw exception");
			e.printStackTrace();
		}
	}


	/**
	 * @throws MsControlException
	 */
	public void processSdpOfferFromCaller() throws MsControlException{
		if (log.isLoggable(Level.FINE)) {
			log.fine("processSdpOfferFromCaller");
		}

		NetworkConnection nc = JSR309Utils.createBasicNetworkConnection();
		m_callRecord.getInitialInviteRequest().getSession().setAttribute(JSR309Utils.NETWORK_CONNECTION_ATTRIBUTE, nc);

		nc.getSdpPortManager().addListener(this);

		/**
		 * WARNING: Added to fix a Sametime Polycom issue. May not work for other endpoints////////////////
		 */
		try{
			byte[] sdpOffer = m_callRecord.getInitialInviteRequest().getRawContent();
			sdpOffer = SipUtils.replaceSDPcontent(sdpOffer, "UDP/TLS/RTP/SAVPF", "RTP/SAVPF");

			nc.getSdpPortManager().processSdpOffer(sdpOffer);
			// the callback method for this request will handled in PHASE 1 in MediaServerEventListener.
		}
		catch(IOException e){
			if (log.isLoggable(Level.WARNING)) {
				log.warning("processSdpOfferFromCaller: exception: " + e);
			}
		}
	}	

	// PHASE 1: 
	// ---------
	// The media server generated sdp offer to the callee.
	// The sdp offer is sent to the callee.
	// 
	private void offerGeneratedByMS(NetworkConnection nc, byte[] sdp) throws IllegalArgumentException, IOException, IllegalStateException, ServletException{
		if (log.isLoggable(Level.FINE)) {
			log.fine("offerReceivedFromMS: Phase 1: Offer ready to send to callee");
		}

		sendSdpOfferToCallee(sdp, nc);
	}

	// PHASE 2: 
	// ---------
	// The media server acknowledged the sdp answer from the callee.
	// The callee will be connected to the caller to start the call.
	// 
	private void answerProcessedByMS(NetworkConnection nc) throws IllegalArgumentException, ServletParseException, IOException, MsControlException{
		if (log.isLoggable(Level.FINE)) {
			log.fine("answerProcessedByMS: Phase 2: SDP answer from callee has been processed. Sending ACK to callee");
		}

		SipServletResponse answerCarryingResponse = (SipServletResponse)nc.getMediaSession().getAttribute(SipUtils.RESPONSE_TO_ACKNOWLEDGE_ATTRIBUTE);
		// acknowledge media server accpet for the the callee sdp answer
		SipUtils.acknowledgeSdpAnswer(answerCarryingResponse);
		m_callRecord.addCallee(nc);

		////////////////////////////////////////////////////////////////////////////
		//	Test to playback media prompt
		//		MediaConfig config = JSR309Utils.getMSFactory().getMediaConfig(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
		//		URI prompt = URI.create("file:////var/lib/xms/media/en-US/verification/video_clip_nascar.wav");
		//
		//		Parameters parameters = JSR309Utils.getMSFactory().createParameters();
		//		parameters.put(Player.REPEAT_COUNT, 2);
		//		parameters.put(Player.INTERVAL, 2000);
		//		parameters.put(Player.MAX_DURATION, 10000);
		//		try
		//		{
		//			MediaGroup mediaGroup = nc.getMediaSession().createMediaGroup(config, parameters);
		//			nc.join(Direction.DUPLEX, mediaGroup);
		//			mediaGroup.getPlayer().addListener(new PlayerEventListener());
		//			mediaGroup.getPlayer().play(prompt, RTC.NO_RTC, parameters);
		//		}
		//		catch (MsControlException e)
		//		{
		//			e.printStackTrace();
		//		}
		////////////////////////////////////////////////////////////////////////////

		//	We will reach Phase 3 after the offer is processed.
		//  FIX: consider doing this in parallel with generating the offer for the callee to speed up processing.
		processSdpOfferFromCaller();
	}

	// PHASE 3: 
	// ---------
	// The media server processed the caller's sdp offer and its response with
	// sdp answer is sent to the caller.
	// 
	private void answerGeneratedByMS(NetworkConnection nc, byte[] sdp) throws IllegalArgumentException, IOException, MsControlException{
		if (log.isLoggable(Level.FINE)) {
			log.fine("answerGeneratedByMS: PHASE 3: Caller SDP processed, sending response");
		}

		m_callRecord.addCaller(nc);

		//	Now join the two network connections
		NetworkConnection calleeNc = m_callRecord.getCallee().getNetworkConnection();
		NetworkConnection callerNc = nc;

		// callee join the caller for the call.
		if( calleeNc != null){
			if (log.isLoggable(Level.FINE)) {
				log.fine("answerGeneratedByMS: join the two network connections");
			}
			callerNc.join(Direction.DUPLEX, calleeNc);
		}

		//	Send the answer to the caller.
		SipServletRequest initialRequest = m_callRecord.getInitialInviteRequest();
		SipUtils.sendOkResponseWithSdpAnswer(initialRequest, sdp );
	}

	/**
	 * Sends sdp offer that was generated by the media server to the callee.
	 * @param sdp sdp offer 
	 * @param nc
	 */
	public void sendSdpOfferToCallee(byte[] sdp, NetworkConnection calleeNC){
		if (log.isLoggable(Level.FINE)) {
			log.fine("sendOfferToCallee: TOP");
		}

		String fromUri = m_callRecord.getCaller().getSipURI();
		String toUri = m_callRecord.getCallee().getSipURI();

		try{
			//	This removes the + from any user parts of the URI
			if( m_callRecord.getCallee().isWebRTC()){
				toUri = fixToUriForWebrtcEndpoint(toUri);
				fromUri = fixFromUriForWebrtcEndpoint(fromUri);
			}

			SipSession sipSession = (SipSession)calleeNC.getMediaSession().getAttribute(JSR309Utils.SIP_SESSION_ATTRIBUTE);
			SipServletRequest inviteRequest = createOfferringInviteToCallee(sipSession, fromUri, toUri);

			//	Using TCP to avoid max MTU issues with UDP.
			((SipURI)inviteRequest.getRequestURI()).setTransportParam("tcp");

			// Route the request to Rtcomm gateway URI
			// in order the INVITE would be received and translated by the sipUa.
			// The sipUa host could be different than the WebRTC client host.
			if( m_callRecord.getCallee().isWebRTC()){

				SipURI sipUaURI = (SipURI) inviteRequest.getRequestURI().clone();

				String localHostname = SipUtils.getLocalHostname();
				sipUaURI.setHost(localHostname);
				sipUaURI.setPort(SipUtils.getRtcommGatewayPort());

				inviteRequest.pushRoute(sipUaURI);

				//	Add a custom header to inform the app router that this request is destined for the Rtcomm GW.
				inviteRequest.setHeader("rtcommGWBound", "true");

				if (log.isLoggable(Level.FINE)) {
					log.fine("sendOfferToCallee: Callee is an Rtcomm client. Push this route to Rtcomm GW: " + sipUaURI);
				}				
			}

			SipServletRequest initialInvite = (SipServletRequest)calleeNC.getMediaSession().getAttribute(SipUtils.INITIAL_INVITE_ATTRIBUTE);

			//	This is used to send the response in case of an error.
			inviteRequest.setAttribute(SipUtils.PENDING_REQUEST, initialInvite);

			inviteRequest.getSession().setAttribute(JSR309Utils.NETWORK_CONNECTION_ATTRIBUTE, calleeNC);
			inviteRequest.getSession().setAttribute(CallsManager.CALL_RECORD_ID_ATTRIBUTE, m_callRecord.getCallRecordId());

			//	Save the peer sessions so that we can get back to them on the BYE and other inDialog messages.
			m_callRecord.getCallerSipSession().setAttribute(SipUtils.PEER_SESSION, inviteRequest.getSession());
			inviteRequest.getSession().setAttribute(SipUtils.PEER_SESSION, m_callRecord.getCallerSipSession());

			///////////////////////
			String sdpStr = new String(sdp, "UTF-8");
			sdpStr = sdpStr.replace("h264","H264");
//			sdpStr = sdpStr.replace("SAVP","SAVPF");

			sdp = sdpStr.getBytes();
			///////////////////////
			inviteRequest.setContent(sdp, SipUtils.CONTENT_TYPE_SDP);
			calleeNC.getMediaSession().setAttribute(JSR309Utils.SIP_SESSION_ATTRIBUTE, inviteRequest.getSession());

			//	This informs that application router that this is a continuation of a previous request.
			//	This is really not needed unless the CAR is enabled but leaving it for now.
			inviteRequest.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE, initialInvite);


			if (log.isLoggable(Level.FINE)) {
				log.fine("sendOfferToCallee: Sending invite request to callee: " + inviteRequest);
			}	

			inviteRequest.send();
		}
		catch(Exception e){
			log.warning("sendSdpOfferToCallee failed to send sdp offer to callee");
			e.printStackTrace();
		}
	}

	public SipServletRequest createOfferringInviteToCallee(SipSession sipSession, String fromUri, String toUri){

		if (log.isLoggable(Level.FINE)) {
			log.fine("createOfferringInviteToCallee - fromUri = " + fromUri + 
					" toUri = " + toUri + " sipSession = " + sipSession);
		}

		SipServletRequest inviteRequest = null;
		try {
			if(sipSession == null){
				inviteRequest = SipUtils.createInitialInvite(fromUri, toUri);
				inviteRequest.getSession().setHandler(SipUtils.SERLVET_NAME);
				inviteRequest.getSession().setAttribute(SipUtils.INITIAL_INVITE, inviteRequest);
			}
			else{
				inviteRequest = SipUtils.createReInvite(sipSession, fromUri, toUri);
			}
		} catch (Exception e) {
			log.warning("createOfferringInviteToCallee failed to create INVITE to the callee");
			e.printStackTrace();
		}

		return inviteRequest;
	}

	/**
	 * Adds the the webrtcgw port to the sip the to SIP URI.
	 * 
	 * @param currentToUri current to URI
	 * @return fixed to SIP URI to webrtc endpoint,
	 * @throws NamingException
	 */
	private String fixToUriForWebrtcEndpoint(String currentToUri){
		//String sipToUri = currentToUri + ":" + SipUtils.getRtcommGatewayPort();

		//	Here we need to remove any + signs added for international calls
		//String[] currentToUriList = currentToUri.split("\\+");
		//String sipToUri = currentToUriList[0];
		String sipToUri = currentToUri.replace("+", "") + ":" + SipUtils.getRtcommGatewayPort();

		if (log.isLoggable(Level.FINE)) {
			log.fine("fixToUriForWebrtcEndpoint: sipToUri = " + sipToUri);
		}

		return sipToUri;
	}

	/**
	 * Adds the the webrtcgw port to the sip the to SIP URI.
	 * 
	 * @param currentToUri current to URI
	 * @return fixed to SIP URI to webrtc endpoint,
	 * @throws NamingException
	 */
	private String fixFromUriForWebrtcEndpoint(String currentFromUri){

		String sipFromoUri = currentFromUri.replace("+", "");

		if (log.isLoggable(Level.FINE)) {
			log.fine("fixFromUriForWebrtcEndpoint: sipFromoUri = " + sipFromoUri);
		}

		return sipFromoUri;
	}	

	/**
	 * This takes care of closing down the call.
	 */
	private void cleanupCall(){
		//FIX: Add the code to shutdown the call.
	}

}
