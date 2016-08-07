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
static void gui_main_window_load(Window *window);
static void gui_main_window_unload(Window *window);
