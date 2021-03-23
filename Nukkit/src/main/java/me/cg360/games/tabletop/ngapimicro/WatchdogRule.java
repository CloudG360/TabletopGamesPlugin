package me.cg360.games.tabletop.ngapimicro;

import net.cg360.nsapi.commons.Check;

/**
 * Handles specific checks which would typically be watched by a
 * minigame API.
 */
public abstract class WatchdogRule {

    public MicroGameWatchdog<?> watchdog;

    public WatchdogRule(MicroGameWatchdog<?> watchdog) {
        Check.nullParam(watchdog, "watchdog");
        this.watchdog = watchdog;
    }

    protected abstract void onStartWatchdog();
    protected abstract void onStopWatchdog();


}
