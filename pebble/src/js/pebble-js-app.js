const C_JS_CMD_SET_PIN = 1;
const C_JS_CMD_DEL_PIN = 2;

const C_JS_PIN_TYPE_FINISH_TIME = 1;

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
    if(msg.payload['JS_KEY_CMD'] == C_JS_CMD_SET_PIN) {
      // all necessary data present?
      if(msg.payload['JS_KEY_TLPIN_ID'] && msg.payload['JS_KEY_TLPIN_TIME'] && msg.payload['JS_KEY_TLPIN_NAME'] && msg.payload['JS_KEY_TLPIN_TYPE']) {
        // layout
        var layout = {}
        layout['type'] = 'genericPin';
        layout['title'] = msg.payload['JS_KEY_TLPIN_NAME'];
        layout['lastUpdated'] = new Date().toISOString();
        if(msg.payload['JS_KEY_TLPIN_TYPE'] == -1 /* this need to be replaced for more than one type! */) {
          /* fill me */
        } else {
          layout['tinyIcon'] = 'system://images/RESULT_SENT';
        }

        var pin = {};
        pin['id'] = msg.payload['JS_KEY_TLPIN_ID'];
        pin['time'] = msg.payload['JS_KEY_TLPIN_TIME'];
        pin['layout'] = layout;
        /* TODO: set reminder */
        timeline.insertUserPin(pin, function(responseText) {
          console.log('Inserting pin via Web-API results in: ' + responseText);
        });
      } else {
        console.log('Incomplete SET_PIN message!');
      }

    } else if(msg.payload['JS_KEY_CMD'] == C_JS_CMD_DEL_PIN) {
      /* TODO */
    }

  }
});
