package me.cg360.games.tabletop.ngapimicro.rule;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;

import java.util.HashMap;

public class RuleReleasePlayerOnQuit extends WatchdogRule implements Listener {

    protected MicroGameWatchdog<?> watchdog;

    @Override
    protected void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesNukkit.get().getServer().getPluginManager().registerEvents(this, TabletopGamesNukkit.get());
    }

    @Override
    protected void onStopWatchdog() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onQuitEvent(PlayerQuitEvent event) {

        if(MicroGameWatchdog.getPlayerWatchdogs().containsKey(event.getPlayer())) { // Check it's a game player.
            HashMap<Player, MicroGameWatchdog<?>> w = MicroGameWatchdog.getPlayerWatchdogs();

            if(w.get(event.getPlayer()) == watchdog) {
                watchdog.releasePlayer(event.getPlayer());
            }
        }
    }
}
