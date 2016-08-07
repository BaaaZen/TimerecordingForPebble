#include <pebble.h>
#include "gui_main.h"
#include "data_handler.h"
#include "messenger.h"

#define PADDING_X 3
#define PADDING_Y 3

static Window *s_main_window;
static ActionBarLayer *s_action_bar;
static Layer *s_layer_bg, *s_layer_content;
static TextLayer *s_text_upper_descr, *s_text_upper_time1, *s_text_upper_time2;
static TextLayer *s_text_center_descr;
static TextLayer *s_text_lower_descr, *s_text_lower_time1, *s_text_lower_time2;
static GBitmap *s_ab_icon_check_in, *s_ab_icon_check_out, *s_ab_icon_task, *s_ab_icon_switch;

static char buf_descr[30];
static GColor c_bg, c_descr;
static char buf_time_u1[7], buf_time_u2[7], buf_time_u_descr[25];
static GColor c_time_u1, c_time_u2;
static char buf_time_l1[7], buf_time_l2[7], buf_time_l_descr[25];
static GColor c_time_l1, c_time_l2;

static bool started = false;

void gui_update_upper_time(const char *t_descr, const char *t_time1, GColor c_time1, const char *t_time2, GColor c_time2) {
  strncpy(buf_time_u_descr, t_descr, 24);
  buf_time_u_descr[24] = '\0';
  c_time_u1 = c_time1;
  strncpy(buf_time_u1, t_time1, 6);
  buf_time_u1[6] = '\0';
  c_time_u2 = c_time2;
  strncpy(buf_time_u2, t_time2, 6);
  buf_time_u1[6] = '\0';
  
  layer_mark_dirty(window_get_root_layer(s_main_window));
}

void gui_update_lower_time(const char *t_descr, const char *t_time1, GColor c_time1, const char *t_time2, GColor c_time2) {
  strncpy(buf_time_l_descr, t_descr, 24);
  buf_time_l_descr[24] = '\0';
  c_time_l1 = c_time1;
  strncpy(buf_time_l1, t_time1, 6);
  buf_time_l1[6] = '\0';
  c_time_l2 = c_time2;
  strncpy(buf_time_l2, t_time2, 6);
  buf_time_l1[6] = '\0';
  
  layer_mark_dirty(window_get_root_layer(s_main_window));
}

void gui_update_common(bool b_started, const char *t_descr, GColor c_descr_i, GColor c_bg_i) {
  strncpy(buf_descr, t_descr, 29);
  buf_descr[29] = '\0';
  c_descr = c_descr_i;
  c_bg = c_bg_i;
  
  started = b_started;
  
  gui_ab_update_icons();
  layer_mark_dirty(window_get_root_layer(s_main_window));
}

static void gui_ab_update_icons(void) {
  if(started) {
    action_bar_layer_set_icon(s_action_bar, BUTTON_ID_UP, s_ab_icon_check_out);
    action_bar_layer_set_icon(s_action_bar, BUTTON_ID_SELECT, s_ab_icon_task);
    action_bar_layer_set_icon(s_action_bar, BUTTON_ID_DOWN, s_ab_icon_switch);
  } else {
    action_bar_layer_set_icon(s_action_bar, BUTTON_ID_UP, s_ab_icon_check_in);
    action_bar_layer_clear_icon(s_action_bar, BUTTON_ID_SELECT);
    action_bar_layer_set_icon(s_action_bar, BUTTON_ID_DOWN, s_ab_icon_switch);
  }
}

static void gui_ab_onclick_up(ClickRecognizerRef recognizer, void *context) {
  msg_cmd_action_punch();
}

static void gui_ab_onclick_select(ClickRecognizerRef recognizer, void *context) {
	/* TODO: menu for task selector */
}

static void gui_ab_onclick_down(ClickRecognizerRef recognizer, void *context) {
  data_nav_next();
}

static void gui_ab_click_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_UP, gui_ab_onclick_up);
  window_single_click_subscribe(BUTTON_ID_SELECT, gui_ab_onclick_select);
  window_single_click_subscribe(BUTTON_ID_DOWN, gui_ab_onclick_down);
}

