package me.cg360.games.tabletop.game;

import cn.nukkit.Player;
import me.cg360.games.tabletop.game.rule.WatchdogRule;
import net.cg360.nsapi.commons.data.Settings;

public abstract class MicroGameBehaviour {
    //TODO: Use FilteredListener from NSAPICommons. Use it for vanilla and game events.

    private MicroGameWatchdog<?> watchdog = null;

    // -- Initialization --

    /** Entry point of the simple game instance. */
    public abstract void init(Settings settings);

    public abstract WatchdogRule[] getRules();



    // -- Important local events --

    /** Called when this micro-game successfully reserves a player. */
    protected abstract void onPlayerCapture(Player player);

    /** Called when a player is removed from the micro-game. This can be externally
     * called in the case the player switches worlds, quits the server, etc. */
    protected abstract void onPlayerRelease(Player player);

    /** Called when the #finish() method is called.*/
    protected abstract void onFinish();



    // -- Internal Watchdog proxy methods --

    /** Called to end the game. */
    public final void finish() {
        getWatchdog().stopGame();
    }

    public final void capturePlayer(Player player) {
        getWatchdog().capturePlayer(player);
    }

    public final void releasePlayer(Player player) {
        getWatchdog().releasePlayer(player);
    }


    // -- Watchdog init --

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
