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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;


public class SipUtils {
	private static Logger log = Logger.getLogger(SipUtils.class.getName());

	public static final String SERLVET_NAME = "WebRTCGatewayServlet";
	public static final String INITIAL_INVITE_ATTRIBUTE ="INITIAL_INVITE";
	public static final String RESPONSE_TO_ACKNOWLEDGE_ATTRIBUTE = "RESPONSE_TO_ACKNOWLEDGE";
	public static final String CONTENT_TYPE_SDP = "application/sdp";
	public static final String PEER_SESSION = "PEER_SESSION";
	public static final String PENDING_REQUEST = "PENDING_REQUEST";
	public static final String INITIAL_INVITE = "INITIAL_INVITE";

	private static final int MAX_CALL_RECORD_ID_RANGE = 1000;

	private static SipFactory 	s_sipFactory;
	private static Properties 	s_configProperties = null;
	//	private static Integer		s_rtcommGatewayPort = null;

	// Must all be stored as lowercased Strings
	private static Set<String> headersToNotCopyGeneral;
	private static Set<String> headersToNotCopy300;
	private static Set<String> headersToNotCopy485;
	private static Set<String> headersToNotCopyRegister;	

	public static void initSipFactory(SipFactory sipFactory, Properties configProperties){
		initHeadersToNotCopy();
		s_sipFactory = sipFactory;
		s_configProperties = configProperties;

		//		try{
		//			InitialContext ic = new InitialContext();
		//			s_rtcommGatewayPort = (Integer) ic.lookup("rtcommGatewayPort");
		//		}
		//		catch(NamingException e){
		//			if (log.isLoggable(Level.WARNING)) {
		//	            log.warning("initSipFactory: no JNDI definition for rtcommGatewayPort.");
		//	            log.warning("initSipFactory: add this JNDI entry to your server.xml: <jndiEntry jndiName=\"rtcommGatewayPort\" value=\"<port number>\"/>");
		//	        }		
		//		}
	}


	public static SipServletRequest createInitialInvite(String fromUri, String toUri) throws ServletParseException{
		SipApplicationSession sas = s_sipFactory.createApplicationSession();
		SipServletRequest iniRequest = s_sipFactory.createRequest(sas, "INVITE", fromUri, toUri);
		return iniRequest;
	}

	public static SipServletRequest createReInvite(SipSession sipSession, String fromUri, String toUri) throws ServletParseException{
		SipServletRequest iniRequest = null;
		iniRequest = sipSession.createRequest("INVITE");
		Address fromAddress = s_sipFactory.createAddress(fromUri);
		Address toAddress = s_sipFactory.createAddress(toUri);
		iniRequest.setAddressHeader("From", fromAddress);
		iniRequest.setAddressHeader("To", toAddress);

		return iniRequest;
	}

	public static void sendOkResponseWithSdpAnswer(SipServletRequest initialRequest, byte[] sdp) throws IllegalArgumentException, IllegalStateException, IOException{
		SipServletResponse successResponse = initialRequest.createResponse(200);
		if (sdp != null) {
			successResponse.setContent(sdp, SipUtils.CONTENT_TYPE_SDP);
		}
		successResponse.send();
		if (log.isLoggable(Level.FINE)) {
			log.fine("sendOkResponseWithSdpAnswer successResponse was sent - " + successResponse);
		}		
	}

	public static void acknowledgeSdpAnswer(SipServletResponse answerCarryingResponse) throws IllegalArgumentException, IOException{
		if (log.isLoggable(Level.FINE)) {
			log.fine("acknowledgeSdpAnswer: top");
		}

		if(answerCarryingResponse == null){
			throw new RuntimeException("No response to acknowledge");
		}

		if(answerCarryingResponse.getStatus()<200 || answerCarryingResponse.getStatus() > 299){
			throw new RuntimeException("This method only support successful responses");
		}

		SipServletRequest ack = answerCarryingResponse.createAck();
		ack.send();

		if (log.isLoggable(Level.FINE)) {
			log.fine("acknowledgeSdpAnswer sent ack: " + ack);
		}	
	}

	public static boolean validateSipInviteRequest(SipServletRequest req) throws IllegalArgumentException, IllegalStateException, IOException, ServletParseException{
		if(!(req.getRequestURI() instanceof SipURI)){
			if (log.isLoggable(Level.WARNING)) {
				log.warning("validateSipInviteRequest req.getRequestURI() not supported: " + req.getRequestURI());
			}
			req.createResponse(406).send();
			return false;
		}
		Address Contact = req.getAddressHeader("Contact");
		if(!(Contact.getURI() instanceof SipURI)){
			if (log.isLoggable(Level.WARNING)) {
				log.warning("validateSipInviteRequest Contact.getURI() not supported: " + Contact.getURI());
			}
			req.createResponse(406).send();
			return false;
		}

		if(!validateSDPContent(req)){
			if (log.isLoggable(Level.WARNING)) {
				log.warning("validateSipInviteRequest bad SDP type: " + req.getContentType());
			}
			req.createResponse(406).send();
			return false;
		}

		return true;
	}

