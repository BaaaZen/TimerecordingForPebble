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
