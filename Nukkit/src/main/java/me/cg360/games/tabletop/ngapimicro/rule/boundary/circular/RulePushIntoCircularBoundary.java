package me.cg360.games.tabletop.ngapimicro.rule.boundary.circular;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.RuleAbstractCircularBoundary;

public class RulePushIntoCircularBoundary extends RuleAbstractCircularBoundary {

    protected Vector3 force;

    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled) { this(origin, radius, areEdgeParticlesEnabled, null); }
    public RulePushIntoCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled, Vector3 force) {
        super(origin, radius, areEdgeParticlesEnabled);
        this.force = force == null ? new Vector3(1.1f, 0.4f, 1.1f) : force;
    }

    @Override
    protected void onStartBoundaryWatchdog() {
        TabletopGamesNukkit.getScheduler().scheduleDelayedRepeatingTask(new Task() {

            @Override
            public void onRun(int currentTick) {

                if(watchdog.isRunning()) {

                    for(Player player: watchdog.getPlayers()) {
                        Vector2 player2D = new Vector2(player.getX(), player.getZ());
                        Vector2 origin2D = new Vector2(origin.getX(), origin.getZ());

                        if(player2D.distance(origin2D) > radius) {
                            Vector2 delta = origin2D.subtract(player2D);
                            Vector2 direction = new Vector2(delta.getX(), delta.getY()).normalize(); // Ignore Y.

                            Vector3 result = new Vector3(direction.getX() * force.getX(), force.getY(), direction.getY() * force.getZ());
                            player.setMotion(result);
                        }
                    }

                } else this.cancel();

            }

        }, 5, 5);
    }

    public void setForce(Vector3 force) { this.force = force; }
}