static void gui_layer_content_update(Layer *layer, GContext *ctx) {
  text_layer_set_text(s_text_upper_descr, buf_time_u_descr);
  text_layer_set_text_color(s_text_upper_time1, c_time_u1);
  text_layer_set_text(s_text_upper_time1, buf_time_u1);
  text_layer_set_text_color(s_text_upper_time2, c_time_u2);
  text_layer_set_text(s_text_upper_time2, buf_time_u2);
  
  text_layer_set_text(s_text_lower_descr, buf_time_l_descr);
  text_layer_set_text_color(s_text_lower_time1, c_time_l1);
  text_layer_set_text(s_text_lower_time1, buf_time_l1);
  text_layer_set_text_color(s_text_lower_time2, c_time_l2);
  text_layer_set_text(s_text_lower_time2, buf_time_l2);
  
  text_layer_set_text_color(s_text_center_descr, c_descr);
  text_layer_set_text(s_text_center_descr, buf_descr);
}

static void gui_layer_bg_update(Layer *layer, GContext *ctx) {
  graphics_context_set_fill_color(ctx, c_bg);
  graphics_fill_rect(ctx, layer_get_bounds(layer), 0, GCornerNone);
  
  int width = layer_get_frame(layer).size.w; 
  int height = layer_get_frame(layer).size.h;

  graphics_context_set_stroke_color(ctx, PBL_IF_COLOR_ELSE(GColorLightGray, GColorBlack));
  graphics_context_set_stroke_width(ctx, 1);
  graphics_draw_line(ctx, GPoint(PADDING_X, height/2-10), GPoint(width-PADDING_X, height/2-10));
  graphics_draw_line(ctx, GPoint(PADDING_X, height/2+10), GPoint(width-PADDING_X, height/2+10));
}

static void gui_tick_handler(struct tm *time_now, TimeUnits changed) {
	msg_cmd_fetch_status();
}

static void gui_main_window_load(Window *window) {
  /* root layer */
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);
  
  /* create action bar */
  s_action_bar = action_bar_layer_create();
  action_bar_layer_add_to_window(s_action_bar, window);
  action_bar_layer_set_background_color(s_action_bar, PBL_IF_COLOR_ELSE(GColorTiffanyBlue, GColorWhite));
  action_bar_layer_set_click_config_provider(s_action_bar, gui_ab_click_provider);
  gui_ab_update_icons();
  
  bounds.size.w -= ACTION_BAR_WIDTH;

  /* background layer */
  s_layer_bg = layer_create(bounds);
  layer_set_update_proc(s_layer_bg, gui_layer_bg_update);
  layer_add_child(window_layer, s_layer_bg);

  /* create content layer on the left */
  s_layer_content = layer_create(bounds);
  layer_set_update_proc(s_layer_content, gui_layer_content_update);
  layer_add_child(window_layer, s_layer_content);
  
  int kern = 2;
  /* text layer center */
  int ch = 22;
  s_text_center_descr = text_layer_create(GRect(PADDING_X, bounds.size.h/2-(ch+kern)/2, bounds.size.w-2*PADDING_X, ch));
  text_layer_set_font(s_text_center_descr, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_background_color(s_text_center_descr, GColorClear);
  text_layer_set_text_alignment(s_text_center_descr, GTextAlignmentCenter);
  text_layer_set_text(s_text_center_descr, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_center_descr));
  
  /* text layers upper */
  /*   description */
  int dh = 22;
  int ds = 3;
  s_text_upper_descr = text_layer_create(GRect(PADDING_X, bounds.size.h/2-dh-ch/2, bounds.size.w-2*PADDING_X, dh));
  text_layer_set_font(s_text_upper_descr, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_background_color(s_text_upper_descr, GColorClear);
  text_layer_set_text_alignment(s_text_upper_descr, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_upper_descr, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_upper_descr));
  /*   time 1 */
  int t1h = 32;
  int t1s = 5;
  s_text_upper_time1 = text_layer_create(GRect(PADDING_X*2, bounds.size.h/2-dh-ch/2+ds-t1h, bounds.size.w-2*PADDING_X*2, t1h));
  text_layer_set_font(s_text_upper_time1, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  text_layer_set_background_color(s_text_upper_time1, GColorClear);
  text_layer_set_text_alignment(s_text_upper_time1, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_upper_time1, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_upper_time1));
  /*  time 2 */
  int t2h = 20;
  s_text_upper_time2 = text_layer_create(GRect(PADDING_X, bounds.size.h/2-kern-dh-ch/2+ds-t1h+t1s-t2h, bounds.size.w-2*PADDING_X, t2h));
  text_layer_set_font(s_text_upper_time2, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_background_color(s_text_upper_time2, GColorClear);
  text_layer_set_text_alignment(s_text_upper_time2, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_upper_time2, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_upper_time2));

  /* text layer lower */
  /*   description */
  s_text_lower_descr = text_layer_create(GRect(PADDING_X, bounds.size.h/2-kern+ch/2, bounds.size.w-2*PADDING_X, dh));
  text_layer_set_font(s_text_lower_descr, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_background_color(s_text_lower_descr, GColorClear);
  text_layer_set_text_alignment(s_text_lower_descr, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_lower_descr, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_lower_descr));
  /*   time 1 */
  s_text_lower_time1 = text_layer_create(GRect(PADDING_X*2, bounds.size.h/2-kern+ch/2+dh-ds, bounds.size.w-2*PADDING_X*2, t1h));
  text_layer_set_font(s_text_lower_time1, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  text_layer_set_background_color(s_text_lower_time1, GColorClear);
  text_layer_set_text_alignment(s_text_lower_time1, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_lower_time1, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_lower_time1));
  /*  time 2 */
  s_text_lower_time2 = text_layer_create(GRect(PADDING_X, bounds.size.h/2-kern+ch/2+dh-ds+t1h-t1s, bounds.size.w-2*PADDING_X, t2h));
  text_layer_set_font(s_text_lower_time2, fonts_get_system_font(FONT_KEY_GOTHIC_18));
  text_layer_set_background_color(s_text_lower_time2, GColorClear);
  text_layer_set_text_alignment(s_text_lower_time2, PBL_IF_ROUND_ELSE(GTextAlignmentRight, GTextAlignmentCenter));
  text_layer_set_text(s_text_lower_time2, "");
  layer_add_child(s_layer_content, text_layer_get_layer(s_text_lower_time2));
}

