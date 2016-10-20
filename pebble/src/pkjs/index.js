const C_JS_CMD_REQUEST_TL_TOKEN = 1;
const C_JS_CMD_RESPONSE_TL_TOKEN = 2;

var timelineToken = null;

// wait until js app is ready
Pebble.addEventListener('ready', function() {
  Pebble.getTimelineToken(function(token) {
    console.log('Timeline token is: ' + token);
    timelineToken = token;

    if(timelineToken != null) {
      /* send timeline token to watch */
      var msg = {};
      msg["JS_KEY_CMD"] = C_JS_CMD_RESPONSE_TL_TOKEN;
      msg["JS_KEY_TL_TOKEN"] = timelineToken;

      Pebble.sendAppMessage(msg, function() {
        console.log('Message sent successfully: ' + JSON.stringify(msg));
      }, function(e) {
        console.log('Message failed: ' + JSON.stringify(e));
      });
    }
  }, function(e) {
    console.log('Error fetching timeline token: ' + e);
  });
});

Pebble.addEventListener('appmessage', function(msg) {
  console.log('AppMessage received: ' + JSON.stringify(msg));
  if(msg.payload['JS_KEY_CMD']) {
    // this message is for the js interface
    if(msg.payload['JS_KEY_CMD'] == C_JS_CMD_REQUEST_TL_TOKEN) {
      if(timelineToken != null) {
        /* send timeline token to watch */
        var msg = {};
        msg["JS_KEY_CMD"] = C_JS_CMD_RESPONSE_TL_TOKEN;
        msg["JS_KEY_TL_TOKEN"] = timelineToken;

        Pebble.sendAppMessage(msg, function() {
          console.log('Message sent successfully: ' + JSON.stringify(msg));
        }, function(e) {
          console.log('Message failed: ' + JSON.stringify(e));
        });
      }
    }
  }
});
