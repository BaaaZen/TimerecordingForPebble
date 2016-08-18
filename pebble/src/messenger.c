/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

#include <pebble.h>
#include "messenger.h"
#include "data_handler.h"

static void msg_inbox_received_callback(DictionaryIterator *iter, void *context) {
  // message received -> fetch and parse command in package
  Tuple *cmd_tuple = dict_find(iter, MESSAGE_KEY_CMD);
  if(cmd_tuple) {
    uint8_t cmd = cmd_tuple->value->uint8;

    if(cmd == MESSAGE_CMD_STATUS_RESPONSE) {
      msg_parse_cmd_status_response(iter);
    }
  }
}

static GColor msg_parse_color(uint8_t c) {
  if(c == MESSAGE_COLOR_WHITE) return GColorWhite;
#ifdef PBL_COLOR
  /* only use color if pebble supports it */
  else if(c == MESSAGE_COLOR_RED) return GColorDarkCandyAppleRed;
  else if(c == MESSAGE_COLOR_GREEN) return GColorIslamicGreen;
  else if(c == MESSAGE_COLOR_BLUE) return GColorBlue;
  else if(c == MESSAGE_COLOR_YELLOW) return GColorYellow;
  else if(c == MESSAGE_COLOR_ORANGE) return GColorOrange;
  else if(c == MESSAGE_COLOR_DARKGRAY) return GColorDarkGray;
  else if(c == MESSAGE_COLOR_LIGHTGRAY) return GColorLightGray;
#endif
  else return GColorBlack;
}

static void msg_parse_cmd_status_response(DictionaryIterator *iter) {
  /* clear all data from data handler */
  Tuple *t_f_clearall = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_CLEARALL);
  if(t_f_clearall) {
    if(t_f_clearall->value->uint8 > 0) data_clear_display_cache();
  }

  /* checked in state */
  Tuple *t_ci = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_STATUS_CHECKED_IN);
  if(t_ci) {
    data_update_checked_in_state(t_ci->value->uint8 > 0);
  }

  /* common description */
  Tuple *t_d_s = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_TEXT);
  Tuple *t_d_c = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_COLOR);
  if(t_d_s && t_d_c) {
    data_update_title_cache(t_d_s->value->cstring, msg_parse_color(t_d_c->value->uint8));
  }

  /* face */
  Tuple *t_f_id = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_ID);
  Tuple *t_f_d_s = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT);
  Tuple *t_f_d_c = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR);
  Tuple *t_f_t1_s = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT);
  Tuple *t_f_t1_c = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR);
  Tuple *t_f_t2_s = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT);
  Tuple *t_f_t2_c = dict_find(iter, MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR);
  if(t_f_id && t_f_d_s && t_f_d_c && t_f_t1_s && t_f_t1_c && t_f_t2_s && t_f_t2_c) {
    data_update_display_cache(t_f_id->value->uint8, t_f_d_s->value->cstring, msg_parse_color(t_f_d_c->value->uint8),
      t_f_t1_s->value->cstring, msg_parse_color(t_f_t1_c->value->uint8),
      t_f_t2_s->value->cstring, msg_parse_color(t_f_t2_c->value->uint8));
  }
}

static void msg_inbox_dropped_callback(AppMessageResult reason, void *context) {
  // A message was received, but had to be dropped
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped. Reason: %d", (int)reason);
}

static void msg_outbox_sent_callback(DictionaryIterator *iter, void *context) {
  // The message just sent has been successfully delivered

}

static void msg_outbox_failed_callback(DictionaryIterator *iter, AppMessageResult reason, void *context) {
  // The message just sent failed to be delivered
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message send failed. Reason: %d", (int)reason);
}


void msg_cmd_fetch_status(void) {
  // Declare the dictionary's iterator
  DictionaryIterator *out_msg;

  // Prepare the outbox buffer for this message
  AppMessageResult result = app_message_outbox_begin(&out_msg);
  if(result == APP_MSG_OK) {
    // Add an item to ask for weather data
    dict_write_uint8(out_msg, MESSAGE_KEY_CMD, MESSAGE_CMD_STATUS_REQUEST);

    // Send this message
    result = app_message_outbox_send();

    // Check the result
    if(result != APP_MSG_OK) {
      APP_LOG(APP_LOG_LEVEL_ERROR, "Error sending the outbox: %d", (int)result);
    }
  } else {
    // The outbox cannot be used right now
    APP_LOG(APP_LOG_LEVEL_ERROR, "Error preparing the outbox: %d", (int)result);
  }
}

void msg_cmd_action_punch(void) {
  // Declare the dictionary's iterator
  DictionaryIterator *out_msg;

  // Prepare the outbox buffer for this message
  AppMessageResult result = app_message_outbox_begin(&out_msg);
  if(result == APP_MSG_OK) {
    // Add an item to ask for weather data
    dict_write_uint8(out_msg, MESSAGE_KEY_CMD, MESSAGE_CMD_ACTION_PUNCH);

    // Send this message
    result = app_message_outbox_send();

    // Check the result
    if(result != APP_MSG_OK) {
      APP_LOG(APP_LOG_LEVEL_ERROR, "Error sending the outbox: %d", (int)result);
    }
  } else {
    // The outbox cannot be used right now
    APP_LOG(APP_LOG_LEVEL_ERROR, "Error preparing the outbox: %d", (int)result);
  }
}

void msg_cmd_fetch_tasks(void) {
  // Declare the dictionary's iterator
  DictionaryIterator *out_msg;

  // Prepare the outbox buffer for this message
  AppMessageResult result = app_message_outbox_begin(&out_msg);
  if(result == APP_MSG_OK) {
    // Add an item to ask for weather data
    dict_write_uint8(out_msg, MESSAGE_KEY_CMD, MESSAGE_CMD_TASKS_REQUEST);

    // Send this message
    result = app_message_outbox_send();

    // Check the result
    if(result != APP_MSG_OK) {
      APP_LOG(APP_LOG_LEVEL_ERROR, "Error sending the outbox: %d", (int)result);
    }
  } else {
    // The outbox cannot be used right now
    APP_LOG(APP_LOG_LEVEL_ERROR, "Error preparing the outbox: %d", (int)result);
  }
}


void msg_init(void) {
  const int inbound_size = 256;
  const int outbound_size = 64;
  app_message_open(inbound_size, outbound_size);
  app_message_register_inbox_received(msg_inbox_received_callback);
  app_message_register_inbox_dropped(msg_inbox_dropped_callback);
  app_message_register_outbox_sent(msg_outbox_sent_callback);
  app_message_register_outbox_failed(msg_outbox_failed_callback);
}

void msg_deinit(void) {
  app_message_deregister_callbacks();
}
