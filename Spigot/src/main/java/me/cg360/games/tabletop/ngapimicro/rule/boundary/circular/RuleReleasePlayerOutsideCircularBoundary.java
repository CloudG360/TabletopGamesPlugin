package me.cg360.games.tabletop.ngapimicro.rule.boundary.circular;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.RuleAbstractCircularBoundary;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RuleReleasePlayerOutsideCircularBoundary extends RuleAbstractCircularBoundary {

    public RuleReleasePlayerOutsideCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled) {
        super(origin, radius, areEdgeParticlesEnabled);
    }

    @Override
    protected void onStartBoundaryWatchdog() {
        new BukkitRunnable() {

            @Override
            public void run() {

                if(watchdog.isRunning()) {

                    for(Player player: watchdog.getPlayers()) {
                        Vector player2D = new Vector(player.getLocation().getX(), 0, player.getLocation().getZ());
                        Vector origin2D = new Vector(origin.getX(), 0, origin.getZ());

                        if(player2D.distance(origin2D) > radius) {
                            watchdog.releasePlayer(player);
                        }
                    }
                } else this.cancel();
            }

        }.runTaskTimer(TabletopGamesSpigot.get(), 5, 10);
    }

}
