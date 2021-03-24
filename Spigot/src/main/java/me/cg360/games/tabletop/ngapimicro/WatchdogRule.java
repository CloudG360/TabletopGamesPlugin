package me.cg360.games.tabletop.ngapimicro;

/**
 * Handles specific checks which would typically be watched by a
 * minigame API.
 */
public abstract class WatchdogRule {

    protected abstract void onStartWatchdog(MicroGameWatchdog<?> watchdog);
    protected abstract void onStopWatchdog();


}
