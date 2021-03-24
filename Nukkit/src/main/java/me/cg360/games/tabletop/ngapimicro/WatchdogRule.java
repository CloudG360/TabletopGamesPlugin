package me.cg360.games.tabletop.ngapimicro;

import net.cg360.nsapi.commons.Check;

/**
 * Handles specific checks which would typically be watched by a
 * minigame API.
 */
public abstract class WatchdogRule {

    protected abstract void onStartWatchdog(MicroGameWatchdog<?> watchdog);
    protected abstract void onStopWatchdog();


}
