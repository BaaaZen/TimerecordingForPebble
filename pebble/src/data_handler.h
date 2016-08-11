/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

#pragma once
#include <pebble.h>

#define STRUCT_BUF_LEN 35

struct str_text {
  GColor c_text;
  char s_text[STRUCT_BUF_LEN];
};

struct str_display {
  bool enabled;
  struct str_text time1;
  struct str_text time2;
  struct str_text descr;
};

void data_clear_display_cache(void);
void data_update_display_cache(int cache_id, char *d_s, GColor d_c, char *t1_s, GColor t1_c, char *t2_s, GColor t2_c);
void data_update_title_cache(char *d_s, GColor d_c);
void data_update_checked_in_state(bool ci);
void data_nav_next(void);
static void data_update_gui(void);
void data_init(void);
void data_deinit(void);