	private static boolean validateSDPContent(SipServletMessage msg) throws IOException{
		if(msg.getRawContent() == null){
			return true;
		}
		if(!msg.getContentType().equals(CONTENT_TYPE_SDP)){
			return false;
		}
		return true;

	}

	public static String generateRandomCallRecordId(String initiatorUser){ 
		Random r = new Random();
		int ri = r.nextInt(MAX_CALL_RECORD_ID_RANGE) + 1;
		return (initiatorUser + ri);
	}

	public static boolean isWebRTCOffer(SipServletRequest req){
		boolean webrtcOffer = false;

		try {
			byte[] sdpBytes;
			sdpBytes = req.getRawContent();
			if (sdpBytes != null){
				String sdpStr = new String(sdpBytes, "UTF-8");
				webrtcOffer = sdpStr.contains("a=fingerprint");
			}
		} catch (IOException e) {
			log.warning("isWebRTCOffer failed - return false as fallback");
			e.printStackTrace();
			webrtcOffer = false;
		}

		return webrtcOffer;

	}

	/** This method is used to verify that initial session is still valid
	 * because UAC can send CANCEL before a response being received 
	 * @param session
	 * @return isValid
	 */
	private static boolean isSessionValid(SipSession session) {
		return session.isValid();
	}

	/**
	 * Creates a response to the linked leg.
	 * 
	 * @param resp the response.
	 * @return a response to the linked leg.
	 */
	public static SipServletResponse createLinkedResponse(SipServletResponse resp) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("createLinkedResponse. resp=" + resp);
		}

		B2buaHelper b2bHelper = resp.getRequest().getB2buaHelper();
		SipSession responseSession = resp.getSession();
		SipSession linkedSession = b2bHelper.getLinkedSession(responseSession);

		if (linkedSession == null || !isSessionValid(linkedSession)) {
			if (log.isLoggable(Level.WARNING)) {
				log.warning("createLinkedResponse. The response can't be processed because the linked session is not valid.");
			}
			return null;
		}

		SipServletRequest linkedRequest = b2bHelper.getLinkedSipServletRequest(resp.getRequest());

		if (linkedRequest == null) {
			if (log.isLoggable(Level.WARNING)) {
				log.warning("createLinkedResponse. Can't send a response, link request is null for " + resp.getStatus() + " on " + resp.getMethod());
			}
			return null;

		} else {
			if (log.isLoggable(Level.FINE)) {
				log.fine("createLinkedResponse. linkedRequest is " + linkedRequest.toString());
			}
		}

		return linkedRequest.createResponse(resp.getStatus(),resp.getReasonPhrase());
	}

	/**
	 * Returns the local hostname
	 * 
	 * @return local hostname
	 */
	public static String getLocalHostname() {
		String localHostName = null;
		try {
			localHostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			if (log.isLoggable(Level.WARNING)) {
				log.warning("ERROR occurred in resolving localhost hostname");
			}
		}
		return localHostName;
	}

	/**
	 * 
	 * @param sdp
	 * @param oldText
	 * @param newText
	 * @return
	 */
	public static byte[] replaceSDPcontent(byte[] sdp, String oldText, String newText){

		try {
			String sdpStr = new String(sdp, "UTF-8");
			sdpStr = sdpStr.replace(oldText,newText);
			sdp = sdpStr.getBytes();
		} catch (Exception e) {
			// TODO: handle exception
		}

		return sdp;
	} 


	/**
	 * 
	 * @return
	 */
	public static String getRemoteProxyServer() {
		String remoteTrunkingServer = s_configProperties.getProperty("remote.proxy-reg.server");
		if (log.isLoggable(Level.FINE)) {
			log.fine("getRemoteProxyServer: remote.proxy-reg.server " + remoteTrunkingServer);
		}
		return remoteTrunkingServer;
	}

	/**
	 * 
	 * @return
	 */
	public static int getRemoteProxyPort() {
		String remoteTrunkingPort = s_configProperties.getProperty("remote.proxy-reg.port");
		if (log.isLoggable(Level.FINE)) {
			log.fine("getRemoteProxyPort: remote.proxy-reg.port " + remoteTrunkingPort);
		}
		return Integer.valueOf(remoteTrunkingPort);
	}

	/**
	 * 
	 * @return
	 */
	public static boolean isLocalProxyRegistrar(){
		String localProxyRegistrar = s_configProperties.getProperty("local.proxy-registrar");
		if (log.isLoggable(Level.FINE)) {
			log.fine("isLocalProxyRegistrar: localProxyRegistrar " + localProxyRegistrar);
		}
		return Boolean.valueOf(localProxyRegistrar);
	}

	/**
	 * @return
	 */
	public static Integer getRtcommGatewayPort(){
		String rtcommGatewayPort = s_configProperties.getProperty("rtcomm.gateway.port");
		if (log.isLoggable(Level.FINE)) {
			log.fine("getRemotePort: rtcomm.gateway.port " + rtcommGatewayPort);
		}
		return Integer.valueOf(rtcommGatewayPort);
	}


	public static void initHeadersToNotCopy() {
		// What headers shouldn't we copy from message to message?
		Set<String> general = new HashSet<String>();
		general.add("i");
		general.add("Call-ID".toLowerCase());
		general.add("m");
		general.add("Contact".toLowerCase());
		general.add("c");
		general.add("Content-Type".toLowerCase());
		/* No Short */general.add("CSeq".toLowerCase());
		/* No Short */general.add("RSeq".toLowerCase());
		/* No Short */general.add("RAck".toLowerCase());
		general.add("f");
		general.add("From".toLowerCase());
		/* No Short */general.add("Record-Route".toLowerCase());
		/* No Short */general.add("Route".toLowerCase());
		general.add("t");
		general.add("To".toLowerCase());
		general.add("v");
		general.add("Via".toLowerCase());
		headersToNotCopyGeneral = Collections.unmodifiableSet(new HashSet<String>(general));

		// Relax restrictions somewhat for a REGISTER request or 3xx, 485 responses
		Set<String> register = new HashSet<String>(headersToNotCopyGeneral);
		register.remove("m");
		register.remove("Contact");
		headersToNotCopyRegister = Collections.unmodifiableSet(new HashSet<String>(register));
		headersToNotCopy300 = headersToNotCopyRegister;
		headersToNotCopy485 = headersToNotCopyRegister;
	}

	// Throws IAEs if we can't remove/add the header
	public static void copyHeaders(SipServletMessage source, SipServletMessage destination) {
		// Aggregates all of the header manipulation problems into one exception, speeding
		// up debugging.  Done lazily, to avoid overhead when there are no problems to report.
		StringBuffer msgs = null;

		for (Iterator headerIter = source.getHeaderNames(); headerIter.hasNext();) {
			String header = (String) headerIter.next();
			if (shouldCopyHeader(source, header)) {
				try {
					destination.removeHeader(header);
				} catch (IllegalArgumentException iae) {
					msgs = (msgs == null) ? new StringBuffer() : msgs.append("\n"); // Lazy
					msgs.append("Unable to remove ");
					msgs.append(header);
					msgs.append(" from destination-- ");
					msgs.append(iae.getMessage());
				}

				for (Iterator valueIter = source.getHeaders(header); valueIter.hasNext();) {
					String value = (String) valueIter.next();
					try {
						destination.addHeader(header, value);
					} catch (IllegalArgumentException iae) {
						msgs = (msgs == null) ? new StringBuffer() : msgs.append("\n"); // Lazy 
						msgs.append("Unable to add ");
						msgs.append(header);
						msgs.append(": ");
						msgs.append(value);
						msgs.append(" to destination-- ");
						msgs.append(iae.getMessage());
					}
				}
			}

			if (msgs != null && msgs.length() > 0) { // Lazy
				throw new IllegalStateException("Logic error(s) in copyHeaders--\n" + msgs.toString());
			}
		}
	}

	public static void copyContent(SipServletMessage source, SipServletMessage destination)
			throws UnsupportedEncodingException, IOException {
		if (source.getContent() == null || source.getContentType() == null) {
			// NOP, nothing to copy
		} else {
			destination.setContent(source.getRawContent(), source.getContentType());
		}
	}

	// Assumes destination message is the same exact type
	private static boolean shouldCopyHeader(SipServletMessage source, String header) {
		if (source instanceof SipServletRequest) {
			SipServletRequest sourceRequest = (SipServletRequest) source;
			if (sourceRequest.getMethod().equalsIgnoreCase("REGISTER")) {
				return !headersToNotCopyRegister.contains(header.toLowerCase());
			} else {
				return !headersToNotCopyGeneral.contains(header.toLowerCase());
			}
		} else if (source instanceof SipServletResponse) {
			SipServletResponse sourceResponse = (SipServletResponse) source;
			if ((sourceResponse.getStatus() / 100) == 3) {
				return !headersToNotCopy300.contains(header.toLowerCase());
			} else if (sourceResponse.getStatus() == SipServletResponse.SC_AMBIGUOUS) {
				return !headersToNotCopy485.contains(header.toLowerCase());
			} else {
				return !headersToNotCopyGeneral.contains(header.toLowerCase());
			}
		} else {
			throw new IllegalArgumentException("source neither SipServletRequest nor SipServletResponse");
		}
	}    
}
