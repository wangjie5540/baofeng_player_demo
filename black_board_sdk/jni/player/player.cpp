
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#define __STDC_LIMIT_MACROS

#include <inttypes.h>
#include <math.h>
#include <unistd.h>
#include <limits.h>
#include <signal.h>
#include <assert.h>
#include <stdint.h>
#include <string>

extern "C"
{
#include "libavutil/avstring.h"
#include "libavutil/colorspace.h"
#include "libavutil/mathematics.h"
#include "libavutil/pixdesc.h"
#include "libavutil/imgutils.h"
#include "libavutil/dict.h"
#include "libavutil/parseutils.h"
#include "libavutil/samplefmt.h"
#include "libavutil/avassert.h"
#include "libavutil/time.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/opt.h"
#include "libavcodec/avfft.h"
#include "libswresample/swresample.h"

#include <android/log.h>

#include <SDL.h>
#include <SDL_thread.h>
#include <SDL_events.h>

#include "player.h"
}

using namespace std;

///////////////////////////////////////////////////////////////////////////////

extern "C" int Android_JNI_SendMessage(int message, int param);

///////////////////////////////////////////////////////////////////////////////

#define TAG "bfplayer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

///////////////////////////////////////////////////////////////////////////////
// messages

#define MSG_STATE_CHANGED 	1
#define MSG_ERROR			2

///////////////////////////////////////////////////////////////////////////////
// events

#define PLAYER_ALLOC_EVENT           (SDL_USEREVENT + 1)
#define PLAYER_QUIT_EVENT            (SDL_USEREVENT + 2)
#define PLAYER_TOGGLE_PAUSE_EVENT    (SDL_USEREVENT + 3)
#define PLAYER_SEEK_EVENT            (SDL_USEREVENT + 4)
#define PLAYER_COMPLETE_EVENT        (SDL_USEREVENT + 5)
#define PLAYER_REFRESH_EVENT         (SDL_USEREVENT + 6)
#define PLAYER_SURFACE_CHANGED_EVENT (SDL_USEREVENT + 7)

///////////////////////////////////////////////////////////////////////////////
// consts

#define MAX_QUEUE_SIZE (15 * 1024 * 1024)

// Minimum SDL audio buffer size, in samples.
#define SDL_AUDIO_MIN_BUFFER_SIZE 512
// Calculate actual buffer size keeping in mind not cause too frequent audio callbacks
#define SDL_AUDIO_MAX_CALLBACKS_PER_SEC 30

// no AV sync correction is done if below the minimum AV sync threshold
#define AV_SYNC_THRESHOLD_MIN 0.04
// AV sync correction is done if above the maximum AV sync threshold
#define AV_SYNC_THRESHOLD_MAX 0.1
// If a frame duration is longer than this, it will not be duplicated to compensate AV sync
#define AV_SYNC_FRAMEDUP_THRESHOLD 0.1
// no AV correction is done if too big error
#define AV_NOSYNC_THRESHOLD 10.0

// maximum audio speed change to get correct sync
#define SAMPLE_CORRECTION_PERCENT_MAX 10

// external clock speed adjustment constants for realtime sources based on buffer fullness
#define EXTERNAL_CLOCK_SPEED_MIN  0.900
#define EXTERNAL_CLOCK_SPEED_MAX  1.010
#define EXTERNAL_CLOCK_SPEED_STEP 0.001

// we use about AUDIO_DIFF_AVG_NB A-V differences to make the average
#define AUDIO_DIFF_AVG_NB   20

// polls for possible required screen refresh at least this often, should be less than 1/fps
#define REFRESH_RATE 0.01

// NOTE: the size must be big enough to compensate the hardware audio buffersize size
// TODO: We assume that a decoded and resampled frame fits into this buffer
#define SAMPLE_ARRAY_SIZE (8 * 65536)

#define CURSOR_HIDE_DELAY 1000000

#define VIDEO_PICTURE_QUEUE_SIZE 3
#define SUBPICTURE_QUEUE_SIZE 16

///////////////////////////////////////////////////////////////////////////////
// types

enum PlayState
{
	STATE_PREPARING = 0,
	STATE_BUFFERING = 1,
	STATE_READY = 2,
	STATE_ENDED = 3,
};

enum ErrorCode
{
	EC_OPEN_ERROR = 1
};

enum
{
	AV_SYNC_AUDIO_MASTER,   // default choice
	AV_SYNC_VIDEO_MASTER,
	AV_SYNC_EXTERNAL_CLOCK, // synchronize to an external clock
};

enum ShowMode
{
	SHOW_MODE_NONE = -1,
	SHOW_MODE_VIDEO = 0,
	SHOW_MODE_WAVES,
	SHOW_MODE_RDFT,
	SHOW_MODE_NB
};

struct MyAVPacketList
{
	AVPacket pkt;
	struct MyAVPacketList *next;
	int serial;
};

struct PacketQueue
{
	MyAVPacketList *first_pkt, *last_pkt;
	int nb_packets;
	int size;
	bool abort_request;
	int serial;
	SDL_mutex *mutex;
	SDL_cond *cond;
};

struct VideoPicture
{
	double pts;             // presentation timestamp for this picture
	double duration;        // estimated duration based on frame rate
	int64_t pos;            // byte position in file

	AVFrame *av_frame;
	int byte_count;
	uint8_t *buffer;

	int width, height;      // source height & width
	int allocated;
	int reallocate;
	int serial;

	AVRational sar;
};

struct SubPicture
{
	double pts;             // presentation time stamp for this picture
	AVSubtitle sub;
	int serial;
};

struct AudioParams
{
	int freq;
	int channels;
	int64_t channel_layout;
	enum AVSampleFormat fmt;
	int frame_size;
	int bytes_per_sec;
};

struct Clock
{
	double pts;           // clock base
	double pts_drift;     // clock base minus time at which we updated the clock
	double last_updated;
	double speed;
	int serial;           // clock is based on a packet with this serial
	int paused;
	int *queue_serial;    // pointer to the current packet queue serial, used for obsolete clock detection
};

struct VideoState
{
	SDL_Thread *event_loop_tid;
	SDL_Thread *read_tid;
	SDL_Thread *video_tid;
	AVInputFormat *iformat;
	int no_background;
	bool abort_request;
	bool force_refresh;
	int paused;
	int last_paused;
	int queue_attachments_req;
	int seek_req;
	int seek_flags;
	int64_t seek_pos;
	int64_t seek_rel;
	int read_pause_return;
	AVFormatContext *ic;
	int realtime;
	int audio_finished;
	int video_finished;

	Clock audclk;
	Clock vidclk;
	Clock extclk;

	int audio_stream;

	int av_sync_type;

	double audio_clock;
	int audio_clock_serial;
	double audio_diff_cum;         // used for AV difference average computation
	double audio_diff_avg_coef;
	double audio_diff_threshold;
	int audio_diff_avg_count;
	AVStream *audio_st;
	PacketQueue audioq;
	int audio_hw_buf_size;
	uint8_t silence_buf[SDL_AUDIO_MIN_BUFFER_SIZE];
	uint8_t *audio_buf;
	uint8_t *audio_buf1;
	unsigned int audio_buf_size;   // in bytes
	unsigned int audio_buf1_size;
	int audio_buf_index;           // in bytes
	int audio_write_buf_size;
	int audio_buf_frames_pending;
	AVPacket audio_pkt_temp;
	AVPacket audio_pkt;
	int audio_pkt_temp_serial;
	int audio_last_serial;
	struct AudioParams audio_src;
	struct AudioParams audio_tgt;
	struct SwrContext *swr_ctx;
	int frame_drops_early;
	int frame_drops_late;
	AVFrame *frame;
	int64_t audio_frame_next_pts;
	ShowMode show_mode;

	int16_t sample_array[SAMPLE_ARRAY_SIZE];
	int sample_array_index;
	int last_i_start;
	RDFTContext *rdft;
	int rdft_bits;
	FFTSample *rdft_data;
	int xpos;
	double last_vis_time;

	SDL_Thread *subtitle_tid;
	int subtitle_stream;
	AVStream *subtitle_st;
	PacketQueue subtitleq;
	SubPicture subpq[SUBPICTURE_QUEUE_SIZE];
	int subpq_size, subpq_rindex, subpq_windex;
	SDL_mutex *subpq_mutex;
	SDL_cond *subpq_cond;

	double frame_timer;
	double frame_last_returned_time;
	double frame_last_filter_delay;
	int video_stream;
	AVStream *video_st;
	PacketQueue videoq;
	int64_t video_current_pos;      // current displayed file pos
	double max_frame_duration;      // maximum duration of a frame - above this, we consider the jump a timestamp discontinuity
	VideoPicture pictq[VIDEO_PICTURE_QUEUE_SIZE];
	int pictq_size, pictq_rindex, pictq_windex, pictq_rindex_shown;
	SDL_mutex *pictq_mutex;
	SDL_cond *pictq_cond;
	struct SwsContext *img_convert_ctx;
	SDL_Rect last_display_rect;

	char filename[1024];
	int width, height, xleft, ytop;
	int step;

	int last_video_stream;
	int last_audio_stream;
	int last_subtitle_stream;

	SDL_cond *continue_read_thread;

	bool is_buffered_ok;
	bool sent_play_complete_msg;
	int total_duration;  // msec
	int current_pos;     // msec
};

///////////////////////////////////////////////////////////////////////////////
// global variables

static string player_source;
static VideoState *current_stream = NULL;

static SDL_Window *window = NULL;
static SDL_Renderer *renderer = NULL;
static SDL_Texture *texture = NULL;

static int64_t sws_flags = SWS_BICUBIC;
static PixelFormat dst_fix_fmt = AV_PIX_FMT_YUV420P;
static int display_fix_fmt = SDL_PIXELFORMAT_IYUV;

// options specified by the user
static int min_buffer_frames = 1;
static AVInputFormat *file_iformat;
static int audio_disable;
static int video_disable;
static int subtitle_disable;
static int wanted_stream[AVMEDIA_TYPE_NB] = {-1, -1, -1, -1, -1};
static int seek_by_bytes = -1;
static int display_disable;
static int show_status = 1;
static int av_sync_type = AV_SYNC_AUDIO_MASTER;
static int64_t start_time = AV_NOPTS_VALUE;
static int64_t duration = AV_NOPTS_VALUE;
static int workaround_bugs = 1;
static int fast = 0;
static int genpts = 0;
static int lowres = 0;
static int decoder_reorder_pts = -1;
static int autoexit;
static int loop = 1;
static int framedrop = -1;
static int infinite_buffer = -1;
static enum ShowMode show_mode = SHOW_MODE_NONE;
static const char *audio_codec_name;
static const char *subtitle_codec_name;
static const char *video_codec_name;
double rdftspeed = 0.02;
static int64_t cursor_last_shown;
static int cursor_hidden = 0;
static int autorotate = 1;
static int64_t audio_callback_time;
static AVPacket flush_pkt;
static int seek_pos_on_start = 0;

///////////////////////////////////////////////////////////////////////////////

static double get_master_clock(VideoState *is);
static int event_loop_thread(void *arg);

///////////////////////////////////////////////////////////////////////////////
// functions

static void reset_global_variables()
{
	current_stream = NULL;
	seek_pos_on_start = 0;
	min_buffer_frames = 1;

	window = NULL;
	renderer = NULL;
	texture = NULL;
}

static inline int cmp_audio_fmts(enum AVSampleFormat fmt1, int64_t channel_count1,
	enum AVSampleFormat fmt2, int64_t channel_count2)
{
	// If channel count == 1, planar and non-planar formats are the same
	if (channel_count1 == 1 && channel_count2 == 1)
		return av_get_packed_sample_fmt(fmt1) != av_get_packed_sample_fmt(fmt2);
	else
		return channel_count1 != channel_count2 || fmt1 != fmt2;
}

