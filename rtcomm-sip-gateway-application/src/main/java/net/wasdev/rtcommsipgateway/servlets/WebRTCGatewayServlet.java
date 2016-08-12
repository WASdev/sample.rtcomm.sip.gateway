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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.CodecPolicy;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;
import javax.servlet.sip.SipURI;

import net.wasdev.rtcommsipgateway.callmgr.CallRecord;
import net.wasdev.rtcommsipgateway.callmgr.CallsManager;
import net.wasdev.rtcommsipgateway.driver.JSR309Driver;
import net.wasdev.rtcommsipgateway.driver.MSConnectorFactory;
import net.wasdev.rtcommsipgateway.utils.JNDIHelper;
import net.wasdev.rtcommsipgateway.utils.JSR309Utils;
import net.wasdev.rtcommsipgateway.utils.SipUtils;

@javax.servlet.sip.annotation.SipServlet(description = "The main servlet for the gateway application", name = "WebRTCGatewayServlet", loadOnStartup=1)
public class WebRTCGatewayServlet extends SipServlet{

	private static Logger log = Logger.getLogger(WebRTCGatewayServlet.class.getName());

	private static final long serialVersionUID = 1538727590067323890L;

	/**
	 * Path for the configuration properties file that the application will use.
	 */
	private static final String JSR309_DRIVER_PROPERTIES_FILE_PATH = "/WEB-INF/JSR309Driver.properties";
	private static final String SIP_PROPERTIES_FILE_PATH = "/WEB-INF/sip.properties";
	
	@Resource
	private SipFactory s_factory;

	private static volatile JSR309Driver s_driverSingleton = null;

	private static CallsManager s_callsManager;

	/**
	 * Thread safe lock for driver loading.
	 */
	private static Object s_lock = new Object();

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		if (log.isLoggable(Level.FINE)) {
			log.fine("init. initing SIP Utils");
		}
		super.init();
		SipUtils.initSipFactory(s_factory, loadConfigurationProperties(SIP_PROPERTIES_FILE_PATH));

