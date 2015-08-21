package bf.cloud.android.events;

import bf.cloud.android.components.player.PlayerCommand;

/**
 * 功能:定义播放器相关事件
 * Created by gehuanfei on 2014/9/18.
 */
public class PlayerEvent implements BFYICustomEvent {

    PlayerCommand mCommand;

    public PlayerEvent(PlayerCommand command) {
        mCommand = command;
    }

    public Object getData() {
        return mCommand;
    }
}