static inline int64_t get_valid_channel_layout(int64_t channel_layout, int channels)
{
	if (channel_layout && av_get_channel_layout_nb_channels(channel_layout) == channels)
		return channel_layout;
	else
		return 0;
}

static void send_event(int event_type, void *data1, void *data2)
{
	SDL_Event event;
	event.type = event_type;
	event.user.data1 = data1;
	event.user.data2 = data2;
	SDL_PushEvent(&event);
}

static int packet_queue_put_private(PacketQueue *q, AVPacket *pkt)
{
	MyAVPacketList *pkt1;

	if (q->abort_request)
	   return -1;

	pkt1 = (MyAVPacketList*)av_malloc(sizeof(MyAVPacketList));
	if (!pkt1)
		return -1;
	pkt1->pkt = *pkt;
	pkt1->next = NULL;
	if (pkt == &flush_pkt)
		q->serial++;
	pkt1->serial = q->serial;

	if (!q->last_pkt)
		q->first_pkt = pkt1;
	else
		q->last_pkt->next = pkt1;
	q->last_pkt = pkt1;
	q->nb_packets++;
	q->size += pkt1->pkt.size + sizeof(*pkt1);
	// XXX: should duplicate packet data in DV case
	SDL_CondSignal(q->cond);
	return 0;
}

static int packet_queue_put(PacketQueue *q, AVPacket *pkt)
{
	int ret;

	// duplicate the packet
	if (pkt != &flush_pkt && av_dup_packet(pkt) < 0)
		return -1;

	SDL_LockMutex(q->mutex);
	ret = packet_queue_put_private(q, pkt);
	SDL_UnlockMutex(q->mutex);

	if (pkt != &flush_pkt && ret < 0)
		av_free_packet(pkt);

	return ret;
}

static int packet_queue_put_nullpacket(PacketQueue *q, int stream_index)
{
	AVPacket pkt1, *pkt = &pkt1;
	av_init_packet(pkt);
	pkt->data = NULL;
	pkt->size = 0;
	pkt->stream_index = stream_index;
	return packet_queue_put(q, pkt);
}

// packet queue handling
static void packet_queue_init(PacketQueue *q)
{
	memset(q, 0, sizeof(PacketQueue));
	q->mutex = SDL_CreateMutex();
	q->cond = SDL_CreateCond();
	q->abort_request = true;
}

static void packet_queue_flush(PacketQueue *q)
{
	MyAVPacketList *pkt, *pkt1;

	SDL_LockMutex(q->mutex);
	for (pkt = q->first_pkt; pkt; pkt = pkt1)
	{
		pkt1 = pkt->next;
		av_free_packet(&pkt->pkt);
		av_freep(&pkt);
	}
	q->last_pkt = NULL;
	q->first_pkt = NULL;
	q->nb_packets = 0;
	q->size = 0;
	SDL_UnlockMutex(q->mutex);
}

static void packet_queue_destroy(PacketQueue *q)
{
	packet_queue_flush(q);
	SDL_DestroyMutex(q->mutex);
	SDL_DestroyCond(q->cond);
}

static void packet_queue_abort(PacketQueue *q)
{
	SDL_LockMutex(q->mutex);

	q->abort_request = true;

	SDL_CondSignal(q->cond);

	SDL_UnlockMutex(q->mutex);
}

static void packet_queue_start(PacketQueue *q)
{
	SDL_LockMutex(q->mutex);
	q->abort_request = false;
	packet_queue_put_private(q, &flush_pkt);
	SDL_UnlockMutex(q->mutex);
}

// return < 0 if aborted, 0 if no packet and > 0 if packet.
static int packet_queue_get(PacketQueue *q, AVPacket *pkt, int block, int *serial)
{
	MyAVPacketList *pkt1;
	int ret;

	SDL_LockMutex(q->mutex);

	for (;;)
	{
		if (q->abort_request)
		{
			ret = -1;
			break;
		}

		pkt1 = q->first_pkt;
		if (pkt1)
		{
			q->first_pkt = pkt1->next;
			if (!q->first_pkt)
				q->last_pkt = NULL;
			q->nb_packets--;
			q->size -= pkt1->pkt.size + sizeof(*pkt1);
			*pkt = pkt1->pkt;
			if (serial)
				*serial = pkt1->serial;
			av_free(pkt1);
			ret = 1;
			break;
		}
		else if (!block)
		{
			ret = 0;
			break;
		}
		else
		{
			SDL_CondWait(q->cond, q->mutex);
		}
	}
	SDL_UnlockMutex(q->mutex);
	return ret;
}

#define RGBA_IN(r, g, b, a, s)\
{\
	unsigned int v = ((const uint32_t *)(s))[0];\
	a = (v >> 24) & 0xff;\
	r = (v >> 16) & 0xff;\
	g = (v >> 8) & 0xff;\
	b = v & 0xff;\
}

#define YUVA_OUT(d, y, u, v, a)\
{\
	((uint32_t *)(d))[0] = (a << 24) | (y << 16) | (u << 8) | v;\
}

static void free_picture(VideoPicture *vp)
{
	if (vp->av_frame)
	{
		avcodec_free_frame(&vp->av_frame);
		vp->av_frame = NULL;
	}

	if (vp->buffer)
	{
		av_free(vp->buffer);
		vp->buffer = NULL;
	}
}

static void free_subpicture(SubPicture *sp)
{
	avsubtitle_free(&sp->sub);
}

static void calculate_display_rect(SDL_Rect *rect,
	int scr_xleft, int scr_ytop, int scr_width, int scr_height,
	int pic_width, int pic_height, AVRational pic_sar)
{
	float aspect_ratio;
	int width, height, x, y;

	if (pic_sar.num == 0)
		aspect_ratio = 0;
	else
		aspect_ratio = av_q2d(pic_sar);

	if (aspect_ratio <= 0.0)
		aspect_ratio = 1.0;
	aspect_ratio *= (float)pic_width / (float)pic_height;

	// XXX: we suppose the screen has a 1.0 pixel ratio
	height = scr_height;
	width = ((int)rint(height * aspect_ratio)) & ~1;
	if (width > scr_width)
	{
		width = scr_width;
		height = ((int)rint(width / aspect_ratio)) & ~1;
	}
	x = (scr_width - width) / 2;
	y = (scr_height - height) / 2;
	rect->x = scr_xleft + x;
	rect->y = scr_ytop  + y;
	rect->w = FFMAX(width,  1);
	rect->h = FFMAX(height, 1);
}

static void video_image_display(VideoState *is)
{
	VideoPicture *vp;
	SubPicture *sp;
	AVPicture pict;
	SDL_Rect rect;

	vp = &is->pictq[(is->pictq_rindex + is->pictq_rindex_shown) % VIDEO_PICTURE_QUEUE_SIZE];
	if (vp->av_frame)
	{
		calculate_display_rect(&rect, is->xleft, is->ytop, is->width, is->height, vp->width, vp->height, vp->sar);

		if (NULL == texture)
		{
			LOGD("ceating texture.");
			texture = SDL_CreateTexture(renderer, display_fix_fmt,
				SDL_TEXTUREACCESS_STATIC, is->video_st->codec->width,
				is->video_st->codec->height);
			if (!texture)
			{
				LOGD( "Couldn't set create texture: %s\n", SDL_GetError());
				return;
			}
			SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_BLEND);
		}

		SDL_RenderClear(renderer);

		SDL_UpdateYUVTexture(texture, NULL,
			vp->av_frame->data[0], vp->av_frame->linesize[0],
			vp->av_frame->data[1], vp->av_frame->linesize[1],
			vp->av_frame->data[2], vp->av_frame->linesize[2]);

		SDL_RenderCopy(renderer, texture, NULL, &rect);

		SDL_RenderPresent(renderer);

		is->current_pos = (int)get_master_clock(is) * 1000;
	}
}

static inline int compute_mod(int a, int b)
{
	return a < 0 ? a%b + b : a%b;
}

static void stream_close(VideoState *is)
{
	int i;

	is->abort_request = true;
	SDL_DetachThread(is->event_loop_tid);
	SDL_WaitThread(is->read_tid, NULL);

	packet_queue_destroy(&is->videoq);
	packet_queue_destroy(&is->audioq);
	packet_queue_destroy(&is->subtitleq);

	// free all pictures
	for (i = 0; i < VIDEO_PICTURE_QUEUE_SIZE; i++)
		free_picture(&is->pictq[i]);
	for (i = 0; i < SUBPICTURE_QUEUE_SIZE; i++)
		free_subpicture(&is->subpq[i]);

	SDL_DestroyMutex(is->pictq_mutex);
	SDL_DestroyCond(is->pictq_cond);
	SDL_DestroyMutex(is->subpq_mutex);
	SDL_DestroyCond(is->subpq_cond);
	SDL_DestroyCond(is->continue_read_thread);
	sws_freeContext(is->img_convert_ctx);
	av_free(is);
}

static void do_exit(VideoState *is)
{
	LOGD("player exiting...");

	if (is)
		stream_close(is);

	av_lockmgr_register(NULL);
	avformat_network_deinit();

	SDL_Quit();
	reset_global_variables();

	LOGD("player exit.");
}

static void sigterm_handler(int sig)
{
}

static int video_open(VideoState *is, VideoPicture *vp)
{
	int w = 0, h = 0;

	if (!window)
	{
		LOGD("creating window.");
		window = SDL_CreateWindow("player", 0, 0, 0, 0, SDL_WINDOW_OPENGL);

		if (window == NULL)
		{
			LOGD("fail to create window.");
			return -1;
		}
	}

	if (!renderer)
	{
		LOGD("creating renderer.");
		renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);

		if (renderer == NULL)
		{
			LOGD("fail to create renderer.");
			return -1;
		}
	}

	if (window)
	{
		// get the actual size of the window. 
		// for android, the window size is always set to Android_ScreenWidth and Android_ScreenHeight
		// in Android_CreateWindow().
		
		SDL_GetWindowSize(window, &w, &h);

		is->width = w;
		is->height = h;
	}

	LOGD("video_open: w=%d, h=%d", w, h);

	return 0;
}

// display the current picture, if any
static void video_display(VideoState *is)
{
	if (!window)
		video_open(is, NULL);

	if (is->video_st)
		video_image_display(is);
}

static double get_clock(Clock *c)
{
	if (*c->queue_serial != c->serial)
		return NAN;
	if (c->paused)
	{
		return c->pts;
	}
	else
	{
		double time = av_gettime_relative() / 1000000.0;
		return c->pts_drift + time - (time - c->last_updated) * (1.0 - c->speed);
	}
}

static void set_clock_at(Clock *c, double pts, int serial, double time)
{
	c->pts = pts;
	c->last_updated = time;
	c->pts_drift = c->pts - time;
	c->serial = serial;
}

static void set_clock(Clock *c, double pts, int serial)
{
	double time = av_gettime_relative() / 1000000.0;
	set_clock_at(c, pts, serial, time);
}

static void set_clock_speed(Clock *c, double speed)
{
	set_clock(c, get_clock(c), c->serial);
	c->speed = speed;
}

static void init_clock(Clock *c, int *queue_serial)
{
	c->speed = 1.0;
	c->paused = 0;
	c->queue_serial = queue_serial;
	set_clock(c, NAN, -1);
}

static void sync_clock_to_slave(Clock *c, Clock *slave)
{
	double clock = get_clock(c);
	double slave_clock = get_clock(slave);
	if (!isnan(slave_clock) && (isnan(clock) || fabs(clock - slave_clock) > AV_NOSYNC_THRESHOLD))
		set_clock(c, slave_clock, slave->serial);
}

static int get_master_sync_type(VideoState *is)
{
	if (is->av_sync_type == AV_SYNC_VIDEO_MASTER)
	{
		if (is->video_st)
			return AV_SYNC_VIDEO_MASTER;
		else
			return AV_SYNC_AUDIO_MASTER;
	}
	else if (is->av_sync_type == AV_SYNC_AUDIO_MASTER)
	{
		if (is->audio_st)
			return AV_SYNC_AUDIO_MASTER;
		else
			return AV_SYNC_EXTERNAL_CLOCK;
	}
	else
	{
		return AV_SYNC_EXTERNAL_CLOCK;
	}
}

