package bf.cloud.android.components.mediaplayer;

public abstract class MediaControllerBase {
	public interface MediaPlayerControl{
        void    start();
        void	stop();
        void    pause();
        void	resume();
        long     getDuration();
        long     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
        void	setVrFlag(boolean flag);
        /**
         * Get the audio session id for the player used by this VideoView. This can be used to
         * apply audio effects to the audio track of a video.
         * @return The audio session, or 0 if there was an error.
         */
//        int     getAudioSessionId();
	}
}
