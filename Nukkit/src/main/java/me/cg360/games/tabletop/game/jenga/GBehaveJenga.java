package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.*;
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

    protected JengaLayer topTowerLayer;


    @Override
    public void init(Settings settings) {

        // -- Basic inits --

        this.initSettings = settings;
        this.players = new ArrayList<>();

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

        this.topTowerLayer =  new JengaLayer(origin, 1f/3, false);
        topTowerLayer.fillLayer();

        for(int i = 0; i < 9; i++) {
            JengaLayer newLayer = new JengaLayer(topTowerLayer);
            newLayer.fillLayer();
            this.topTowerLayer = newLayer;
        }
    }

    protected void validBlockHit(EntityDamageByEntityEvent event, int layersBelow, int posInLayer, boolean isAlternateLayer) {

    }

    @EventHandler
    public void onBlockDamage(EntityDamageByEntityEvent event) {

        if((event.getDamager() instanceof Player) && (event.getEntity() instanceof EntityJengaBlock)) {
            Player player = (Player) event.getDamager();
            EntityJengaBlock jengaBlock = (EntityJengaBlock) event.getEntity();

            if(players.contains(player) && (jengaBlock.namedTag != null)) {
                CompoundTag nbtTag = jengaBlock.namedTag;
                CompoundTag towerTag = nbtTag.getCompound(JengaLayer.NBT_COMPOUND_TOWER);

                if(towerTag != null) {
                    String uuid = towerTag.getString(JengaLayer.NBT_TOWER_UUID);

                    // Check UUID is valid/present + check it's the same as this game's tower.
                    if( ((uuid != null) && (uuid.length() != 0)) && uuid.equals(topTowerLayer.getTowerUUID().toString()) ) {
                        Tag layersBelowTag = towerTag.get(JengaLayer.NBT_LAYERS_BELOW_COUNT);
                        Tag posInLayerTag = towerTag.get(JengaLayer.NBT_POS_IN_LAYER);
                        Tag isAlternateLayerTag = towerTag.get(JengaLayer.NBT_LAYER_ALTERNATE_AXIS);

                        //Ensure they all valid.
                        if(             (layersBelowTag instanceof IntTag)
                                &&  (posInLayerTag instanceof IntTag)
                                && (isAlternateLayerTag instanceof ByteTag) ){

                            int layersBelow = ((IntTag) layersBelowTag).getData();
                            int posInLayer = ((IntTag) posInLayerTag).getData();
                            boolean isAlternateLayer = ((ByteTag) isAlternateLayerTag).getData() == 1;
                            validBlockHit(event, layersBelow, posInLayer, isAlternateLayer);
                        }
                    }
                }
            }
        }
    }

}