// get the current master clock value
static double get_master_clock(VideoState *is)
{
	double val;

	switch (get_master_sync_type(is))
	{
	case AV_SYNC_VIDEO_MASTER:
		val = get_clock(&is->vidclk);
		break;
	case AV_SYNC_AUDIO_MASTER:
		val = get_clock(&is->audclk);
		break;
	default:
		val = get_clock(&is->extclk);
		break;
	}

	return val;
}

static void check_external_clock_speed(VideoState *is)
{
	if (is->video_stream >= 0 && is->videoq.nb_packets <= min_buffer_frames / 2 ||
		is->audio_stream >= 0 && is->audioq.nb_packets <= min_buffer_frames / 2)
	{
		set_clock_speed(&is->extclk, FFMAX(EXTERNAL_CLOCK_SPEED_MIN, is->extclk.speed - EXTERNAL_CLOCK_SPEED_STEP));
	}
	else if ((is->video_stream < 0 || is->videoq.nb_packets > min_buffer_frames * 2) &&
		(is->audio_stream < 0 || is->audioq.nb_packets > min_buffer_frames * 2))
	{
		set_clock_speed(&is->extclk, FFMIN(EXTERNAL_CLOCK_SPEED_MAX, is->extclk.speed + EXTERNAL_CLOCK_SPEED_STEP));
	}
	else
	{
		double speed = is->extclk.speed;
		if (speed != 1.0)
		set_clock_speed(&is->extclk, speed + EXTERNAL_CLOCK_SPEED_STEP * (1.0 - speed) / fabs(1.0 - speed));
	}
}

// seek in the stream
static void stream_seek(VideoState *is, int64_t pos, int64_t rel, bool seek_any_frame)
{
	if (!is->seek_req)
	{
		is->seek_pos = pos;
		is->seek_rel = rel;
		is->seek_flags |= AVSEEK_FLAG_BACKWARD;
		is->seek_flags &= ~AVSEEK_FLAG_BYTE;
		if (seek_any_frame)
			is->seek_flags |= AVSEEK_FLAG_ANY;
		else
			is->seek_flags &= ~AVSEEK_FLAG_ANY;
		is->seek_req = 1;
		SDL_CondSignal(is->continue_read_thread);
	}
}

// pause or resume the video
static void stream_toggle_pause(VideoState *is)
{
	LOGD("toggle pause.");

	if (is->paused)
	{
		is->frame_timer += av_gettime_relative() / 1000000.0 + is->vidclk.pts_drift - is->vidclk.pts;
		if (is->read_pause_return != AVERROR(ENOSYS))
			is->vidclk.paused = 0;
		set_clock(&is->vidclk, get_clock(&is->vidclk), is->vidclk.serial);
	}
	set_clock(&is->extclk, get_clock(&is->extclk), is->extclk.serial);
	is->paused = is->audclk.paused = is->vidclk.paused = is->extclk.paused = !is->paused;
}

static void toggle_pause(VideoState *is)
{
	stream_toggle_pause(is);
	is->step = 0;
}

static void step_to_next_frame(VideoState *is)
{
	LOGD("step_to_next_frame.");
	// if the stream is paused unpause it, then step
	if (is->paused)
		stream_toggle_pause(is);
	is->step = 1;
}

static double compute_target_delay(double delay, VideoState *is)
{
	double sync_threshold, diff;

	// update delay to follow master synchronisation source
	if (get_master_sync_type(is) != AV_SYNC_VIDEO_MASTER)
	{
		// if video is slave, we try to correct big delays by duplicating or deleting a frame
		diff = get_clock(&is->vidclk) - get_master_clock(is);

		// skip or repeat frame. We take into account the delay to compute the threshold. I still don't know if it is the best guess
		sync_threshold = FFMAX(AV_SYNC_THRESHOLD_MIN, FFMIN(AV_SYNC_THRESHOLD_MAX, delay));
		if (!isnan(diff) && fabs(diff) < is->max_frame_duration)
		{
			if (diff <= -sync_threshold)
				delay = FFMAX(0, delay + diff);
			else if (diff >= sync_threshold && delay > AV_SYNC_FRAMEDUP_THRESHOLD)
				delay = delay + diff;
			else if (diff >= sync_threshold)
				delay = 2 * delay;
		}
	}

	av_dlog(NULL, "video: delay=%0.3f A-V=%f\n", delay, -diff);

	return delay;
}

static double vp_duration(VideoState *is, VideoPicture *vp, VideoPicture *nextvp)
{
	if (vp->serial == nextvp->serial)
	{
		double duration = nextvp->pts - vp->pts;
		if (isnan(duration) || duration <= 0 || duration > is->max_frame_duration)
			return vp->duration;
		else
			return duration;
	}
	else
	{
		return 0.0;
	}
}

// return the number of undisplayed pictures in the queue
static int pictq_nb_remaining(VideoState *is)
{
	return is->pictq_size - is->pictq_rindex_shown;
}

// jump back to the previous picture if available by resetting rindex_shown
static int pictq_prev_picture(VideoState *is)
{
	int ret = is->pictq_rindex_shown;
	is->pictq_rindex_shown = 0;
	return ret;
}

static void pictq_next_picture(VideoState *is)
{
	if (!is->pictq_rindex_shown)
	{
		is->pictq_rindex_shown = 1;
		return;
	}
	// update queue size and signal for next picture
	if (++is->pictq_rindex == VIDEO_PICTURE_QUEUE_SIZE)
		is->pictq_rindex = 0;

	SDL_LockMutex(is->pictq_mutex);
	is->pictq_size--;
	SDL_CondSignal(is->pictq_cond);
	SDL_UnlockMutex(is->pictq_mutex);
}

static void update_video_pts(VideoState *is, double pts, int64_t pos, int serial)
{
	// update current video pts
	set_clock(&is->vidclk, pts, serial);
	sync_clock_to_slave(&is->extclk, &is->vidclk);
	is->video_current_pos = pos;
}

// called to display each frame
static void video_refresh(void *opaque, double *remaining_time)
{
	VideoState *is = (VideoState*)opaque;
	double time;

	SubPicture *sp, *sp2;

	if (!is->paused && get_master_sync_type(is) == AV_SYNC_EXTERNAL_CLOCK && is->realtime)
		check_external_clock_speed(is);

	if (!display_disable && is->show_mode != SHOW_MODE_VIDEO && is->audio_st)
	{
		time = av_gettime_relative() / 1000000.0;
		if (is->force_refresh || is->last_vis_time + rdftspeed < time)
		{
			video_display(is);
			is->last_vis_time = time;
		}

		if (remaining_time != NULL)
			*remaining_time = FFMIN(*remaining_time, is->last_vis_time + rdftspeed - time);
	}

	if (is->video_st)
	{
		int redisplay = 0;
		if (is->force_refresh)
			redisplay = pictq_prev_picture(is);
retry:
		if (pictq_nb_remaining(is) == 0)
		{
			// nothing to do, no picture to display in the queue
		}
		else
		{
			double last_duration, duration, delay;
			VideoPicture *vp, *lastvp;

			// dequeue the picture
			lastvp = &is->pictq[is->pictq_rindex];
			vp = &is->pictq[(is->pictq_rindex + is->pictq_rindex_shown) % VIDEO_PICTURE_QUEUE_SIZE];

			if (vp->serial != is->videoq.serial)
			{
				pictq_next_picture(is);
				is->video_current_pos = -1;
				redisplay = 0;
				goto retry;
			}

			if (lastvp->serial != vp->serial && !redisplay)
				is->frame_timer = av_gettime_relative() / 1000000.0;

			if (is->paused)
				goto display;

			// compute nominal last_duration
			last_duration = vp_duration(is, lastvp, vp);
			if (redisplay)
				delay = 0.0;
			else
				delay = compute_target_delay(last_duration, is);

			time = av_gettime_relative()/1000000.0;
			if (time < is->frame_timer + delay && !redisplay)
			{
				if (remaining_time != NULL)
					*remaining_time = FFMIN(is->frame_timer + delay - time, *remaining_time);
				return;
			}

			is->frame_timer += delay;
			if (delay > 0 && time - is->frame_timer > AV_SYNC_THRESHOLD_MAX)
				is->frame_timer = time;

			SDL_LockMutex(is->pictq_mutex);
			if (!redisplay && !isnan(vp->pts))
				update_video_pts(is, vp->pts, vp->pos, vp->serial);
			SDL_UnlockMutex(is->pictq_mutex);

			if (pictq_nb_remaining(is) > 1)
			{
				VideoPicture *nextvp = &is->pictq[(is->pictq_rindex + is->pictq_rindex_shown + 1) % VIDEO_PICTURE_QUEUE_SIZE];
				duration = vp_duration(is, vp, nextvp);
				if (!is->step && (redisplay || framedrop>0 || (framedrop && get_master_sync_type(is) != AV_SYNC_VIDEO_MASTER)) && time > is->frame_timer + duration) {
					if (!redisplay)
						is->frame_drops_late++;
					pictq_next_picture(is);
					redisplay = 0;
					goto retry;
				}
			}

			if (is->subtitle_st)
			{
				while (is->subpq_size > 0)
				{
					sp = &is->subpq[is->subpq_rindex];

					if (is->subpq_size > 1)
						sp2 = &is->subpq[(is->subpq_rindex + 1) % SUBPICTURE_QUEUE_SIZE];
					else
						sp2 = NULL;

					if (sp->serial != is->subtitleq.serial
						|| (is->vidclk.pts > (sp->pts + ((float) sp->sub.end_display_time / 1000)))
						|| (sp2 && is->vidclk.pts > (sp2->pts + ((float) sp2->sub.start_display_time / 1000))))
					{
						free_subpicture(sp);

						// update queue size and signal for next picture
						if (++is->subpq_rindex == SUBPICTURE_QUEUE_SIZE)
							is->subpq_rindex = 0;

						SDL_LockMutex(is->subpq_mutex);
						is->subpq_size--;
						SDL_CondSignal(is->subpq_cond);
						SDL_UnlockMutex(is->subpq_mutex);
					}
					else
					{
						break;
					}
				}
			}

display:

			// display picture
			if (!display_disable && is->show_mode == SHOW_MODE_VIDEO)
				video_display(is);

			pictq_next_picture(is);

			if (is->step && !is->paused)
			{
				LOGD("toggle pause for step.");
				stream_toggle_pause(is);
			}
		}
	}
	is->force_refresh = false;
	if (show_status)
	{
		static int64_t last_time;
		int64_t cur_time;
		int aqsize, vqsize, sqsize;
		double av_diff;

		cur_time = av_gettime_relative();
		if (!last_time || (cur_time - last_time) >= 30000)
		{
			aqsize = 0;
			vqsize = 0;
			sqsize = 0;
			if (is->audio_st)
				aqsize = is->audioq.size;
			if (is->video_st)
				vqsize = is->videoq.size;
			if (is->subtitle_st)
				sqsize = is->subtitleq.size;
			av_diff = 0;
			if (is->audio_st && is->video_st)
				av_diff = get_clock(&is->audclk) - get_clock(&is->vidclk);
			else if (is->video_st)
				av_diff = get_master_clock(is) - get_clock(&is->vidclk);
			else if (is->audio_st)
				av_diff = get_master_clock(is) - get_clock(&is->audclk);
			fflush(stdout);
			last_time = cur_time;
		}
	}
}

