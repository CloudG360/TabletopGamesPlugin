package me.cg360.games.tabletop.ngapimicro.rule;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import net.cg360.nsapi.commons.Check;

import java.util.HashMap;

public class RuleReleasePlayerOutsideRange extends WatchdogRule implements Listener {

    public static final double POINT_STEP = 2d / 3d;

    protected MicroGameWatchdog<?> watchdog;

    protected Location origin;
    protected double radius;
    protected boolean areEdgeParticlesEnabled;

    public RuleReleasePlayerOutsideRange(Location origin, double radius, boolean areEdgeParticlesEnabled){
        Check.nullParam(origin, "origin");

        this.origin = origin;
        this.areEdgeParticlesEnabled = areEdgeParticlesEnabled;
        this.setRadius(radius);

    }

    @Override
    protected void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesNukkit.get().getServer().getPluginManager().registerEvents(this, TabletopGamesNukkit.get());

        TabletopGamesNukkit.getScheduler().scheduleDelayedRepeatingTask(new Task() {

            @Override
            public void onRun(int currentTick) {

                if(watchdog.isRunning()) {

                    if(areEdgeParticlesEnabled && radius > 0){
                        double circ = Math.PI * (radius * 2);
                        int pointCount = (int) Math.floor(circ * POINT_STEP);
                        double angleStep = 360d / pointCount;

                        Vector3 forward = new Vector3(1, 0, 0);

                        for(double angle = 0; angle < 360d; angle += angleStep){
                            Vector3 direction = forward.multiply(Math.cos(angle)).normalize();
                            Vector3 radiusDelta = direction.multiply(radius);

                            Location loc = origin.getLocation().add(radiusDelta);
                            Entity.entityCount++; // Increment before using/
                            loc.getLevel().addParticleEffect(loc, ParticleEffect.REDSTONE_TORCH_DUST, Entity.entityCount, origin.getLevel().getDimension(), watchdog.getPlayers());
                        }
                    }

                } else this.cancel();

            }

        }, 1, 1);
    }

    @Override
    protected void onStopWatchdog() {
        HandlerList.unregisterAll(this);
    }



    public void setRadius(double radius) { this.radius = radius >= 0 ? radius : 0; }
    public void setRenderEdgeParticlesEnabled(boolean renderEdgeParticles) { this.areEdgeParticlesEnabled = renderEdgeParticles; }
}
