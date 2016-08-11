/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

#pragma once
void gui_main_init(void);
void gui_main_deinit(void);

void gui_update_upper_time(const char *t_descr, const char *t_time1, GColor c_time1, const char *t_time2, GColor c_time2);
void gui_update_lower_time(const char *t_descr, const char *t_time1, GColor c_time1, const char *t_time2, GColor c_time2);
void gui_update_common(bool b_started, const char *t_descr, GColor c_descr_i, GColor c_bg_i);

static void gui_ab_update_icons(void);
static void gui_ab_onclick_up(ClickRecognizerRef recognizer, void *context);
static void gui_ab_onclick_select(ClickRecognizerRef recognizer, void *context);
static void gui_ab_onclick_down(ClickRecognizerRef recognizer, void *context);
static void gui_ab_click_provider(void *context);
static void gui_layer_content_update(Layer *layer, GContext *ctx);
static void gui_layer_bg_update(Layer *layer, GContext *ctx);
static void gui_tick_handler(struct tm *time_now, TimeUnits changed);
static void gui_main_window_load(Window *window);
static void gui_main_window_unload(Window *window);