// allocate a picture (needs to do that in main thread to avoid potential locking problems
static void alloc_picture(VideoState *is)
{
	VideoPicture *vp;
	int64_t bufferdiff;

	vp = &is->pictq[is->pictq_windex];

	free_picture(vp);

	video_open(is, vp);

	vp->av_frame = avcodec_alloc_frame();

	vp->width = is->video_st->codec->width;
	vp->height = is->video_st->codec->height;

	vp->byte_count = avpicture_get_size(dst_fix_fmt, vp->width, vp->height);
	vp->buffer = (uint8_t*)av_malloc(vp->byte_count * sizeof(uint8_t));

	if (!vp->av_frame || !vp->buffer)
	{
		LOGD("can not get frame memory.");
		return;
	}

	avpicture_fill((AVPicture*)vp->av_frame, vp->buffer, dst_fix_fmt, vp->width, vp->height);

	SDL_LockMutex(is->pictq_mutex);
	vp->allocated = 1;
	SDL_CondSignal(is->pictq_cond);
	SDL_UnlockMutex(is->pictq_mutex);
}

static int queue_picture(VideoState *is, AVFrame *src_frame, double pts, double duration, int64_t pos, int serial)
{
	VideoPicture *vp;

	// wait until we have space to put a new picture
	SDL_LockMutex(is->pictq_mutex);

	while (is->pictq_size >= VIDEO_PICTURE_QUEUE_SIZE && !is->videoq.abort_request)
		SDL_CondWait(is->pictq_cond, is->pictq_mutex);
	SDL_UnlockMutex(is->pictq_mutex);

	if (is->videoq.abort_request)
		return -1;

	vp = &is->pictq[is->pictq_windex];

	vp->sar = src_frame->sample_aspect_ratio;

	// alloc or resize hardware picture buffer
	if (!vp->av_frame || vp->reallocate || !vp->allocated ||
		vp->width  != src_frame->width ||
		vp->height != src_frame->height)
	{
		vp->allocated  = 0;
		vp->reallocate = 0;
		vp->width = src_frame->width;
		vp->height = src_frame->height;

		// the allocation must be done in the main thread to avoid locking problems.
		send_event(PLAYER_ALLOC_EVENT, is, NULL);

		// wait until the picture is allocated
		SDL_LockMutex(is->pictq_mutex);
		while (!vp->allocated && !is->videoq.abort_request)
		{
			SDL_CondWait(is->pictq_cond, is->pictq_mutex);
		}
		// if the queue is aborted, we have to pop the pending ALLOC event or wait for the allocation to complete
		SDL_Event event;
		if (is->videoq.abort_request && SDL_PeepEvents(&event, 1, SDL_GETEVENT, PLAYER_ALLOC_EVENT, PLAYER_ALLOC_EVENT) != 1)
		{
			while (!vp->allocated && !is->abort_request)
				SDL_CondWait(is->pictq_cond, is->pictq_mutex);
		}
		SDL_UnlockMutex(is->pictq_mutex);

		if (is->videoq.abort_request)
			return -1;
	}

	// if the frame is not skipped, then display it
	if (vp->av_frame)
	{
		av_picture_copy((AVPicture*)vp->av_frame, (AVPicture*)src_frame, (AVPixelFormat)src_frame->format, vp->width, vp->height);

		vp->pts = pts;
		vp->duration = duration;
		vp->pos = pos;
		vp->serial = serial;

		// now we can update the picture count
		if (++is->pictq_windex == VIDEO_PICTURE_QUEUE_SIZE)
			is->pictq_windex = 0;
		SDL_LockMutex(is->pictq_mutex);
		is->pictq_size++;
		SDL_UnlockMutex(is->pictq_mutex);
	}

	return 0;
}

static int get_video_frame(VideoState *is, AVFrame *frame, AVPacket *pkt, int *serial)
{
	int got_picture;

	if (packet_queue_get(&is->videoq, pkt, 1, serial) < 0)
		return -1;

	if (pkt->data == flush_pkt.data)
	{
		avcodec_flush_buffers(is->video_st->codec);
		return 0;
	}

	if (avcodec_decode_video2(is->video_st->codec, frame, &got_picture, pkt) < 0)
		return 0;

	if (!got_picture && !pkt->data)
		is->video_finished = *serial;

	if (got_picture)
	{
		int ret = 1;
		double dpts = NAN;

		if (decoder_reorder_pts == -1)
			frame->pts = av_frame_get_best_effort_timestamp(frame);
		else if (decoder_reorder_pts)
			frame->pts = frame->pkt_pts;
		else
			frame->pts = frame->pkt_dts;

		if (frame->pts != AV_NOPTS_VALUE)
			dpts = av_q2d(is->video_st->time_base) * frame->pts;

		frame->sample_aspect_ratio = av_guess_sample_aspect_ratio(is->ic, is->video_st, frame);

		if (framedrop>0 || (framedrop && get_master_sync_type(is) != AV_SYNC_VIDEO_MASTER))
		{
			if (frame->pts != AV_NOPTS_VALUE)
			{
				double diff = dpts - get_master_clock(is);
				if (!isnan(diff) && fabs(diff) < AV_NOSYNC_THRESHOLD &&
					diff - is->frame_last_filter_delay < 0 &&
					*serial == is->vidclk.serial &&
					is->videoq.nb_packets)
				{
					is->frame_drops_early++;
					av_frame_unref(frame);
					ret = 0;
				}
			}
		}

		return ret;
	}

	return 0;
}

static int video_thread(void *arg)
{
	AVPacket pkt = { 0 };
	VideoState *is = (VideoState*)arg;
	AVFrame *frame = av_frame_alloc();
	double pts;
	double duration;
	int ret;
	int serial = 0;
	AVRational tb = is->video_st->time_base;
	AVRational frame_rate = av_guess_frame_rate(is->ic, is->video_st, NULL);

	for (;;)
	{
		while (is->paused && !is->videoq.abort_request)
			SDL_Delay(10);

		av_free_packet(&pkt);

		ret = get_video_frame(is, frame, &pkt, &serial);
		if (ret < 0)
			goto the_end;
		if (!ret)
			continue;

		duration = (frame_rate.num && frame_rate.den ? av_q2d((AVRational){frame_rate.den, frame_rate.num}) : 0);
		pts = (frame->pts == AV_NOPTS_VALUE) ? NAN : frame->pts * av_q2d(tb);
		ret = queue_picture(is, frame, pts, duration, av_frame_get_pkt_pos(frame), serial);
		av_frame_unref(frame);

		if (ret < 0)
			goto the_end;
	}

the_end:
	av_free_packet(&pkt);
	av_frame_free(&frame);
	return 0;
}

static int subtitle_thread(void *arg)
{
	VideoState *is = (VideoState*)arg;
	SubPicture *sp;
	AVPacket pkt1, *pkt = &pkt1;
	int got_subtitle;
	int serial;
	double pts;
	int i, j;
	int r, g, b, y, u, v, a;

	for (;;)
	{
		while (is->paused && !is->subtitleq.abort_request)
			SDL_Delay(10);

		if (packet_queue_get(&is->subtitleq, pkt, 1, &serial) < 0)
			break;

		if (pkt->data == flush_pkt.data)
		{
			avcodec_flush_buffers(is->subtitle_st->codec);
			continue;
		}

		SDL_LockMutex(is->subpq_mutex);
		while (is->subpq_size >= SUBPICTURE_QUEUE_SIZE &&
		   !is->subtitleq.abort_request)
		{
			SDL_CondWait(is->subpq_cond, is->subpq_mutex);
		}

		SDL_UnlockMutex(is->subpq_mutex);

		if (is->subtitleq.abort_request)
			return 0;

		sp = &is->subpq[is->subpq_windex];

		// NOTE: ipts is the PTS of the _first_ picture beginning in this packet, if any
		pts = 0;
		if (pkt->pts != AV_NOPTS_VALUE)
			pts = av_q2d(is->subtitle_st->time_base) * pkt->pts;

		avcodec_decode_subtitle2(is->subtitle_st->codec, &sp->sub, &got_subtitle, pkt);
		if (got_subtitle && sp->sub.format == 0)
		{
			if (sp->sub.pts != AV_NOPTS_VALUE)
				pts = sp->sub.pts / (double)AV_TIME_BASE;
			sp->pts = pts;
			sp->serial = serial;

			for (i = 0; i < sp->sub.num_rects; i++)
			{
				for (j = 0; j < sp->sub.rects[i]->nb_colors; j++)
				{
					RGBA_IN(r, g, b, a, (uint32_t*)sp->sub.rects[i]->pict.data[1] + j);
					y = RGB_TO_Y_CCIR(r, g, b);
					u = RGB_TO_U_CCIR(r, g, b, 0);
					v = RGB_TO_V_CCIR(r, g, b, 0);
					YUVA_OUT((uint32_t*)sp->sub.rects[i]->pict.data[1] + j, y, u, v, a);
				}
			}

			// now we can update the picture count
			if (++is->subpq_windex == SUBPICTURE_QUEUE_SIZE)
				is->subpq_windex = 0;
			SDL_LockMutex(is->subpq_mutex);
			is->subpq_size++;
			SDL_UnlockMutex(is->subpq_mutex);
		}
		else if (got_subtitle)
		{
			avsubtitle_free(&sp->sub);
		}
		av_free_packet(pkt);
	}

	return 0;
}

// copy samples for viewing in editor window
static void update_sample_display(VideoState *is, short *samples, int samples_size)
{
	int size, len;

	size = samples_size / sizeof(short);
	while (size > 0)
	{
		len = SAMPLE_ARRAY_SIZE - is->sample_array_index;
		if (len > size)
			len = size;
		memcpy(is->sample_array + is->sample_array_index, samples, len * sizeof(short));
		samples += len;
		is->sample_array_index += len;
		if (is->sample_array_index >= SAMPLE_ARRAY_SIZE)
			is->sample_array_index = 0;
		size -= len;
	}
}

// return the wanted number of samples to get better sync if sync_type is video or external master clock
static int synchronize_audio(VideoState *is, int nb_samples)
{
	int wanted_nb_samples = nb_samples;

	// if not master, then we try to remove or add samples to correct the clock
	if (get_master_sync_type(is) != AV_SYNC_AUDIO_MASTER)
	{
		double diff, avg_diff;
		int min_nb_samples, max_nb_samples;

		diff = get_clock(&is->audclk) - get_master_clock(is);

		if (!isnan(diff) && fabs(diff) < AV_NOSYNC_THRESHOLD)
		{
			is->audio_diff_cum = diff + is->audio_diff_avg_coef * is->audio_diff_cum;
			if (is->audio_diff_avg_count < AUDIO_DIFF_AVG_NB)
			{
				// not enough measures to have a correct estimate
				is->audio_diff_avg_count++;
			}
			else
			{
				// estimate the A-V difference
				avg_diff = is->audio_diff_cum * (1.0 - is->audio_diff_avg_coef);

				if (fabs(avg_diff) >= is->audio_diff_threshold)
				{
					wanted_nb_samples = nb_samples + (int)(diff * is->audio_src.freq);
					min_nb_samples = ((nb_samples * (100 - SAMPLE_CORRECTION_PERCENT_MAX) / 100));
					max_nb_samples = ((nb_samples * (100 + SAMPLE_CORRECTION_PERCENT_MAX) / 100));
					wanted_nb_samples = FFMIN(FFMAX(wanted_nb_samples, min_nb_samples), max_nb_samples);
				}

				av_dlog(NULL, "diff=%f adiff=%f sample_diff=%d apts=%0.3f %f\n",
					diff, avg_diff, wanted_nb_samples - nb_samples,
					is->audio_clock, is->audio_diff_threshold);
			}
		}
		else
		{
			// too big difference : may be initial PTS errors, so reset A-V filter
			is->audio_diff_avg_count = 0;
			is->audio_diff_cum       = 0;
		}
	}

	return wanted_nb_samples;
}

/**
 * Decode one audio frame and return its uncompressed size.
 *
 * The processed audio frame is decoded, converted if required, and
 * stored in is->audio_buf, with size in bytes given by the return
 * value.
 */
