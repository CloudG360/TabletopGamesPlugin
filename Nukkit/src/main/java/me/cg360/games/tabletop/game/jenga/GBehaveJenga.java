package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.game.jenga.entity.EntityVisualJengaBlock;
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
        Optional<JengaLayer> nextLayer = Optional.ofNullable(topTowerLayer);

        while (nextLayer.isPresent()) {
            JengaLayer currentLayer = nextLayer.get();

            currentLayer.getLeft().ifPresent(EntityVisualJengaBlock::close);
            currentLayer.getCenter().ifPresent(EntityVisualJengaBlock::close);
            currentLayer.getRight().ifPresent(EntityVisualJengaBlock::close);

            nextLayer = currentLayer.getLayerBelow();
        }
    }



    protected void onFinishRecruitment() {

        this.topTowerLayer =  new JengaLayer(origin, 2/3f, false);
        topTowerLayer.fillLayer();

        for(int i = 0; i < 9; i++) {
            JengaLayer newLayer = new JengaLayer(topTowerLayer);
            newLayer.fillLayer();
            this.topTowerLayer = newLayer;
        }
    }

    protected void validBlockHit(EntityDamageByEntityEvent event, Player attacker, int layersBelow, int posInLayer, boolean isAlternateLayer) {
        if(initSettings.getOrElse(InitKeys.DEBUG_MODE_ENABLED, false)) {
            attacker.sendMessage(Util.fMessage("DEBUG", TextFormat.GOLD, "Block Hit! Here's some debug info!"));

            attacker.sendMessage(String.format("%sTower UUID: %s%s", TextFormat.GRAY, TextFormat.GOLD, topTowerLayer.getTowerUUID().toString()));
            attacker.sendMessage(String.format("%sTower Scale: %s%s", TextFormat.GRAY, TextFormat.GOLD, topTowerLayer.getScale()));

            attacker.sendMessage(String.format("%sLayers below this block: %s%s", TextFormat.GRAY, TextFormat.GOLD, layersBelow));
            attacker.sendMessage(String.format("%sPosition within its layer: %s%s", TextFormat.GRAY, TextFormat.GOLD, posInLayer));
            attacker.sendMessage(String.format("%sIs Layer 'Alternate'?: %s%s", TextFormat.GRAY, TextFormat.GOLD, isAlternateLayer ? "Yes!" : "No."));
        }
    }

    // NORTH = -Z
    // SOUTH = +Z
    // WEST =  -X
    // EAST  = +X

    // Assumptions made in calculations:
    // - Each layer of the tower alternates in axis.
    // - The floor below is stable (Calculating for each layer and merging fixes this ofc)

    // Stored { NORTH, SOUTH, WEST, EAST }
    protected float[] calculateSingleLayerIntegrity(JengaLayer layer) {
        int layersPresent = 0;
        float[] baseStability = new float[]{ 1.0f, 1.0f, 1.0f, 1.0f };
        if(layer.hasLeft()) layersPresent++;
        if(layer.hasCenter()) layersPresent++;
        if(layer.hasRight()) layersPresent++;

        // Rule 0: If a layer is empty, it's completely unstable :D
        if(layersPresent == 0) return new float[]{ 0.0f, 0.0f, 0.0f, 0.0f};

        // Rule 1: If only one block is present, it ***must*** be the center block else gravity happens.
        if((layersPresent == 1) && (layer.hasLeft() || layer.hasRight())) {
            return new float[] { 0.0f, 0.0f, 0.0f, 0.0f };

        } else {

            if(layer.isAxisAlternate()) {
                baseStability[2] *= 0.86f; // Blocks stretch across the tower along axis Z. Destabilize X as there's no blocks on each edge.
                baseStability[3] *= 0.86f;

            } else {
                baseStability[0] *= 0.86f; // Blocks stretch across the tower along axis X. Destabilize Z as there's no blocks on each edge.
                baseStability[1] *= 0.86f;
            }
        }

        // Rule 2: Layers have a fixed random variation to determine stability.
        // Seed depends on the layer's depth (+ 1 to avoid a seed of 0) along with the tower's uuid.
        // Limit variation's scope by multiplying it by a small number and applying it as (1 - variation)
        // The right variation if shuffled along by me pressing a few random numbers to offset it a bit :D
        float variationLeft = 0.1f * new Random((1 + layer.getLayersBelowCount()) * layer.getTowerUUID().getLeastSignificantBits()).nextFloat();
        float variationRight = 0.1f * new Random(28351 + ((1 + layer.getLayersBelowCount()) * layer.getTowerUUID().getLeastSignificantBits())).nextFloat();
        if(layer.isAxisAlternate()) {
            baseStability[2] *= (1 - variationLeft);
            baseStability[3] *= (1 - variationRight);

        } else {
            baseStability[0] *= (1 - variationLeft);
            baseStability[1] *= (1 - variationRight);
        }


        return baseStability;
    }

    protected float calculateRemovalIntegrity(int layersBelow, int posInLayer, boolean alternateLayer) {
        float northIntegrity = 1f;
        float southIntegrity = 1f;
        float eastIntegrity = 1f;
        float westIntegrity = 1f;

        for()

        return 1.0f;
    }



    @EventHandler
    public void onBlockDamage(EntityDamageByEntityEvent event) {

        if((event.getDamager() instanceof Player) && (event.getEntity() instanceof EntityVisualJengaBlock)) {
            Player player = (Player) event.getDamager();
            EntityVisualJengaBlock jengaBlock = (EntityVisualJengaBlock) event.getEntity();

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
                            validBlockHit(event, player, layersBelow, posInLayer, isAlternateLayer);
                        }
                    }
                }
            }
        }
    }

}
