#include <pebble.h>
#include "data_handler.h"
#include "gui_main.h"

#define DISPLAY_CACHE_SIZE_MAX 5

static struct str_display display_cache[DISPLAY_CACHE_SIZE_MAX];
static int display_cache_size = 0;
static struct str_text title_cache;
static bool checked_in = false;
static int display_id = 0;

void data_clear_display_cache(void) {
  /* clean up display cache */
  display_cache_size = 0;
  for(int i=0; i<5; i++) {
    display_cache[i].enabled = false;
  }
}

void data_update_display_cache(int cache_id, char *d_s, GColor d_c, char *t1_s, GColor t1_c, char *t2_s, GColor t2_c) {
  if(cache_id < 0 || cache_id >= DISPLAY_CACHE_SIZE_MAX) return;
  
  strncpy(display_cache[cache_id].descr.s_text, d_s, STRUCT_BUF_LEN - 1);
  display_cache[cache_id].descr.s_text[STRUCT_BUF_LEN - 1] = '\0';
  display_cache[cache_id].descr.c_text = d_c;

  strncpy(display_cache[cache_id].time1.s_text, t1_s, STRUCT_BUF_LEN - 1);
  display_cache[cache_id].time1.s_text[STRUCT_BUF_LEN - 1] = '\0';
  display_cache[cache_id].time1.c_text = t1_c;
  
  strncpy(display_cache[cache_id].time2.s_text, t2_s, STRUCT_BUF_LEN - 1);
  display_cache[cache_id].time2.s_text[STRUCT_BUF_LEN - 1] = '\0';
  display_cache[cache_id].time2.c_text = t2_c;
  
  display_cache[cache_id].enabled = true;
  if(display_cache_size == cache_id) {
    while(display_cache_size < DISPLAY_CACHE_SIZE_MAX) {
      if(display_cache[display_cache_size].enabled) {
        display_cache_size++;
      } else {
				break;
			}
    }
  }

  if(display_cache_size > 0) data_update_gui();
}
  
void data_update_title_cache(char *d_s, GColor d_c) {
  strncpy(title_cache.s_text, d_s, STRUCT_BUF_LEN - 1);
  title_cache.s_text[STRUCT_BUF_LEN - 1] = '\0';
  title_cache.c_text = d_c;

  data_update_gui();
}

void data_update_checked_in_state(bool ci) {
  checked_in = ci;
  
  data_update_gui();
}

void data_nav_next(void) {
  if(display_cache_size > 0) {
    display_id = (display_id + 1) % display_cache_size;
   
    data_update_gui();
  }
}

static void data_update_gui(void) {
  gui_update_common(checked_in, title_cache.s_text, title_cache.c_text, GColorWhite);

  bool upper_set = false;
  if(display_cache_size > 1) {
    // we have data for upper display
    int upper_display_id = (display_id + 1) % display_cache_size;
    if(display_cache[upper_display_id].enabled) {
      // and data is enabled
      gui_update_upper_time(display_cache[upper_display_id].descr.s_text, display_cache[upper_display_id].time1.s_text, display_cache[upper_display_id].time1.c_text, display_cache[upper_display_id].time2.s_text, display_cache[upper_display_id].time2.c_text);
      upper_set = true;
    }
  }
  if(!upper_set) {
    // no data available
    gui_update_upper_time("", "", GColorBlack, "", GColorBlack);
  }
  
  bool lower_set = false;
  if(display_cache_size > 0) {
    // we have data for lower display
    int lower_display_id = display_id % display_cache_size;
    if(display_cache[lower_display_id].enabled) {
      // and data is enabled
      gui_update_lower_time(display_cache[lower_display_id].descr.s_text, display_cache[lower_display_id].time1.s_text, display_cache[lower_display_id].time1.c_text, display_cache[lower_display_id].time2.s_text, display_cache[lower_display_id].time2.c_text);
      lower_set = true;
    }
  }
  if(!lower_set) {
    // no data available
    gui_update_lower_time("", "", GColorBlack, "", GColorBlack);
  }
}