static int audio_decode_frame(VideoState *is)
{
	AVPacket *pkt_temp = &is->audio_pkt_temp;
	AVPacket *pkt = &is->audio_pkt;
	AVCodecContext *dec = is->audio_st->codec;
	int len1, data_size, resampled_data_size;
	int64_t dec_channel_layout;
	int got_frame;
	av_unused double audio_clock0;
	int wanted_nb_samples;
	AVRational tb;
	int ret;
	int reconfigure;

	for (;;)
	{
		// NOTE: the audio packet can contain several frames
		while (pkt_temp->stream_index != -1 || is->audio_buf_frames_pending)
		{
			if (!is->frame)
			{
				if (!(is->frame = av_frame_alloc()))
					return AVERROR(ENOMEM);
			}
			else
			{
				av_frame_unref(is->frame);
			}

			if (is->audioq.serial != is->audio_pkt_temp_serial)
				break;

			if (is->paused)
				return -1;

			if (!is->audio_buf_frames_pending)
			{
				len1 = avcodec_decode_audio4(dec, is->frame, &got_frame, pkt_temp);
				if (len1 < 0)
				{
					// if error, we skip the frame
					pkt_temp->size = 0;
					break;
				}

				pkt_temp->dts =
				pkt_temp->pts = AV_NOPTS_VALUE;
				pkt_temp->data += len1;
				pkt_temp->size -= len1;

				if (pkt_temp->data && pkt_temp->size <= 0 || !pkt_temp->data && !got_frame)
					pkt_temp->stream_index = -1;
				if (!pkt_temp->data && !got_frame)
					is->audio_finished = is->audio_pkt_temp_serial;

				if (!got_frame)
					continue;

				tb = (AVRational){1, is->frame->sample_rate};
				if (is->frame->pts != AV_NOPTS_VALUE)
					is->frame->pts = av_rescale_q(is->frame->pts, dec->time_base, tb);
				else if (is->frame->pkt_pts != AV_NOPTS_VALUE)
					is->frame->pts = av_rescale_q(is->frame->pkt_pts, is->audio_st->time_base, tb);
				else if (is->audio_frame_next_pts != AV_NOPTS_VALUE)

				is->frame->pts = av_rescale_q(is->audio_frame_next_pts, (AVRational){1, is->audio_src.freq}, tb);

				if (is->frame->pts != AV_NOPTS_VALUE)
					is->audio_frame_next_pts = is->frame->pts + is->frame->nb_samples;
			}

			data_size = av_samples_get_buffer_size(NULL, av_frame_get_channels(is->frame),
				is->frame->nb_samples,
				(AVSampleFormat)is->frame->format, 1);

			dec_channel_layout =
				(is->frame->channel_layout && av_frame_get_channels(is->frame) == av_get_channel_layout_nb_channels(is->frame->channel_layout)) ?
				is->frame->channel_layout : av_get_default_channel_layout(av_frame_get_channels(is->frame));
			wanted_nb_samples = synchronize_audio(is, is->frame->nb_samples);

			if (is->frame->format        != is->audio_src.fmt            ||
				dec_channel_layout       != is->audio_src.channel_layout ||
				is->frame->sample_rate   != is->audio_src.freq           ||
				(wanted_nb_samples       != is->frame->nb_samples && !is->swr_ctx))
			{
				swr_free(&is->swr_ctx);
				is->swr_ctx = swr_alloc_set_opts(NULL,
					is->audio_tgt.channel_layout, is->audio_tgt.fmt, is->audio_tgt.freq,
					dec_channel_layout, (AVSampleFormat)is->frame->format, is->frame->sample_rate,
					0, NULL);
				if (!is->swr_ctx || swr_init(is->swr_ctx) < 0)
				{
					av_log(NULL, AV_LOG_ERROR,
						"Cannot create sample rate converter for conversion of %d Hz %s %d channels to %d Hz %s %d channels!\n",
						is->frame->sample_rate, av_get_sample_fmt_name((AVSampleFormat)is->frame->format), av_frame_get_channels(is->frame),
						is->audio_tgt.freq, av_get_sample_fmt_name(is->audio_tgt.fmt), is->audio_tgt.channels);
					break;
				}
				is->audio_src.channel_layout = dec_channel_layout;
				is->audio_src.channels = av_frame_get_channels(is->frame);
				is->audio_src.freq = is->frame->sample_rate;
				is->audio_src.fmt = (AVSampleFormat)is->frame->format;
			}

			if (is->swr_ctx)
			{
				const uint8_t **in = (const uint8_t **)is->frame->extended_data;
				uint8_t **out = &is->audio_buf1;
				int out_count = (int64_t)wanted_nb_samples * is->audio_tgt.freq / is->frame->sample_rate + 256;
				int out_size  = av_samples_get_buffer_size(NULL, is->audio_tgt.channels, out_count, is->audio_tgt.fmt, 0);
				int len2;
				if (out_size < 0)
				{
					av_log(NULL, AV_LOG_ERROR, "av_samples_get_buffer_size() failed\n");
					break;
				}
				if (wanted_nb_samples != is->frame->nb_samples)
				{
					if (swr_set_compensation(is->swr_ctx, (wanted_nb_samples - is->frame->nb_samples) * is->audio_tgt.freq / is->frame->sample_rate,
							wanted_nb_samples * is->audio_tgt.freq / is->frame->sample_rate) < 0)
					{
						av_log(NULL, AV_LOG_ERROR, "swr_set_compensation() failed\n");
						break;
					}
				}
				av_fast_malloc(&is->audio_buf1, &is->audio_buf1_size, out_size);
				if (!is->audio_buf1)
					return AVERROR(ENOMEM);
				len2 = swr_convert(is->swr_ctx, out, out_count, in, is->frame->nb_samples);
				if (len2 < 0)
				{
					av_log(NULL, AV_LOG_ERROR, "swr_convert() failed\n");
					break;
				}
				if (len2 == out_count)
				{
					av_log(NULL, AV_LOG_WARNING, "audio buffer is probably too small\n");
					swr_init(is->swr_ctx);
				}
				is->audio_buf = is->audio_buf1;
				resampled_data_size = len2 * is->audio_tgt.channels * av_get_bytes_per_sample(is->audio_tgt.fmt);
			}
			else
			{
				is->audio_buf = is->frame->data[0];
				resampled_data_size = data_size;
			}

			audio_clock0 = is->audio_clock;
			// update the audio clock with the pts
			if (is->frame->pts != AV_NOPTS_VALUE)
				is->audio_clock = is->frame->pts * av_q2d(tb) + (double) is->frame->nb_samples / is->frame->sample_rate;
			else
				is->audio_clock = NAN;
			is->audio_clock_serial = is->audio_pkt_temp_serial;
#ifdef DEBUG
			{
				static double last_clock;
				printf("audio: delay=%0.3f clock=%0.3f clock0=%0.3f\n",
					   is->audio_clock - last_clock,
					   is->audio_clock, audio_clock0);
				last_clock = is->audio_clock;
			}
#endif
			return resampled_data_size;
		}

		// free the current packet
		if (pkt->data)
			av_free_packet(pkt);
		memset(pkt_temp, 0, sizeof(*pkt_temp));
		pkt_temp->stream_index = -1;

		if (is->audioq.abort_request)
			return -1;

		if (is->audioq.nb_packets == 0)
			SDL_CondSignal(is->continue_read_thread);

		// read next packet
		if ((packet_queue_get(&is->audioq, pkt, 1, &is->audio_pkt_temp_serial)) < 0)
			return -1;

		if (pkt->data == flush_pkt.data)
		{
			avcodec_flush_buffers(dec);
			is->audio_buf_frames_pending = 0;
			is->audio_frame_next_pts = AV_NOPTS_VALUE;
			if ((is->ic->iformat->flags & (AVFMT_NOBINSEARCH | AVFMT_NOGENSEARCH | AVFMT_NO_BYTE_SEEK)) && !is->ic->iformat->read_seek)
				is->audio_frame_next_pts = is->audio_st->start_time;
		}

		*pkt_temp = *pkt;
	}
}

// prepare a new audio buffer
static void sdl_audio_callback(void *opaque, Uint8 *stream, int len)
{
	VideoState *is = (VideoState*)opaque;
	int audio_size, len1;

	audio_callback_time = av_gettime_relative();

	while (len > 0)
	{
		if (is->audio_buf_index >= is->audio_buf_size)
		{
			audio_size = audio_decode_frame(is);
			if (audio_size < 0)
			{
				// if error, just output silence
				is->audio_buf      = is->silence_buf;
				is->audio_buf_size = sizeof(is->silence_buf) / is->audio_tgt.frame_size * is->audio_tgt.frame_size;
			}
			else
			{
				if (is->show_mode != SHOW_MODE_VIDEO)
					update_sample_display(is, (int16_t *)is->audio_buf, audio_size);
				is->audio_buf_size = audio_size;
			}
		   is->audio_buf_index = 0;
		}

		len1 = is->audio_buf_size - is->audio_buf_index;
		if (len1 > len)
			len1 = len;
		memcpy(stream, (uint8_t *)is->audio_buf + is->audio_buf_index, len1);
		len -= len1;
		stream += len1;
		is->audio_buf_index += len1;
	}
	is->audio_write_buf_size = is->audio_buf_size - is->audio_buf_index;
	// Let's assume the audio driver that is used by SDL has two periods.
	if (!isnan(is->audio_clock))
	{
		set_clock_at(&is->audclk, is->audio_clock - (double)(2 * is->audio_hw_buf_size + is->audio_write_buf_size) / is->audio_tgt.bytes_per_sec, is->audio_clock_serial, audio_callback_time / 1000000.0);
		sync_clock_to_slave(&is->extclk, &is->audclk);
	}
}

static int audio_open(void *opaque, int64_t wanted_channel_layout, int wanted_nb_channels, int wanted_sample_rate, struct AudioParams *audio_hw_params)
{
	SDL_AudioSpec wanted_spec, spec;
	const char *env;
	static const int next_nb_channels[] = {0, 0, 1, 6, 2, 6, 4, 6};
	static const int next_sample_rates[] = {0, 44100, 48000, 96000, 192000};
	int next_sample_rate_idx = FF_ARRAY_ELEMS(next_sample_rates) - 1;

	env = SDL_getenv("SDL_AUDIO_CHANNELS");
	if (env)
	{
		wanted_nb_channels = atoi(env);
		wanted_channel_layout = av_get_default_channel_layout(wanted_nb_channels);
	}
	if (!wanted_channel_layout || wanted_nb_channels != av_get_channel_layout_nb_channels(wanted_channel_layout))
	{
		wanted_channel_layout = av_get_default_channel_layout(wanted_nb_channels);
		wanted_channel_layout &= ~AV_CH_LAYOUT_STEREO_DOWNMIX;
	}
	wanted_nb_channels = av_get_channel_layout_nb_channels(wanted_channel_layout);
	wanted_spec.channels = wanted_nb_channels;
	wanted_spec.freq = wanted_sample_rate;
	if (wanted_spec.freq <= 0 || wanted_spec.channels <= 0)
	{
		av_log(NULL, AV_LOG_ERROR, "Invalid sample rate or channel count!\n");
		return -1;
	}
	while (next_sample_rate_idx && next_sample_rates[next_sample_rate_idx] >= wanted_spec.freq)
		next_sample_rate_idx--;
	wanted_spec.format = AUDIO_S16SYS;
	wanted_spec.silence = 0;
	wanted_spec.samples = FFMAX(SDL_AUDIO_MIN_BUFFER_SIZE, 2 << av_log2(wanted_spec.freq / SDL_AUDIO_MAX_CALLBACKS_PER_SEC));
	wanted_spec.callback = sdl_audio_callback;
	wanted_spec.userdata = opaque;
	while (SDL_OpenAudio(&wanted_spec, &spec) < 0)
	{
		av_log(NULL, AV_LOG_WARNING, "SDL_OpenAudio (%d channels, %d Hz): %s\n",
			wanted_spec.channels, wanted_spec.freq, SDL_GetError());
		wanted_spec.channels = next_nb_channels[FFMIN(7, wanted_spec.channels)];
		if (!wanted_spec.channels)
		{
			wanted_spec.freq = next_sample_rates[next_sample_rate_idx--];
			wanted_spec.channels = wanted_nb_channels;
			if (!wanted_spec.freq)
			{
				av_log(NULL, AV_LOG_ERROR, "No more combinations to try, audio open failed\n");
				return -1;
			}
		}
		wanted_channel_layout = av_get_default_channel_layout(wanted_spec.channels);
	}
	if (spec.format != AUDIO_S16SYS)
	{
		av_log(NULL, AV_LOG_ERROR, "SDL advised audio format %d is not supported!\n", spec.format);
		return -1;
	}
	if (spec.channels != wanted_spec.channels)
	{
		wanted_channel_layout = av_get_default_channel_layout(spec.channels);
		if (!wanted_channel_layout)
		{
			av_log(NULL, AV_LOG_ERROR, "SDL advised channel count %d is not supported!\n", spec.channels);
			return -1;
		}
	}

	audio_hw_params->fmt = AV_SAMPLE_FMT_S16;
	audio_hw_params->freq = spec.freq;
	audio_hw_params->channel_layout = wanted_channel_layout;
	audio_hw_params->channels =  spec.channels;
	audio_hw_params->frame_size = av_samples_get_buffer_size(NULL, audio_hw_params->channels, 1, audio_hw_params->fmt, 1);
	audio_hw_params->bytes_per_sec = av_samples_get_buffer_size(NULL, audio_hw_params->channels, audio_hw_params->freq, audio_hw_params->fmt, 1);
	if (audio_hw_params->bytes_per_sec <= 0 || audio_hw_params->frame_size <= 0)
	{
		av_log(NULL, AV_LOG_ERROR, "av_samples_get_buffer_size failed\n");
		return -1;
	}
	return spec.size;
}

