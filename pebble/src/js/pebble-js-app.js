const C_JS_CMD_ADD_PIN = 1;
const C_JS_CMD_DEL_PIN = 2;

var timeline = require('./timeline');
var timelineToken = null;

// wait until js app is ready
Pebble.addEventListener('ready', function() {
  Pebble.getTimelineToken(function(token) {
    console.log('Timeline token is: ' + token);
    timelineToken = token;
  }, function(error) {
    console.log('Error fetching timeline token: ' + error);
  });
});

Pebble.addEventListener('appmessage', function(msg) {
  console.log('AppMessage received: ' + JSON.stringify(msg));
  if(msg.payload['JS_KEY_CMD']) {
    // this message is for the js interface
    if(msg.payload['JS_KEY_CMD'] == C_JS_CMD_ADD_PIN) {
      var pin = {
        'id': msg.payload['JS_KEY_TLPIN_ID']
      }
    } else if(msg.payload['JS_KEY_CMD'] == C_JS_CMD_DEL_PIN) {

    }

  }
});
