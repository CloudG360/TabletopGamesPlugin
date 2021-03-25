package me.cg360.games.tabletop.ngapimicro.rule.boundary.circular;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.RuleAbstractCircularBoundary;

public class RulePushIntoCircularBoundary extends RuleAbstractCircularBoundary {

    protected Vector3 force;

    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled) { this(origin, radius, areEdgeParticlesEnabled, null); }
    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled, Vector3 force) {
        super(origin, radius, areEdgeParticlesEnabled);
        this.force = force == null ? new Vector3(0.6f, 0.2f, 0.6f) : force;
    }

    @Override
    protected void onStartBoundaryWatchdog() {
        TabletopGamesNukkit.getScheduler().scheduleDelayedRepeatingTask(new Task() {

            @Override
            public void onRun(int currentTick) {

                if(watchdog.isRunning()) {

                    for(Player player: watchdog.getPlayers()) {

                        if(player.distance(origin) > radius) {
                            Vector3 delta = origin.getLocation().subtract(player.getLocation());
                            Vector3 direction = new Vector3(delta.getX(), 0, delta.getZ()).normalize(); // Ignore Y.

                            Vector3 result = new Vector3(direction.getX() * force.getX(), force.getY(), direction.getZ() * force.getZ());
                            player.setMotion(result);
                        }
                    }

                } else this.cancel();

            }

        }, 5, 10);
    }

    public void setForce(Vector3 force) { this.force = force; }
}