// open a given stream. Return 0 if OK
static int stream_component_open(VideoState *is, int stream_index)
{
	AVFormatContext *ic = is->ic;
	AVCodecContext *avctx;
	AVCodec *codec;
	const char *forced_codec_name = NULL;
	AVDictionary *opts = NULL;
	AVDictionaryEntry *t = NULL;
	int sample_rate, nb_channels;
	int64_t channel_layout;
	int ret;
	int stream_lowres = lowres;

	if (stream_index < 0 || stream_index >= ic->nb_streams)
		return -1;
	avctx = ic->streams[stream_index]->codec;

	codec = avcodec_find_decoder(avctx->codec_id);

	switch (avctx->codec_type)
	{
	case AVMEDIA_TYPE_AUDIO   : is->last_audio_stream    = stream_index; forced_codec_name = audio_codec_name; break;
	case AVMEDIA_TYPE_SUBTITLE: is->last_subtitle_stream = stream_index; forced_codec_name = subtitle_codec_name; break;
	case AVMEDIA_TYPE_VIDEO   : is->last_video_stream    = stream_index; forced_codec_name = video_codec_name; break;
	}
	if (forced_codec_name)
		codec = avcodec_find_decoder_by_name(forced_codec_name);
	if (!codec)
	{
		if (forced_codec_name)
			av_log(NULL, AV_LOG_WARNING, "No codec could be found with name '%s'\n", forced_codec_name);
		else
			av_log(NULL, AV_LOG_WARNING, "No codec could be found with id %d\n", avctx->codec_id);
		return -1;
	}

	avctx->codec_id = codec->id;
	avctx->workaround_bugs   = workaround_bugs;
	if (stream_lowres > av_codec_get_max_lowres(codec))
	{
		av_log(avctx, AV_LOG_WARNING, "The maximum value for lowres supported by the decoder is %d\n",
			av_codec_get_max_lowres(codec));
		stream_lowres = av_codec_get_max_lowres(codec);
	}
	av_codec_set_lowres(avctx, stream_lowres);

	if (stream_lowres) avctx->flags |= CODEC_FLAG_EMU_EDGE;
	if (fast)   avctx->flags2 |= CODEC_FLAG2_FAST;
	if (codec->capabilities & CODEC_CAP_DR1)
		avctx->flags |= CODEC_FLAG_EMU_EDGE;

	if (!av_dict_get(opts, "threads", NULL, 0))
		av_dict_set(&opts, "threads", "auto", 0);
	if (stream_lowres)
		av_dict_set_int(&opts, "lowres", stream_lowres, 0);
	if (avctx->codec_type == AVMEDIA_TYPE_VIDEO || avctx->codec_type == AVMEDIA_TYPE_AUDIO)
		av_dict_set(&opts, "refcounted_frames", "1", 0);
	if (avcodec_open2(avctx, codec, &opts) < 0)
		return -1;
	if ((t = av_dict_get(opts, "", NULL, AV_DICT_IGNORE_SUFFIX)))
	{
		av_log(NULL, AV_LOG_ERROR, "Option %s not found.\n", t->key);
		return AVERROR_OPTION_NOT_FOUND;
	}

	ic->streams[stream_index]->discard = AVDISCARD_DEFAULT;
	switch (avctx->codec_type)
	{
	case AVMEDIA_TYPE_AUDIO:
		sample_rate    = avctx->sample_rate;
		nb_channels    = avctx->channels;
		channel_layout = avctx->channel_layout;

		// prepare audio output
		if ((ret = audio_open(is, channel_layout, nb_channels, sample_rate, &is->audio_tgt)) < 0)
			return ret;
		is->audio_hw_buf_size = ret;
		is->audio_src = is->audio_tgt;
		is->audio_buf_size  = 0;
		is->audio_buf_index = 0;

		// init averaging filter
		is->audio_diff_avg_coef  = exp(log(0.01) / AUDIO_DIFF_AVG_NB);
		is->audio_diff_avg_count = 0;
		// since we do not have a precise anough audio fifo fullness, we correct audio sync only if larger than this threshold
		is->audio_diff_threshold = (double)(is->audio_hw_buf_size) / is->audio_tgt.bytes_per_sec;

		memset(&is->audio_pkt, 0, sizeof(is->audio_pkt));
		memset(&is->audio_pkt_temp, 0, sizeof(is->audio_pkt_temp));
		is->audio_pkt_temp.stream_index = -1;

		is->audio_stream = stream_index;
		is->audio_st = ic->streams[stream_index];

		packet_queue_start(&is->audioq);
		SDL_PauseAudio(0);
		break;
	case AVMEDIA_TYPE_VIDEO:
		is->video_stream = stream_index;
		is->video_st = ic->streams[stream_index];

		packet_queue_start(&is->videoq);
		is->video_tid = SDL_CreateThread(video_thread, "video_thread", is);
		is->queue_attachments_req = 1;
		break;
	case AVMEDIA_TYPE_SUBTITLE:
		is->subtitle_stream = stream_index;
		is->subtitle_st = ic->streams[stream_index];
		packet_queue_start(&is->subtitleq);

		is->subtitle_tid = SDL_CreateThread(subtitle_thread, "subtitle_thread", is);
		break;
	default:
		break;
	}

	return 0;
}

static void stream_component_close(VideoState *is, int stream_index)
{
	AVFormatContext *ic = is->ic;
	AVCodecContext *avctx;

	if (stream_index < 0 || stream_index >= ic->nb_streams)
		return;
	avctx = ic->streams[stream_index]->codec;

	switch (avctx->codec_type)
	{
	case AVMEDIA_TYPE_AUDIO:
		packet_queue_abort(&is->audioq);

		SDL_CloseAudio();

		packet_queue_flush(&is->audioq);
		av_free_packet(&is->audio_pkt);
		swr_free(&is->swr_ctx);
		av_freep(&is->audio_buf1);
		is->audio_buf1_size = 0;
		is->audio_buf = NULL;
		av_frame_free(&is->frame);

		if (is->rdft)
		{
			av_rdft_end(is->rdft);
			av_freep(&is->rdft_data);
			is->rdft = NULL;
			is->rdft_bits = 0;
		}
		break;
	case AVMEDIA_TYPE_VIDEO:
		packet_queue_abort(&is->videoq);

		// note: we also signal this mutex to make sure we deblock the video thread in all cases
		SDL_LockMutex(is->pictq_mutex);
		SDL_CondSignal(is->pictq_cond);
		SDL_UnlockMutex(is->pictq_mutex);

		SDL_WaitThread(is->video_tid, NULL);

		packet_queue_flush(&is->videoq);
		break;
	case AVMEDIA_TYPE_SUBTITLE:
		packet_queue_abort(&is->subtitleq);

		// note: we also signal this mutex to make sure we deblock the video thread in all cases
		SDL_LockMutex(is->subpq_mutex);
		SDL_CondSignal(is->subpq_cond);
		SDL_UnlockMutex(is->subpq_mutex);

		SDL_WaitThread(is->subtitle_tid, NULL);

		packet_queue_flush(&is->subtitleq);
		break;
	default:
		break;
	}

	ic->streams[stream_index]->discard = AVDISCARD_ALL;
	avcodec_close(avctx);
	switch (avctx->codec_type)
	{
	case AVMEDIA_TYPE_AUDIO:
		is->audio_st = NULL;
		is->audio_stream = -1;
		break;
	case AVMEDIA_TYPE_VIDEO:
		is->video_st = NULL;
		is->video_stream = -1;
		break;
	case AVMEDIA_TYPE_SUBTITLE:
		is->subtitle_st = NULL;
		is->subtitle_stream = -1;
		break;
	default:
		break;
	}
}

static int decode_interrupt_cb(void *ctx)
{
	VideoState *is = (VideoState*)ctx;
	return is->abort_request ? 1 : 0;
}

static int is_realtime(AVFormatContext *s)
{
	if (!strcmp(s->iformat->name, "rtp")
		|| !strcmp(s->iformat->name, "rtsp")
		|| !strcmp(s->iformat->name, "sdp") )
		return 1;

	if (s->pb && (!strncmp(s->filename, "rtp:", 4) || !strncmp(s->filename, "udp:", 4)))
		return 1;

	return 0;
}

static void calc_total_duration(VideoState *is)
{
	AVFormatContext *ic = is->ic;

	if (ic && (ic->duration != AV_NOPTS_VALUE))
		is->total_duration = ic->duration / 1000; // msec
	else
		is->total_duration = 0;
}

