package me.cg360.games.tabletop.game;

import cn.nukkit.Player;
import net.cg360.nsapi.commons.data.Settings;

public abstract class MicroGameBehaviour {
    //TODO: Use FilteredListener from NSAPICommons. Use it for vanilla and game events.

    private MicroGameWatchdog<?> watchdog = null;

    /** Entry point of the simple game instance. */
    public abstract void init(Settings settings);

    /** Called when the game has already been ended. */
    public void finish() {
        if(getWatchdog().isRunning()) {
            getWatchdog().stopGame();
        }
    }



    /** Called when this micro-game successfully reserves a player. */
    protected abstract void onSuccessfulPlayerCapture(Player player);

    /** Called when a player is removed from the micro-game. This can be externally
     * called in the case the player switches worlds, quits the server, etc. */
    protected abstract void onPlayerRelease(Player player);




    /**
     * Sets the watchdog of this live game instance if not
     * already set.
     * @param watchdog the watchdog.
     */
    public void setWatchdog(MicroGameWatchdog<?> watchdog) {
        if(this.watchdog == null) {
            this.watchdog = watchdog;
        }
    }

    public MicroGameWatchdog<?> getWatchdog() { return watchdog; }
}