		//	Print the outbound interface list
		if (log.isLoggable(Level.FINE)) {
			log.fine("init. OutboundInterfaceList:");
			List<SipURI> sipUriList = (List<SipURI>)getServletContext().getAttribute(SipServlet.OUTBOUND_INTERFACES);

			Iterator<SipURI> it = sipUriList.iterator();
			while (it.hasNext()){
				log.fine("    OutboundInterface:" + it.next());
			}
		}
	}

	@Override
	protected void doRegister(SipServletRequest req) throws ServletException,
	IOException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("doRegister. req="+req);
		}
		initJSR309Driver();

		//	First check to see if this is a local or remote proxy registrar.
		//	Note that this code is treating a register like its a B2BUA.
		if (SipUtils.isLocalProxyRegistrar() == false){
			//	Here we forward the request to an external proxy/registrar
			SipServletRequest registerRequest = createRegisterRequest(req, true);

			if (log.isLoggable(Level.FINE)) {
				log.fine("doRegister. forwarding register request ="+registerRequest);
			}

			registerRequest.getSession().setHandler(SipUtils.SERLVET_NAME);
			registerRequest.send();
		}
		else{
			//	Here we handle the registration locally
			//RegistrationManager.handleRegistration(req);
			req.createResponse(200).send();
		}
	}

	/**
	 * Creates new initial requests (based on the received request).
	 *
	 * @param req the received request from UAC.
	 * @throws ServletException
	 * @throws IOException
	 */
	public static SipServletRequest createRegisterRequest(SipServletRequest req, boolean linked) throws ServletException,
	IOException {

		//	Note that getting the proxy/registrar from the JSR 309 properties file is a hack
		//	that needs to be cleaned up at some point.
		String remoteServer = SipUtils.getRemoteProxyServer();
		int remotePort = SipUtils.getRemoteProxyPort();
		log.fine("Create Register Request");
		Map<String, List<String>> headerMap = new HashMap<String, List<String>>();

		SipURI fromURI = (SipURI)(req.getFrom().getURI());
		String from = "sip:" + fromURI.getUser() + "@" + remoteServer;
		String to = from;
		headerMap.put("From", Collections.singletonList(from));
		headerMap.put("To", Collections.singletonList(to));

		String contact = req.getRequestURI().toString().split(";")[0];
		headerMap.put("Contact", Collections.singletonList(contact));

		B2buaHelper b2bHelper = req.getB2buaHelper();
		SipServletRequest registerRequest = b2bHelper.createRequest(req, linked, headerMap);

		SipURI registerURI = (SipURI) registerRequest.getRequestURI();
		registerURI.setHost(remoteServer);
		registerURI.setPort(remotePort);
		registerURI.setTransportParam("tcp");
		registerRequest.setRequestURI(registerURI);

		CallsManager.getInstance().addRegistration(fromURI.getUser(), fromURI);

		return registerRequest;
	}

	/**
	 * Setup a call record and starting the call scenario to the invited endpoint.
	 */
	protected void doInvite(SipServletRequest req) throws ServletException,
	IOException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("doInvite. req="+req);
		}

		initJSR309Driver();
		
		if (SipUtils.validateSipInviteRequest(req) == false){
			return;
		}
		else if (shouldProcessInvite(req)) {

			req.getSession().setHandler(SipUtils.SERLVET_NAME);

			try {
				
				/**
				 * First create the call record to keep up with the latest state for this call.
				 */
				CallRecord record = new CallRecord(req);
				s_callsManager.addCallRecord(record);
				req.getSession().setAttribute(CallsManager.CALL_RECORD_ID_ATTRIBUTE, record.getCallRecordId());

				boolean isWebRTC = record.getCallee().isWebRTC();

				/*
				 *  Here we generate the offer to send to the callee.
				 */
				if (log.isLoggable(Level.FINE)) {
					log.fine("doInvite: create new media server network connection and SDP for callee");
				}
				NetworkConnection calleeNC = JSR309Utils.createCustomNetworkConnection(isWebRTC);
				calleeNC.getMediaSession().setAttribute(SipUtils.INITIAL_INVITE_ATTRIBUTE, req);

				// use the same listener because both caller and callee have the same call record id.
				calleeNC.getSdpPortManager().addListener(new MediaServerEventListener(record));

				//	SIP Trunking supports audio only. This is the code that removes video from the SDP.
				//	Note that this is critical to get audio to playback in Chrome
				String[] mediaTypeCaps = new String[2];
				mediaTypeCaps[0] = "audio";
				mediaTypeCaps[1] = "video";
				CodecPolicy codecPolicy = calleeNC.getSdpPortManager().getCodecPolicy();
				codecPolicy.setMediaTypeCapabilities(mediaTypeCaps);
				calleeNC.getSdpPortManager().setCodecPolicy(codecPolicy);

				// the callback method for this request will handled in PHASE 1 in ConnectorSDPPortEventListener.
				calleeNC.getSdpPortManager().generateSdpOffer();

			} catch (MsControlException e) {
				log.warning("doInvite -  processSdpOffer failed.");
				e.printStackTrace();
			}
		}
		else{
			/**
			 * Note that a DAR should be set up so that every originating INVITE flows through this application.
			 * This else statements takes care of forwarding outbound INVITES that should not be processed by
			 * this applic
			 */
			if (log.isLoggable(Level.WARNING)) {
				log.warning("doInvite: ERROR: received an INVITE that should not be processed!!!");
			}
			//
			//			ArrayList<URI> destinations = new ArrayList<URI>();
			//			destinations.add(req.getRequestURI());
			//
			//			Proxy proxy = req.getProxy();
			//			proxy.setSupervised(false);
			//			proxy.setRecordRoute(false);
			//			proxy.createProxyBranches(destinations);
			//			proxy.startProxy();
		}
	}

	/**
	 * Handles response from invited endpoint and send the answer to the media server.
	 * After media server will process the answer, the call will start.
	 */
	@Override
	protected void doSuccessResponse(SipServletResponse resp) throws ServletException,
	IOException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("doSuccessResponse: response="+resp);
		}

		if (resp.getMethod().equals("INVITE")){
			//  Sends received sdp answer to the media server
			SipSession sipSession = resp.getSession();
			byte[] remoteSdp = resp.getRawContent();

			NetworkConnection calleeNC = (NetworkConnection)sipSession.getAttribute(JSR309Utils.NETWORK_CONNECTION_ATTRIBUTE);
			//    		calleeNC.getMediaSession().setAttribute(SipUtils.INITIAL_INVITE_ATTRIBUTE, resp.getAttribute(SipUtils.PENDING_REQUEST));
			calleeNC.getMediaSession().setAttribute(SipUtils.RESPONSE_TO_ACKNOWLEDGE_ATTRIBUTE, resp);
			try {
				if (remoteSdp != null) {
					calleeNC.getSdpPortManager().processSdpAnswer(remoteSdp);
					// the callback method for this request will handled in PHASE 3 in ConnectorSDPPortEventListener.
				}
				else{
					//	FIX: we need to send an immediate success response with no SDP here.
					//	In this case the media could be negotiated in a reinvite.
				}
			} catch (MsControlException e) {
				log.warning("doSuccessResponse: processSdpAnswer failed.");
				e.printStackTrace();
			}

		}
		else if (resp.getMethod().equals("REGISTER")) {
			SipServletResponse newResp =  SipUtils.createLinkedResponse(resp);
			newResp.send();
		}
		else{
			SipServletRequest origRequest = (SipServletRequest)resp.getRequest().getAttribute(SipUtils.PENDING_REQUEST);

			if (origRequest != null){
				origRequest.createResponse(200);
				origRequest.send();
			}
		}
	}

	@Override
	protected void doProvisionalResponse(SipServletResponse resp) throws ServletException, IOException {
		SipServletRequest origRequest = (SipServletRequest)resp.getRequest().getAttribute(SipUtils.PENDING_REQUEST);

		if (log.isLoggable(Level.FINE)) {
			log.fine("doProvisionalResponse: response received="+resp);
		}

		if (origRequest != null){
			SipServletResponse newResponse = origRequest.createResponse(resp.getStatus());

			SipUtils.copyHeaders(resp, newResponse);
			SipUtils.copyContent(resp, newResponse);

			if (log.isLoggable(Level.FINE)) {
				log.fine("doProvisionalResponse: response forwarded="+newResponse);
			}
			newResponse.send();
		}
	}

	@Override
	protected void doErrorResponse(SipServletResponse resp)
			throws ServletException, IOException {

		if (log.isLoggable(Level.WARNING)) {
			log.warning("doErrorResponse: "+resp.getStatus() + " "+resp.getReasonPhrase());
		}

		if (resp.getMethod().equals("INVITE")){
			String callRecordID = (String)resp.getSession().getAttribute(CallsManager.CALL_RECORD_ID_ATTRIBUTE);
			s_callsManager.cleanMediaServer(callRecordID);
		}

		SipServletRequest origRequest = (SipServletRequest)resp.getRequest().getAttribute(SipUtils.PENDING_REQUEST);

		if (origRequest != null){
			SipServletResponse newResponse = origRequest.createResponse(resp.getStatus());

			SipUtils.copyHeaders(resp, newResponse);
			SipUtils.copyContent(resp, newResponse);

			if (log.isLoggable(Level.FINE)) {
				log.fine("doErrorResponse: response forwarded="+newResponse);
			}

			newResponse.send();
		}
	}

	@Override
	protected void doInfo(SipServletRequest req) throws ServletException, IOException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("doInfo. request ="+req);
			//log.fine("doInfo. sending 200 OK with no processing");
		}

		//	FIX: Need to terminate trickle ICE request here when they are supported by the media server.
		//req.createResponse(200).send();

		SipSession peerSipSession = (SipSession)req.getSession().getAttribute(SipUtils.PEER_SESSION);

		if (peerSipSession != null){
			SipServletRequest newRequest = peerSipSession.createRequest(req.getMethod());
			SipUtils.copyHeaders(req, newRequest);
			SipUtils.copyContent(req, newRequest);
			newRequest.setAttribute(SipUtils.PENDING_REQUEST, req);

			if (log.isLoggable(Level.FINE)) {
				log.fine("doInfo: request forwarded="+newRequest);
			}
			newRequest.send();
		}
	}

	/**
	 * Terminate call request was received.
	 * Clean media server resources and connections for the call record.
	 */
	@Override
	protected void doBye(SipServletRequest req) throws ServletException,
	IOException {
		if (log.isLoggable(Level.FINE)) {
			log.fine("doBye. received req=" + req);
		}

		//	Now forward the BYE
		SipSession inSession = req.getSession();
		SipSession outSession = (SipSession) inSession.getAttribute(SipUtils.PEER_SESSION);

		if (outSession != null && outSession.isValid() && outSession.getState() != State.TERMINATED){
			SipServletRequest outReq = outSession.createRequest("BYE");

			if (log.isLoggable(Level.FINE)) {
				log.fine("doBye. sending req to other leg=" + outReq);
			}

			outReq.send();
		}
		else{
			if (log.isLoggable(Level.FINE)) {
				log.fine("doBye. Could not forward BYE request because leg is invalid");
			}
		}

		req.createResponse(200).send();
		String callRecordID = (String) req.getSession().getAttribute(CallsManager.CALL_RECORD_ID_ATTRIBUTE);

		// releases media server resources and connections for the call record id.
		if (log.isLoggable(Level.FINE)) {
			log.fine("doBye. cleanMediaServer");
		}

		s_callsManager.cleanMediaServer(callRecordID);

		if (log.isLoggable(Level.FINE)) {
			log.fine("doBye. exiting");
		}
	}

	@Override
	protected void doCancel(SipServletRequest req) throws ServletException, IOException {

		SipSession peerSipSession = (SipSession)req.getSession().getAttribute(SipUtils.PEER_SESSION);

		if (peerSipSession != null){
			if (peerSipSession.getState() == SipSession.State.EARLY || peerSipSession.getState() == SipSession.State.INITIAL){
				//	Send CANCEL
				SipServletRequest initialInvite = (SipServletRequest)peerSipSession.getAttribute(SipUtils.INITIAL_INVITE);
				SipServletRequest cancel = initialInvite.createCancel();
				cancel.send();
			}
			else if (peerSipSession.getState() == SipSession.State.CONFIRMED){
				SipServletRequest byeRequest = peerSipSession.createRequest("BYE");

				if (log.isLoggable(Level.FINE)) {
					log.fine("doCancel. sending req to other leg=" + byeRequest);
				}

				byeRequest.send();
			}
		}
	}

	@Override
	public void destroy() {
		// releases media server resources and connections.
		s_callsManager.cleanMediaServer();
	}

	/**
	 * Initialize JSR309 driver and necessary utilities.
	 */
	private void initJSR309Driver(){
		if(s_driverSingleton == null){
			synchronized (s_lock) {
				if(s_driverSingleton == null){
					Properties driverProperties = loadConfigurationProperties(JSR309_DRIVER_PROPERTIES_FILE_PATH);
					
					//Map for JNDI Entries to Properties
					HashMap<String, String> jndiToPropertyKeyMap = new HashMap<String, String>();
					jndiToPropertyKeyMap.put("mediaServerSipAddress", "mediaserver.sip.ipaddress");
					jndiToPropertyKeyMap.put("mediaServerSipPort",  "mediaserver.sip.port");
					
					//This will load the JNDI Values specified in the HashMap and override the properties file
	                   JNDIHelper helper = new JNDIHelper();

					helper.overrideProperties(jndiToPropertyKeyMap, driverProperties);
					
					s_driverSingleton = loadDriver(driverProperties);
					s_callsManager = CallsManager.getInstance();
					JSR309Utils.initMsControlFactory(s_driverSingleton.getMsControlFactory(), driverProperties);
				}
			}
		}
	}

	/**
	 * Loads JSR309 driver.
	 * Selected vendor is determined by the configuration properties file.
	 * @return JSR309 loaded driver
	 */
	private JSR309Driver loadDriver(Properties factoryOverrideProperties){
		return MSConnectorFactory.getDriver(factoryOverrideProperties);
	}

	/**
	 * Loads application configuration properties
	 * @return configuration properties
	 */
	private Properties loadConfigurationProperties(String propertyFile){
		Properties properties = new Properties();
		try {
			properties.load(getServletContext().getResourceAsStream(propertyFile));
		} catch (IOException e) {
			log.warning("loadConfigurationProperties failed.");
			e.printStackTrace();
		}

		return properties;
	}

	/**
	 * This is just a temporary fix and should be removed once the 309 use feature
	 * is exclused form app routing.
	 */
	private boolean shouldProcessInvite(SipServletRequest req){
		boolean processRequest = true;

		Address fromAddress = req.getFrom();

		if (fromAddress.getURI().isSipURI()){

			SipURI sipURI = (SipURI)fromAddress.getURI();

			if (log.isLoggable(Level.FINE)) {
				log.fine("shouldProcessInvite: user: " + sipURI.getUser());
			}

			if (sipURI.getUser() != null){
				if (sipURI.getUser().equals("dlgjmc") == true){
					if (log.isLoggable(Level.FINE)) {
						log.fine("shouldProcessInvite: found the 309 driver. DON'T PROCESS THE INVITE HERE!!!!");
					}
					processRequest = false;
				}
			}
		}

		return (processRequest);
	}
}
