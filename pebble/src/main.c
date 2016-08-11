/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
	http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

#include <pebble.h>
#include "data_handler.h"
#include "gui_main.h"
#include "messenger.h"

static void init(void) {
	data_init();
  msg_init();
  gui_main_init();
  msg_cmd_fetch_status();
}

static void deinit(void) {
  gui_main_deinit();
  msg_deinit();
	data_deinit();
}

int main(void) {
  init();
  app_event_loop();
  deinit();
}
