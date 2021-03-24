package me.cg360.games.tabletop.ngapimicro.rule;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

public class RuleReleasePlayerOnQuit extends WatchdogRule implements Listener {

    protected MicroGameWatchdog<?> watchdog;

    @Override
    protected void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesSpigot.get().getServer().getPluginManager().registerEvents(this, TabletopGamesSpigot.get());
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