// this thread gets the stream from the disk or the network
static int read_thread(void *arg)
{
	VideoState *is = (VideoState*)arg;
	AVFormatContext *ic = NULL;
	int err, i, ret;
	int st_index[AVMEDIA_TYPE_NB];
	AVPacket pkt1, *pkt = &pkt1;
	int eof = 0;
	int64_t stream_start_time;
	int pkt_in_play_range = 0;
	AVDictionaryEntry *t;
	int orig_nb_streams;
	SDL_mutex *wait_mutex = SDL_CreateMutex();

	LOGD("send message: MSG_STATE_CHANGED: STATE_PREPARING");
	Android_JNI_SendMessage(MSG_STATE_CHANGED, STATE_PREPARING);

	memset(st_index, -1, sizeof(st_index));
	is->last_video_stream = is->video_stream = -1;
	is->last_audio_stream = is->audio_stream = -1;
	is->last_subtitle_stream = is->subtitle_stream = -1;

	ic = avformat_alloc_context();
	ic->interrupt_callback.callback = decode_interrupt_cb;
	ic->interrupt_callback.opaque = is;
	err = avformat_open_input(&ic, is->filename, is->iformat, NULL);
	if (err < 0)
	{
		ret = -1;
		goto fail;
	}
	is->ic = ic;

	if (genpts)
		ic->flags |= AVFMT_FLAG_GENPTS;

	av_format_inject_global_side_data(ic);

	orig_nb_streams = ic->nb_streams;

	err = avformat_find_stream_info(ic, NULL);
	if (err < 0)
	{
		av_log(NULL, AV_LOG_WARNING,
			   "%s: could not find codec parameters\n", is->filename);
		ret = -1;
		goto fail;
	}

	if (ic->pb)
		ic->pb->eof_reached = 0; // FIXME hack, ffplay maybe should not use avio_feof() to test for the end

	if (seek_by_bytes < 0)
		seek_by_bytes = !!(ic->iformat->flags & AVFMT_TS_DISCONT) && strcmp("ogg", ic->iformat->name);

	is->max_frame_duration = (ic->iformat->flags & AVFMT_TS_DISCONT) ? 10.0 : 3600.0;

	// if seeking requested, we execute it
	if (start_time != AV_NOPTS_VALUE)
	{
		int64_t timestamp;

		timestamp = start_time;
		// add the stream start time
		if (ic->start_time != AV_NOPTS_VALUE)
			timestamp += ic->start_time;
		ret = avformat_seek_file(ic, -1, INT64_MIN, timestamp, INT64_MAX, 0);
		if (ret < 0)
		{
			av_log(NULL, AV_LOG_WARNING, "%s: could not seek to position %0.3f\n",
					is->filename, (double)timestamp / AV_TIME_BASE);
		}
	}

	is->realtime = is_realtime(ic);

	for (i = 0; i < ic->nb_streams; i++)
		ic->streams[i]->discard = AVDISCARD_ALL;
	if (!video_disable)
		st_index[AVMEDIA_TYPE_VIDEO] =
			av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO,
								wanted_stream[AVMEDIA_TYPE_VIDEO], -1, NULL, 0);
	if (!audio_disable)
		st_index[AVMEDIA_TYPE_AUDIO] =
			av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO,
								wanted_stream[AVMEDIA_TYPE_AUDIO],
								st_index[AVMEDIA_TYPE_VIDEO],
								NULL, 0);
	if (!video_disable && !subtitle_disable)
		st_index[AVMEDIA_TYPE_SUBTITLE] =
			av_find_best_stream(ic, AVMEDIA_TYPE_SUBTITLE,
								wanted_stream[AVMEDIA_TYPE_SUBTITLE],
								(st_index[AVMEDIA_TYPE_AUDIO] >= 0 ?
								 st_index[AVMEDIA_TYPE_AUDIO] :
								 st_index[AVMEDIA_TYPE_VIDEO]),
								NULL, 0);

	if (show_status)
		av_dump_format(ic, 0, is->filename, 0);

	is->show_mode = show_mode;

	// open the streams
	if (st_index[AVMEDIA_TYPE_AUDIO] >= 0)
	{
		stream_component_open(is, st_index[AVMEDIA_TYPE_AUDIO]);
	}

	ret = -1;
	if (st_index[AVMEDIA_TYPE_VIDEO] >= 0)
		ret = stream_component_open(is, st_index[AVMEDIA_TYPE_VIDEO]);

	if (is->show_mode == SHOW_MODE_NONE)
		is->show_mode = ret >= 0 ? SHOW_MODE_VIDEO : SHOW_MODE_RDFT;

	if (st_index[AVMEDIA_TYPE_SUBTITLE] >= 0)
		stream_component_open(is, st_index[AVMEDIA_TYPE_SUBTITLE]);

	if (is->video_stream < 0 && is->audio_stream < 0)
	{
		av_log(NULL, AV_LOG_FATAL, "Failed to open file '%s' or configure filtergraph\n",
			   is->filename);
		ret = -1;
		goto fail;
	}

	if (infinite_buffer < 0 && is->realtime)
		infinite_buffer = 1;

	calc_total_duration(is);

	for (;;)
	{
		if (is->abort_request)
			break;
		if (is->paused != is->last_paused)
		{
			is->last_paused = is->paused;
			if (is->paused)
				is->read_pause_return = av_read_pause(ic);
			else
				av_read_play(ic);
		}
#if CONFIG_RTSP_DEMUXER || CONFIG_MMSH_PROTOCOL
		if (is->paused &&
			(!strcmp(ic->iformat->name, "rtsp") ||
			(ic->pb && !strncmp(player_source.c_str(), "mmsh:", 5))))
		{
			// wait 10 ms to avoid trying to get another packet
			// XXX: horrible
			SDL_Delay(10);
			continue;
		}
#endif
		if (is->seek_req)
		{
			int64_t seek_target = is->seek_pos;
			int64_t seek_min    = is->seek_rel > 0 ? seek_target - is->seek_rel + 2: INT64_MIN;
			int64_t seek_max    = is->seek_rel < 0 ? seek_target - is->seek_rel - 2: INT64_MAX;

			// seeking to the end of the file will block in avformat_seek_file().
			if (seek_target >= (int64_t)is->total_duration * 1000)
			{
				const int DELTA_MILLISEC = 1;
				seek_target = (int64_t)(is->total_duration - DELTA_MILLISEC) * 1000;
				if (seek_target < 0)
					seek_target = 0;
			}

			// FIXME the +-2 is due to rounding being not done in the correct direction in generation of the seek_pos/seek_rel variables

			ret = avformat_seek_file(is->ic, -1, seek_min, seek_target, seek_max, is->seek_flags);
			if (ret < 0)
			{
				LOGD("seek error");
			}
			else
			{
				LOGD("seek ok");

				if (is->audio_stream >= 0)
				{
					packet_queue_flush(&is->audioq);
					packet_queue_put(&is->audioq, &flush_pkt);
				}
				if (is->subtitle_stream >= 0)
				{
					packet_queue_flush(&is->subtitleq);
					packet_queue_put(&is->subtitleq, &flush_pkt);
				}
				if (is->video_stream >= 0)
				{
					packet_queue_flush(&is->videoq);
					packet_queue_put(&is->videoq, &flush_pkt);
				}
				if (is->seek_flags & AVSEEK_FLAG_BYTE)
				{
				   set_clock(&is->extclk, NAN, 0);
				}
				else
				{
				   set_clock(&is->extclk, seek_target / (double)AV_TIME_BASE, 0);
				}
			}
			is->seek_req = 0;
			is->queue_attachments_req = 1;
			eof = 0;
			if (is->paused)
				step_to_next_frame(is);
		}
		if (is->queue_attachments_req)
		{
			if (is->video_st && is->video_st->disposition & AV_DISPOSITION_ATTACHED_PIC)
			{
				AVPacket copy;
				if ((ret = av_copy_packet(&copy, &is->video_st->attached_pic)) < 0)
					goto fail;
				packet_queue_put(&is->videoq, &copy);
				packet_queue_put_nullpacket(&is->videoq, is->video_stream);
			}
			is->queue_attachments_req = 0;
		}

		// if the queue are full, no need to read more
		if (infinite_buffer<1 &&
			  (is->audioq.size + is->videoq.size + is->subtitleq.size > MAX_QUEUE_SIZE
			|| (   (is->audioq   .nb_packets > min_buffer_frames || is->audio_stream < 0 || is->audioq.abort_request)
				&& (is->videoq   .nb_packets > min_buffer_frames || is->video_stream < 0 || is->videoq.abort_request
					|| (is->video_st->disposition & AV_DISPOSITION_ATTACHED_PIC))
				&& (is->subtitleq.nb_packets > min_buffer_frames || is->subtitle_stream < 0 || is->subtitleq.abort_request))))
		{
			// wait 10 ms
			SDL_LockMutex(wait_mutex);
			SDL_CondWaitTimeout(is->continue_read_thread, wait_mutex, 10);
			SDL_UnlockMutex(wait_mutex);
			continue;
		}
		if (!is->paused &&
			(!is->audio_st || is->audio_finished == is->audioq.serial) &&
			(!is->video_st || (is->video_finished == is->videoq.serial && pictq_nb_remaining(is) == 0)))
		{
			if (loop != 1 && (!loop || --loop))
			{
				stream_seek(is, start_time != AV_NOPTS_VALUE ? start_time : 0, 0, false);
			}
			else if (autoexit)
			{
				ret = AVERROR_EOF;
				goto fail;
			}
		}
		if (eof)
		{
			if (is->video_stream >= 0)
				packet_queue_put_nullpacket(&is->videoq, is->video_stream);
			if (is->audio_stream >= 0)
				packet_queue_put_nullpacket(&is->audioq, is->audio_stream);
			if (is->subtitle_stream >= 0)
				packet_queue_put_nullpacket(&is->subtitleq, is->subtitle_stream);
			SDL_Delay(10);
			eof = 0;
			continue;
		}
		ret = av_read_frame(ic, pkt);
		if (ret < 0)
		{
			if (ret == AVERROR_EOF || avio_feof(ic->pb))
			{
				eof = 1;

				if (!is->sent_play_complete_msg)
				{
					is->sent_play_complete_msg = true;

					LOGD("play eof");

					send_event(PLAYER_COMPLETE_EVENT, NULL, NULL);
				}
			}
			if (ic->pb && ic->pb->error)
				break;
			SDL_LockMutex(wait_mutex);
			SDL_CondWaitTimeout(is->continue_read_thread, wait_mutex, 10);
			SDL_UnlockMutex(wait_mutex);
			continue;
		}
		else
		{
			is->sent_play_complete_msg = false;
		}

		// check if packet is in play range specified by user, then queue, otherwise discard
		stream_start_time = ic->streams[pkt->stream_index]->start_time;
		pkt_in_play_range = duration == AV_NOPTS_VALUE ||
			(pkt->pts - (stream_start_time != AV_NOPTS_VALUE ? stream_start_time : 0)) *
			av_q2d(ic->streams[pkt->stream_index]->time_base) -
			(double)(start_time != AV_NOPTS_VALUE ? start_time : 0) / 1000000
			<= ((double)duration / 1000000);
		if (pkt->stream_index == is->audio_stream && pkt_in_play_range)
		{
			packet_queue_put(&is->audioq, pkt);
		}
		else if (pkt->stream_index == is->video_stream && pkt_in_play_range
			&& !(is->video_st->disposition & AV_DISPOSITION_ATTACHED_PIC))
		{
			packet_queue_put(&is->videoq, pkt);
		}
		else if (pkt->stream_index == is->subtitle_stream && pkt_in_play_range)
		{
			packet_queue_put(&is->subtitleq, pkt);
		}
		else
		{
			av_free_packet(pkt);
		}
	}
	// wait until the end
	while (!is->abort_request)
		SDL_Delay(100);

	ret = 0;

 fail:
	// close each stream
	if (is->audio_stream >= 0)
		stream_component_close(is, is->audio_stream);
	if (is->video_stream >= 0)
		stream_component_close(is, is->video_stream);
	if (is->subtitle_stream >= 0)
		stream_component_close(is, is->subtitle_stream);
	if (is->ic)
		avformat_close_input(&is->ic);

	if (ret != 0)
	{
		send_event(PLAYER_QUIT_EVENT, is, NULL);
		/*
		LOGD("send message: MSG_ERROR: EC_OPEN_ERROR");
		Android_JNI_SendMessage(MSG_ERROR, EC_OPEN_ERROR);
		*/
	}
	SDL_DestroyMutex(wait_mutex);
	return 0;
}

static VideoState *stream_open(const char *filename, AVInputFormat *iformat)
{
	VideoState *is;

	is = (VideoState*)av_mallocz(sizeof(VideoState));
	if (!is)
		return NULL;
	av_strlcpy(is->filename, filename, sizeof(is->filename));
	is->iformat = iformat;
	is->ytop = 0;
	is->xleft = 0;

	// start video display
	is->pictq_mutex = SDL_CreateMutex();
	is->pictq_cond  = SDL_CreateCond();

	is->subpq_mutex = SDL_CreateMutex();
	is->subpq_cond  = SDL_CreateCond();

	packet_queue_init(&is->videoq);
	packet_queue_init(&is->audioq);
	packet_queue_init(&is->subtitleq);

	is->continue_read_thread = SDL_CreateCond();

	init_clock(&is->vidclk, &is->videoq.serial);
	init_clock(&is->audclk, &is->audioq.serial);
	init_clock(&is->extclk, &is->extclk.serial);
	is->audio_clock_serial = -1;
	is->audio_last_serial = -1;
	is->av_sync_type = av_sync_type;

	is->read_tid = SDL_CreateThread(read_thread, "read_thread", is);
	if (!is->read_tid)
	{
		av_free(is);
		return NULL;
	}

	is->event_loop_tid = SDL_CreateThread(event_loop_thread, "event_loop_thread", is);
	if (!is->event_loop_tid)
	{
		av_free(is);
		return NULL;
	}

	return is;
}

