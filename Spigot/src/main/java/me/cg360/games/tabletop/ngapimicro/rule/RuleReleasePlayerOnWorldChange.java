package me.cg360.games.tabletop.ngapimicro.rule;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;

public class RuleReleasePlayerOnWorldChange extends WatchdogRule implements Listener {

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
    public void onChangeDaWorld(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if((from.getWorld() != null) && (to != null) && (from.getWorld() != to.getWorld())) { // Check for world change.

            if(MicroGameWatchdog.getPlayerWatchdogs().containsKey(event.getPlayer())) { // Check it's a game player.
                HashMap<Player, MicroGameWatchdog<?>> w = MicroGameWatchdog.getPlayerWatchdogs();

                if(w.get(event.getPlayer()) == watchdog) {
                    watchdog.releasePlayer(event.getPlayer());
                }
            }
        }
    }
}
