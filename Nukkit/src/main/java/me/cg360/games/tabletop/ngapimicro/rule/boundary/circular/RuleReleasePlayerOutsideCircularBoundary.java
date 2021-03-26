package me.cg360.games.tabletop.ngapimicro.rule.boundary.circular;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.math.Vector2;
import cn.nukkit.scheduler.Task;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.RuleAbstractCircularBoundary;

public class RuleReleasePlayerOutsideCircularBoundary extends RuleAbstractCircularBoundary {

    public RuleReleasePlayerOutsideCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled) {
        super(origin, radius, areEdgeParticlesEnabled);
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
                            watchdog.releasePlayer(player);
                        }
                    }

                } else this.cancel();

            }

        }, 5, 10);
    }

}
