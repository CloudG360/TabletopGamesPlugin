package me.cg360.games.tabletop.ngapimicro.rule.boundary.circular;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.RuleAbstractCircularBoundary;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RulePushIntoCircularBoundary extends RuleAbstractCircularBoundary {

    protected Vector force;

    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled) { this(origin, radius, areEdgeParticlesEnabled, null); }
    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled, Vector force) {
        super(origin, radius, areEdgeParticlesEnabled);
        this.force = force == null ? new Vector(1.1f, 0.4f, 1.1f) : force;
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
                            Vector delta = origin2D.subtract(player2D);
                            Vector direction = new Vector(delta.getX(), 0, delta.getY()).normalize(); // Ignore Y.

                            Vector result = new Vector(direction.getX() * force.getX(), force.getY(), direction.getZ() * force.getZ());
                            player.setVelocity(result);
                        }
                    }
                } else this.cancel();
            }

        }.runTaskTimer(TabletopGamesSpigot.get(), 5, 5);
    }

    public void setForce(Vector force) { this.force = force; }
}
