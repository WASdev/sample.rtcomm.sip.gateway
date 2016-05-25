
# Setting Up the Rtcomm Client

First of you'll need to setup the [Rtcomm Client Sample](https://github.com/WASdev/lib.rtcomm.clientjs/blob/master/docs/sample.md) before making changes to it. Go [here](https://github.com/WASdev/lib.rtcomm.clientjs/blob/master/docs/sample.md) to set it up.

Now that you've setup the sample, go to the  <code>lib.rtcomm.clientjs/sample/videoClient.html</code> file.

###### Change the Rtcomm Topic Path

Go to <code>line 177</code>

Change
```javascript

rtcommTopicPath: "/rtcommVideoSample/"
```
to

```javascript

rtcommTopicPath: "/rtcomm/"
```

###### Disable broadcast video and trickleICE

Scroll down to <code>line 257</code>. You'll see this:

```javascript
...
webrtcConfig:{
  broadcast: {
    audio: true,
    video: true},
    ...
  }
}
...
```

We're going to disable *chat*, *generic_messages*, and *video*, as well as *trickleICE* so that section of code will end up like this:

```javascript
....
chat: false,
generic_messages: false,
webrtcConfig:{
  broadcast: {
    audio: true,
    video: false},
    trickleICE: false
    ...
  }
}
...

```

###### Disable the chat and generic_message protocols



Restart the server, this sample will setup an MQTT Broker on your localhost which you can connect the Rtcomm Gateway to. After that you can access the sample at http://localhost:8080/sample/videoClient.html.
1. Click 'Register', enter a name and click 'Go'
2. Next Steps:
  + [Setup Linphone](docs/setup-linphone.md)
  + [Run the Rtcomm SIP Gateway](README.md)
