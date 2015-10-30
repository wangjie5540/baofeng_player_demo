#ifndef _PLAYER_H_
#define _PLAYER_H_

#ifdef __cplusplus
extern "C" {
#endif

bool player_init();
void player_set_source(const char *url);
void player_start();
void player_pause();
void player_stop();
int player_get_duration();
int player_get_cur_pos();
void player_seek_to(int msec);
bool player_is_playing();

#ifdef __cplusplus
}
#endif

#endif
