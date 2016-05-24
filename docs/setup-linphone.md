# Setup Linphone to Use With Rtcomm SIP Gateway


You can download and learn more about Linphone at http://www.linphone.org/.

After installing Linphone you'll need to configure it to work with the Gateway.

1. Click Linphone (Options if you're using Mac OSX) > Preferences
2. Go to the *Codecs* tab
3. Disable all the *Video* codecs.
4. Disable all the *Audio* codecs, **except** PCMU 8000.

In order to place a call to the Gateway/Rtcomm Endpoint. Enter the address under the *SIP Adddress or phone number:* text field in Linphone. The address scheme would be a regular SIP Address:

```text
sip:<user>@<IP Address>:<Port>
```

**For example**, if the Rtcomm Endpoint is registered as 'John'. And the IP Address of the gateway is <code>1.2.3.123</code> and listening on <code>5060</code>
 In Linphone place a call with the following address:

 sip:John@1.2.3.123:5060.

 Upon placing a call the Rtcomm Client should receive a dialog asking to respond to the call.