static void stream_cycle_channel(VideoState *is, int codec_type)
{
	AVFormatContext *ic = is->ic;
	int start_index, stream_index;
	int old_index;
	AVStream *st;
	AVProgram *p = NULL;
	int nb_streams = is->ic->nb_streams;

	if (codec_type == AVMEDIA_TYPE_VIDEO)
	{
		start_index = is->last_video_stream;
		old_index = is->video_stream;
	}
	else if (codec_type == AVMEDIA_TYPE_AUDIO)
	{
		start_index = is->last_audio_stream;
		old_index = is->audio_stream;
	}
	else
	{
		start_index = is->last_subtitle_stream;
		old_index = is->subtitle_stream;
	}
	stream_index = start_index;

	if (codec_type != AVMEDIA_TYPE_VIDEO && is->video_stream != -1)
	{
		p = av_find_program_from_stream(ic, NULL, is->video_stream);
		if (p)
		{
			nb_streams = p->nb_stream_indexes;
			for (start_index = 0; start_index < nb_streams; start_index++)
				if (p->stream_index[start_index] == stream_index)
					break;
			if (start_index == nb_streams)
				start_index = -1;
			stream_index = start_index;
		}
	}

	for (;;)
	{
		if (++stream_index >= nb_streams)
		{
			if (codec_type == AVMEDIA_TYPE_SUBTITLE)
			{
				stream_index = -1;
				is->last_subtitle_stream = -1;
				goto the_end;
			}
			if (start_index == -1)
				return;
			stream_index = 0;
		}
		if (stream_index == start_index)
			return;
		st = is->ic->streams[p ? p->stream_index[stream_index] : stream_index];
		if (st->codec->codec_type == codec_type)
		{
			// check that parameters are OK
			switch (codec_type)
			{
			case AVMEDIA_TYPE_AUDIO:
				if (st->codec->sample_rate != 0 &&
					st->codec->channels != 0)
					goto the_end;
				break;
			case AVMEDIA_TYPE_VIDEO:
			case AVMEDIA_TYPE_SUBTITLE:
				goto the_end;
			default:
				break;
			}
		}
	}

 the_end:
	if (p && stream_index != -1)
		stream_index = p->stream_index[stream_index];
	av_log(NULL, AV_LOG_INFO, "Switch %s stream from #%d to #%d\n",
		av_get_media_type_string((AVMediaType)codec_type),
		old_index,
		stream_index);

	stream_component_close(is, old_index);
	stream_component_open(is, stream_index);
}

static void player_buffer_check(VideoState *is)
{
	const int MAX_MBF = 64;

	if (is->is_buffered_ok)
	{
		if (is->videoq.nb_packets <= 0)
		{
			is->is_buffered_ok = false;

			min_buffer_frames *= 2;
			if (min_buffer_frames > MAX_MBF)
				min_buffer_frames = MAX_MBF;

			LOGD("send message: MSG_STATE_CHANGED: STATE_BUFFERING");
			Android_JNI_SendMessage(MSG_STATE_CHANGED, STATE_BUFFERING);
		}
	}
	else
	{
		if (is->videoq.nb_packets >= min_buffer_frames)
		{
			is->is_buffered_ok = true;

			LOGD("send message: MSG_STATE_CHANGED: STATE_READY");
			Android_JNI_SendMessage(MSG_STATE_CHANGED, STATE_READY);
		}
	}
}

static void refresh_loop_wait_event(VideoState *is, SDL_Event *event)
{
	double remaining_time = 0.0;
	SDL_PumpEvents();
	while (!SDL_PeepEvents(event, 1, SDL_GETEVENT, SDL_FIRSTEVENT, SDL_LASTEVENT))
	{
		if (!cursor_hidden && av_gettime_relative() - cursor_last_shown > CURSOR_HIDE_DELAY)
		{
			SDL_ShowCursor(0);
			cursor_hidden = 1;
		}
		if (remaining_time > 0.0)
			av_usleep((int64_t)(remaining_time * 1000000.0));
		remaining_time = REFRESH_RATE;
		if (is->show_mode != SHOW_MODE_NONE && (!is->paused || is->force_refresh))
			video_refresh(is, &remaining_time);
		player_buffer_check(is);
		SDL_PumpEvents();
	}
}

static void seek_chapter(VideoState *is, int incr)
{
	int64_t pos = get_master_clock(is) * AV_TIME_BASE;
	int i;

	if (!is->ic->nb_chapters)
		return;

	// find the current chapter
	for (i = 0; i < is->ic->nb_chapters; i++)
	{
		AVChapter *ch = is->ic->chapters[i];
		if (av_compare_ts(pos, AV_TIME_BASE_Q, ch->start, ch->time_base) < 0)
		{
			i--;
			break;
		}
	}

	i += incr;
	i = FFMAX(i, 0);
	if (i >= is->ic->nb_chapters)
		return;

	av_log(NULL, AV_LOG_VERBOSE, "Seeking to chapter %d.\n", i);
	stream_seek(is, av_rescale_q(is->ic->chapters[i]->start, is->ic->chapters[i]->time_base, AV_TIME_BASE_Q), 0, false);
}

static void video_resize(VideoState *is, int w, int h)
{
	LOGD("video_resize. w:%d,h:%d", w, h);

	//is->force_refresh = true;
	is->width = w;
	is->height = h;

	const int REFRESH_TIMES = 3;
	for (int i = 0; i < REFRESH_TIMES; ++i)
		send_event(PLAYER_REFRESH_EVENT, NULL, NULL);
}

static void seek_to(int msec, bool seek_any_frame)
{
	if (current_stream)
		current_stream->current_pos = msec;

	send_event(PLAYER_SEEK_EVENT,
		(void*)msec,
		(void*)(seek_any_frame? 1 : 0));
}

static void on_surface_changed(VideoState *is)
{
	if (renderer)
	{
		SDL_DestroyRenderer(renderer);
		renderer = NULL;
	}

	if (texture)
	{
		SDL_DestroyTexture(texture);
		texture = NULL;
	}

	video_open(is, NULL);
}

static int event_loop_thread(void *arg)
{
	VideoState *cur_stream = (VideoState*)arg;

	SDL_Event event;
	double incr, pos, frac;

	while (!cur_stream->abort_request)
	{
		refresh_loop_wait_event(cur_stream, &event);

		switch (event.type)
		{
		case SDL_WINDOWEVENT:
			if (event.window.event == SDL_WINDOWEVENT_RESIZED)
				video_resize(cur_stream, event.window.data1, event.window.data2);
			break;

		case SDL_QUIT:
		case PLAYER_QUIT_EVENT:
			do_exit(cur_stream);
			return 0;

		case PLAYER_ALLOC_EVENT:
			alloc_picture((VideoState*)event.user.data1);
			break;

		case PLAYER_TOGGLE_PAUSE_EVENT:
			toggle_pause(cur_stream);
			break;

		case PLAYER_SEEK_EVENT:
			{
				LOGD("seekto: %d msec. seek_any_frame: %d", (int)event.user.data1, (int)event.user.data2);
				int64_t seek_pos = (int64_t)1000 * (int)event.user.data1;
				bool seek_any_frame = (event.user.data2 != 0);
				stream_seek(cur_stream, seek_pos, 0, seek_any_frame);
				break;
			}

		case PLAYER_COMPLETE_EVENT:
			LOGD("send message: MSG_STATE_CHANGED: STATE_ENDED");
			Android_JNI_SendMessage(MSG_STATE_CHANGED, STATE_ENDED);
			break;

		case PLAYER_REFRESH_EVENT:
			video_display(cur_stream);
			break;

		case PLAYER_SURFACE_CHANGED_EVENT:
			on_surface_changed(cur_stream);
			video_display(cur_stream);
			break;

		default:
			break;
		}
	}

	return 0;
}

static int lockmgr(void **mtx, enum AVLockOp op)
{
	switch (op)
	{
	case AV_LOCK_CREATE:
		*mtx = SDL_CreateMutex();
		if (!*mtx)
			return 1;
		return 0;
	case AV_LOCK_OBTAIN:
		return !!SDL_LockMutex((SDL_mutex*)(*mtx));
	case AV_LOCK_RELEASE:
		return !!SDL_UnlockMutex((SDL_mutex*)(*mtx));
	case AV_LOCK_DESTROY:
		SDL_DestroyMutex((SDL_mutex*)(*mtx));
		return 0;
   }

   return 1;
}

// Called from the main
int main(int argc, char **argv)
{
	return 0;
}

///////////////////////////////////////////////////////////////////////////////
// interface

bool player_init()
{
	LOGD("player_init");

	reset_global_variables();

	av_log_set_flags(AV_LOG_SKIP_REPEATED);

	av_register_all();
	avformat_network_init();

	signal(SIGINT, sigterm_handler);   // Interrupt (ANSI).
	signal(SIGTERM, sigterm_handler);  // Termination (ANSI).

	int flags = SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER;
	if (SDL_Init(flags))
	{
		LOGD("Could not initialize SDL - %s\n", SDL_GetError());
		LOGD("(Did you set the DISPLAY variable?)\n");
		return false;
	}

	SDL_SetHintWithPriority("SDL_RENDER_SCALE_QUALITY", "2", SDL_HINT_OVERRIDE);

	SDL_DisplayMode dm;
	if (SDL_GetDesktopDisplayMode(0, &dm) == 0)
	{
		int screen_width = dm.w, screen_height = dm.h;
		LOGD("screen_width: %d, screen_height: %d", screen_width, screen_height);
	}

	SDL_EventState(SDL_SYSWMEVENT, SDL_IGNORE);
	SDL_EventState(SDL_USEREVENT, SDL_IGNORE);

	if (av_lockmgr_register(lockmgr))
	{
		LOGD("Could not initialize lock manager!\n");
		return false;
	}

	av_init_packet(&flush_pkt);
	flush_pkt.data = (uint8_t *)&flush_pkt;

	return true;
}

void player_set_source(const char *url)
{
	if (url == NULL)
		return;

	LOGD("player_set_source: [%s]", url);

	if (current_stream != NULL)
	{
		LOGD("Please stop player firstly.");
		return;
	}

	player_source = url;
}

void player_start()
{
	LOGD("player_start");

	if (!current_stream)
		current_stream = stream_open(player_source.c_str(), NULL);

	if (current_stream)
	{
		bool is_seeking = current_stream->step;
		if (current_stream->paused || is_seeking)
			send_event(PLAYER_TOGGLE_PAUSE_EVENT, NULL, NULL);

		if (seek_pos_on_start > 0)
		{
			seek_to(seek_pos_on_start, true);
			seek_pos_on_start = 0;
		}
	}
}

void player_pause()
{
	LOGD("player_pause");

	if (current_stream)
	{
		if (!current_stream->paused)
			send_event(PLAYER_TOGGLE_PAUSE_EVENT, NULL, NULL);
	}
}

void player_stop()
{
	LOGD("player_stop");

	if (current_stream)
	{
		send_event(PLAYER_QUIT_EVENT, current_stream, NULL);
		current_stream = NULL;
	}
}

int player_get_duration()
{
	if (current_stream)
		return current_stream->total_duration;
	else
		return 0;
}

int player_get_cur_pos()
{
	if (current_stream)
		return current_stream->current_pos;
	else
		return 0;
}

void player_seek_to(int msec)
{
	LOGD("player_seek_to: %d", msec);

	if (current_stream)
		seek_to(msec, false);
	else
		seek_pos_on_start = msec;
}

bool player_is_playing()
{
	if (current_stream)
		return !(current_stream->paused);
	else
		return false;
}
