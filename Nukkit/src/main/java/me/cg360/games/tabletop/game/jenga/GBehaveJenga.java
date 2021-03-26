package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.game.jenga.entity.EntityJengaBlock;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import me.cg360.games.tabletop.ngapimicro.MicroGameBehaviour;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import me.cg360.games.tabletop.ngapimicro.rule.RuleAcquirePlayersFromRadius;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnQuit;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnWorldChange;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.circular.RulePushIntoCircularBoundary;
import me.cg360.games.tabletop.ngapimicro.rule.boundary.circular.RuleReleasePlayerOutsideCircularBoundary;
import net.cg360.nsapi.commons.data.Settings;

import java.text.DecimalFormat;
import java.util.*;

public class GBehaveJenga extends MicroGameBehaviour implements Listener {




    protected Settings initSettings;
    protected ArrayList<Player> players;

    protected Location origin;
    protected String inviteMessage;
    protected int inviteLengthTicks;

    protected RuleAcquirePlayersFromRadius recruitmentRule;
    protected RuleReleasePlayerOutsideCircularBoundary releaseRule;
    protected RulePushIntoCircularBoundary retentionRule;

    protected HashMap<String, Long> blockEntityIDs;


    @Override
    public void init(Settings settings) {

        // -- Basic inits --

        this.initSettings = settings;
        this.players = new ArrayList<>();
        this.blockEntityIDs = new HashMap<>();

        // -- Init settings + shortcuts --

        Settings properties = getWatchdog().getGameProfile().getProperties();
        Player host = settings.get(InitKeys.HOST_PLAYER);
        this.origin = settings.get(InitKeys.ORIGIN);

        if(origin == null) {
            if(host == null) throw new IllegalArgumentException("An origin (location) or a host (player) must be present to start Jenga.");
            this.origin = host.getLocation();
        }

        if (host != null) {
            MicroGameWatchdog.removePlayerFromGames(host);
            capturePlayer(host);
        }

        // -- Handle invite setup --

        this.inviteLengthTicks = Math.max(initSettings.getOrElse(InitKeys.INITIAL_INVITE_LENGTH, 200), 20);
        this.inviteMessage = host == null ?
                // Null player host.
                String.format("%sYou have been invited to a game of %s%s%s! Would you like to join? You have %s%s%ss to join.",
                        TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"),
                        TextFormat.BLUE, TextFormat.AQUA, new DecimalFormat("0.0").format(((float) inviteLengthTicks) / 20f), TextFormat.BLUE):
                // Player host, use their unformatted name.
                String.format("%s%s%s has invited you to a game of %s%s! %sWould you like to join? You have %s%s%ss to join.",
                        TextFormat.AQUA, host.getName(), TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"),
                        TextFormat.BLUE, TextFormat.AQUA, new DecimalFormat("0.0").format(((float) inviteLengthTicks) / 20f), TextFormat.BLUE);


        // -- Enable Events --
        TabletopGamesNukkit.get().getServer().getPluginManager().registerEvents(this, TabletopGamesNukkit.get());
    }



    @Override
    public WatchdogRule[] getRules() {
        double playAreaRadius = initSettings.getOrElse(InitKeys.PLAY_AREA_RADIUS, 15d);
        this.recruitmentRule = new RuleAcquirePlayersFromRadius(inviteMessage, origin, initSettings.getOrElse(InitKeys.INVITE_RADIUS, 10d), true);
        this.releaseRule = new RuleReleasePlayerOutsideCircularBoundary(origin, playAreaRadius + 10d, false); // Fallback border.
        this.retentionRule = new RulePushIntoCircularBoundary(origin, playAreaRadius, true);

        TabletopGamesNukkit.getScheduler().scheduleDelayedTask(TabletopGamesNukkit.get(), () -> {

            this.recruitmentRule.setEnabled(false); // Disable invites after invite interval, open for at least 1 second.
            if(getWatchdog().isRunning()) onFinishRecruitment(); // Check it's still running.

        }, inviteLengthTicks);

        return new WatchdogRule[] { // Order is important :)
                new RuleReleasePlayerOnQuit(),
                new RuleReleasePlayerOnWorldChange(),
                this.recruitmentRule,
                this.retentionRule,
                this.releaseRule
        };
    }

    @Override
    protected void onPlayerCapture(Player player) {
        players.add(player);
    }

    @Override
    protected void onPlayerRelease(Player player) {
        players.remove(player);
    }

    @Override
    protected void onFinish() {
        // Delete jenga entities.
    }



    protected void onFinishRecruitment() {

        JengaLayer lastLayer = new JengaLayer(origin, 0.5f, false);
        lastLayer.fillLayer();

        for(int i = 0; i < 9; i++) {
            JengaLayer newLayer = new JengaLayer(lastLayer);
            newLayer.fillLayer();
            lastLayer = newLayer;
        }

        /*
        spawnBlock(origin.getLocation().add(0, 0, -BLOCK_SCALE), false);
        spawnBlock(origin.getLocation().add(0, 0, 0), false);
        spawnBlock(origin.getLocation().add(0, 0, BLOCK_SCALE), false);

        spawnBlock(origin.getLocation().add(-BLOCK_SCALE, 1, 0), true);
        spawnBlock(origin.getLocation().add(0, 1, 0), true);
        spawnBlock(origin.getLocation().add(BLOCK_SCALE, 1, 0), true);
        */
    }

}
