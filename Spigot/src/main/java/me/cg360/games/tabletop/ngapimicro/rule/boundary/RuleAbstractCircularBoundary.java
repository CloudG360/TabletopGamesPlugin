package me.cg360.games.tabletop.ngapimicro.rule.boundary;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import net.cg360.nsapi.commons.Check;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public abstract class RuleAbstractCircularBoundary extends WatchdogRule implements Listener {

    public static final double POINT_STEP = 0.6;

    protected MicroGameWatchdog<?> watchdog;

    protected Location origin;
    protected double radius;
    protected boolean areEdgeParticlesEnabled;

    public RuleAbstractCircularBoundary(Location origin, double radius, boolean areEdgeParticlesEnabled){
        Check.nullParam(origin, "origin");
        Check.nullParam(origin.getWorld(), "origin.world");

        this.origin = origin;
        this.areEdgeParticlesEnabled = areEdgeParticlesEnabled;
        this.setRadius(radius);
    }

    @Override
    protected final void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesSpigot.get().getServer().getPluginManager().registerEvents(this, TabletopGamesSpigot.get());

        new BukkitRunnable() {

            @Override
            public void run() {
                if(watchdog.isRunning()) {

                    if(areEdgeParticlesEnabled && radius > 0){
                        double circ = Math.PI * (radius * 2);
                        int pointCount = (int) Math.floor(circ * POINT_STEP);
                        double angleStep = 360d / pointCount;

                        for(double angle = 0d; angle < 360d; angle += angleStep){
                            double angleRadians = Math.toRadians(angle);
                            Vector direction = new Vector(Math.sin(angleRadians), 0, Math.cos(angleRadians));
                            Vector radiusDelta = direction.multiply(radius);

                            Location loc = new Location(origin.getWorld(), origin.getX(), origin.getY(), origin.getZ()).add(radiusDelta);
                            if(loc.getWorld() == null) throw new IllegalStateException("Origin world is null?!"); // silence errors!

                            Location loc1 = loc.add(0, 0.5d, 0);
                            Location loc2 = loc.add(0, 1.0d, 0);
                            Location loc3 = loc.add(0, 1.5d, 0);
                            Location loc4 = loc.add(0, 2.0d, 0);

                            for (Player player: watchdog.getPlayers()) {
                                player.spawnParticle(Particle.REDSTONE, loc1, 1);
                                player.spawnParticle(Particle.REDSTONE, loc2, 1);
                                player.spawnParticle(Particle.REDSTONE, loc3, 1);
                                player.spawnParticle(Particle.REDSTONE, loc4, 1);
                            }
                        }
                    }
                } else this.cancel();
            }

        }.runTaskTimer(TabletopGamesSpigot.get(), 5, 14);

        onStartBoundaryWatchdog();
    }

    @Override
    protected final void onStopWatchdog() {
        HandlerList.unregisterAll(this);
        onStopBoundaryWatchdog();
    }

    // \/\/\/ Ensures the base behaviours don't change but offers a way to add onto the behaviour
    protected void onStartBoundaryWatchdog() { }
    protected void onStopBoundaryWatchdog() { }


    public final void setRadius(double radius) { this.radius = radius >= 0 ? radius : 0; }
    public final void setRenderEdgeParticlesEnabled(boolean renderEdgeParticles) { this.areEdgeParticlesEnabled = renderEdgeParticles; }
}
