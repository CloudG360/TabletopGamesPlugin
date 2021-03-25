package me.cg360.games.tabletop.ngapimicro.rule;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import net.cg360.nsapi.commons.Check;

import java.text.DecimalFormat;
import java.util.HashMap;

public class RuleReleasePlayerOutsideRange extends WatchdogRule implements Listener {

    public static final double POINT_STEP = 0.6;

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

                        for(double angle = 0d; angle < 360d; angle += angleStep){
                            double angleRadians = Math.toRadians(angle);
                            Vector3 direction = new Vector3(Math.sin(angleRadians), 0, Math.cos(angleRadians));
                            Vector3 radiusDelta = direction.multiply(radius);

                            Location loc = origin.getLocation().add(radiusDelta);
                            loc.getLevel().addParticle(new RedstoneParticle(loc.add(0, 0.5, 0), 20), watchdog.getPlayers());
                            loc.getLevel().addParticle(new RedstoneParticle(loc.add(0, 1, 0), 20), watchdog.getPlayers());
                            loc.getLevel().addParticle(new RedstoneParticle(loc.add(0, 1.5, 0), 20), watchdog.getPlayers());
                            loc.getLevel().addParticle(new RedstoneParticle(loc.add(0, 2, 0), 20), watchdog.getPlayers());
                        }
                    }

                } else this.cancel();

            }

        }, 5, 14);
    }

    @Override
    protected void onStopWatchdog() {
        HandlerList.unregisterAll(this);
    }



    public void setRadius(double radius) { this.radius = radius >= 0 ? radius : 0; }
    public void setRenderEdgeParticlesEnabled(boolean renderEdgeParticles) { this.areEdgeParticlesEnabled = renderEdgeParticles; }
}