static void gui_main_window_unload(Window *window) {
  text_layer_destroy(s_text_center_descr);
  
  text_layer_destroy(s_text_upper_descr);
  text_layer_destroy(s_text_upper_time1);
  text_layer_destroy(s_text_upper_time2);
  text_layer_destroy(s_text_lower_descr);
  text_layer_destroy(s_text_lower_time1);
  text_layer_destroy(s_text_lower_time2);
  
  action_bar_layer_destroy(s_action_bar);
  
  layer_destroy(s_layer_content);
  layer_destroy(s_layer_bg);
}

void gui_main_init(void) {
  s_ab_icon_check_in = gbitmap_create_with_resource(RESOURCE_ID_AB_BUTTON_CHECK_IN);
  s_ab_icon_check_out = gbitmap_create_with_resource(RESOURCE_ID_AB_BUTTON_CHECK_OUT);
  s_ab_icon_task = gbitmap_create_with_resource(RESOURCE_ID_AB_BUTTON_TASK);
  s_ab_icon_switch = gbitmap_create_with_resource(RESOURCE_ID_AB_BUTTON_SWITCH);
  
  s_main_window = window_create();
  window_set_window_handlers(s_main_window, (WindowHandlers) {
    .load = gui_main_window_load,
    .unload = gui_main_window_unload
  });
  window_stack_push(s_main_window, true);
  
  gui_update_upper_time("", "", GColorBlack, "", GColorBlack);
  gui_update_lower_time("", "", GColorBlack, "", GColorBlack);
  gui_update_common(false, "Laden ...", GColorBlack, GColorWhite);

	/* update data every minute */
	tick_timer_service_subscribe(MINUTE_UNIT, gui_tick_handler);
}

void gui_main_deinit(void) {
	tick_timer_service_unsubscribe();
	
  window_destroy(s_main_window);
  
  gbitmap_destroy(s_ab_icon_check_in);
  gbitmap_destroy(s_ab_icon_check_out);
  gbitmap_destroy(s_ab_icon_task);
  gbitmap_destroy(s_ab_icon_switch);
}