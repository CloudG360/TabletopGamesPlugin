package me.cg360.games.tabletop.game;

import me.cg360.games.tabletop.game.MicroGameWatchdog;

/**
 * Handles specific checks which would typically be watched by a
 * minigame API.
 */
public abstract class WatchdogRule {

    public MicroGameWatchdog<?> watchdog;

    public WatchdogRule(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
    }

    protected abstract void onStartWatchdog();
    protected abstract void onStopWatchdog();


}
