package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.game.jenga.entity.EntityJengaBlockCollider;
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

    public static final double EXPLODE_MULTIPLIER = 1.2f;

    // Used as a multiplier for each section. Values below 1 bring it closer, values above push it further
    // TODO: These values make more sense now. Work on tweaking them.
    public static final float TOP_FALL_TWEAK = 0.85f;
    public static final float CURRENT_FALL_TWEAK = 1.04f;
    public static final float BOTTOM_FALL_TWEAK = 1f;

    // Reduces the weight of any center block based removals.
    public static final float CENTER_BLOCK_TWEAK = 0.9f;

    // The highest value a layer integrity calculation can go. Above 1 allows the tower to become more stable.
    public static final float LAYER_THRESHOLD = 1.1f;
    // How much can each block randomly vary by in stability.
    public static final float BLOCK_VARIATION_SCALE = 0.24f;
    // How likely is the block variation to restore some integrity.
    public static final float BLOCK_VARIATION_INTEGRITY_RESTORE = 0.2f;

    // When a layer only has one block (Center), what's it's stability score.
    public static final float SINGLE_CENTER_STABILITY = 0.68f;
    // When a layer only has 2 blocks, what's it's stability score.
    public static final float DUO_LAYER_STABILITY = 0.87f;


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
            currentLayer.emptyLayer();

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

        float integrity = calculateRemovalIntegrity(layersBelow, posInLayer) * 2f;


        Optional<JengaLayer> currentLayer = Optional.ofNullable(topTowerLayer);
        // Get the layer selected.
        while (currentLayer.isPresent() && (currentLayer.get().getLayersBelowCount() != layersBelow)) {
            JengaLayer c = currentLayer.get();
            currentLayer = c.getLayerBelow(); // Switch out layer for next loop
        }

        if(!currentLayer.isPresent()) throw new IllegalStateException("Layer of block specified was not present.");
        JengaLayer blockLayer = currentLayer.get();


        // Check if the block actually exists (It always should)
        // Then delete it and add a new one to the top.
        // This if statement manages stuff without the context of a toppling tower :)
        if(!attacker.isSneaking()) { // TODO: Temporary! Use items in the hotbar instead.
            switch (posInLayer) {
                case 0:
                    if (!blockLayer.hasLeft()) return;
                    blockLayer.despawnLeft();
                    break;

                case 1:
                    if (!blockLayer.hasCenter()) return;
                    blockLayer.despawnCenter();
                    break;

                case 2:
                    if (!blockLayer.hasRight()) return;
                    blockLayer.despawnRight();
                    break;
            }

            attacker.getLevel().addSound(attacker.getPosition(), Sound.NOTE_SNARE);
            for(Player player: players) {
                player.sendMessage(Util.fMessage("JENGA", TextFormat.GOLD, String.format("%s%s took a block from the tower!", attacker.getName(), TextFormat.GRAY)));
            }

            // Attempt to add a block. If it isn't added, add a new layer.
            if(!addBlockToTop()) {
                JengaLayer oldLayer = topTowerLayer;
                topTowerLayer = new JengaLayer(oldLayer);
                addBlockToTop();
            }
        }

        // If integrity is below 1, do a random check to see if the tower falls.
        if(integrity <= 1f){
            float chance = 1f - integrity;
            attacker.sendMessage(Util.fMessage("JENGA", TextFormat.GOLD, "Fall Chance: " + TextFormat.GOLD + new DecimalFormat("0.0").format(chance  * 100f) + "%"));

            if(!attacker.isSneaking()) {  // TODO: Temporary! Use items in the hotbar instead.
                float roll = new Random().nextFloat();

                // If tower toppled: explode :)
                if(roll <= chance) {
                    for(Player p: players) p.sendMessage(Util.fMessage("UH OH!", TextFormat.DARK_RED, String.format("%s%s toppled the tower!", attacker.getName(), TextFormat.GRAY)));
                    attacker.getLevel().addSound(origin, Sound.RANDOM_EXPLODE);
                    attacker.getLevel().addParticleEffect(origin.add(0, 2,0), ParticleEffect.HUGE_EXPLOSION_LEVEL);


                    Optional<JengaLayer> l = Optional.ofNullable(topTowerLayer);

                    while (l.isPresent()) {
                        JengaLayer c = l.get();

                        c.getLeft().ifPresent(this::applyExplosionVelocity);
                        c.getCenter().ifPresent(this::applyExplosionVelocity);
                        c.getRight().ifPresent(this::applyExplosionVelocity);

                        l = c.getLayerBelow(); // Switch out layer for next loop
                    }

                    // Delay the game end so the entities last enough to show the explosion.
                    TabletopGamesNukkit.getScheduler().scheduleDelayedTask(TabletopGamesNukkit.get(), () -> {
                        if(TabletopGamesNukkit.isRunning()) {
                            this.getWatchdog().stopGame();
                        }
                    }, 25);
                }
            }

        } else attacker.sendMessage(Util.fMessage("JENGA", TextFormat.GOLD, "Fall Chance: " + TextFormat.GOLD + "0.0%"));

    }

    // It's important this isn't infinitely recursive, else a EmulatedJengaLayer could
    // crash the server running this plugin. :)
    /** @return true if a block was added to the top layer. */
    protected boolean addBlockToTop() {

        if(!topTowerLayer.hasLeft()) {
            topTowerLayer.spawnLeft();
            return true;
        }

        if(!topTowerLayer.hasCenter()) {
            topTowerLayer.spawnCenter();
            return true;
        }

        if(!topTowerLayer.hasRight()) {
            topTowerLayer.spawnRight();
            return true;
        }

        return false;
    }

    protected void applyExplosionVelocity(EntityVisualJengaBlock b) {
        Random r = new Random();
        // Remove colliders and send block in a direction.
        for(EntityJengaBlockCollider collide: new ArrayList<>(b.getColliders())) {
            collide.close();
            b.getColliders().remove(collide);
        }

        // Get delta, add variation and normalize.
        double varX = (r.nextDouble() - 0.5d);
        double varZ = (r.nextDouble() - 0.5d);
        Vector3 delta = b.getLocation().subtract(origin).add(varX, 0, varZ);
        Vector3 direction = new Vector3(delta.getX(), 0, delta.getZ()).normalize().add(0, 0.3d, 0);

        Vector3 velocity = direction.multiply(EXPLODE_MULTIPLIER * b.getScale());
        b.setCustomPhysicsEnabled(true);
        b.setMotion(velocity);
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
        int blocksPresent = 0;
        float[] baseStability = new float[]{ 1.0f, 1.0f, 1.0f, 1.0f };
        if(layer.hasLeft()) blocksPresent++;
        if(layer.hasCenter()) blocksPresent++;
        if(layer.hasRight()) blocksPresent++;

        // Rule 0: If a layer is empty, it's completely unstable :D
        if(blocksPresent == 0) return new float[]{ 0.0f, 0.0f, 0.0f, 0.0f};

        // Rule 1: If only one block is present, it ***must*** be the center block else gravity happens.
        // Weaken the center if so
        if(blocksPresent == 1) {
            if (layer.hasLeft() || layer.hasRight()){
                return new float[]{0.0f, 0.0f, 0.0f, 0.0f};

            } else{

                if (layer.isAxisAlternate()) {
                    baseStability[2] *= SINGLE_CENTER_STABILITY; // Blocks stretch across the tower along axis Z. Destabilize X as there's no blocks on each edge.
                    baseStability[3] *= SINGLE_CENTER_STABILITY;

                } else {
                    baseStability[0] *= SINGLE_CENTER_STABILITY; // Blocks stretch across the tower along axis X. Destabilize Z as there's no blocks on each edge.
                    baseStability[1] *= SINGLE_CENTER_STABILITY;
                }
            }

        // Rule 2: If 2 blocks are present, make it more unstable under the condition that it's the left or the right one missing.
        // The center should be unaffected.
        } else if (blocksPresent == 2){

            // Missing lowest most block of an axis.
            if(!layer.hasLeft()) {
                // 2 = West; 0 = North
                baseStability[layer.isAxisAlternate() ? 2 : 0] *= DUO_LAYER_STABILITY;

            } else if(!layer.hasRight()) {
                // 3 = East; 1 = South
                baseStability[layer.isAxisAlternate() ? 3 : 1] *= DUO_LAYER_STABILITY;
            }
        }

        // Rule 3: Layers have a fixed random variation to determine stability.
        // Seed depends on the layer's depth (+ 1 to avoid a seed of 0) along with the tower's uuid.
        // Limit variation's scope by multiplying it by a small number and applying it as (1 - variation)
        // The right variation if shuffled along by me pressing a few random numbers to offset it a bit :D
        float variationLeft =  BLOCK_VARIATION_SCALE * (new Random((layer.getLayersBelowCount()) * layer.getTowerUUID().getLeastSignificantBits()).nextFloat() - BLOCK_VARIATION_INTEGRITY_RESTORE);
        float variationRight = BLOCK_VARIATION_SCALE * (new Random((layer.getLayersBelowCount() * 1234L) + ((1 + layer.getLayersBelowCount()) * layer.getTowerUUID().getLeastSignificantBits())).nextFloat() - BLOCK_VARIATION_INTEGRITY_RESTORE);
        if(layer.isAxisAlternate()) {
            baseStability[2] *= (1 - variationLeft);
            baseStability[3] *= (1 - variationRight);

        } else {
            baseStability[0] *= (1 - variationLeft);
            baseStability[1] *= (1 - variationRight);
        }

        return baseStability;
    }

    // Calculated the integrity if the block indicated in the coordinates was removed.
    protected float calculateRemovalIntegrity(int layersBelow, int posInLayer) {
        float northCumulativeIntegrity = 1f;
        float southCumulativeIntegrity = 1f;
        float westCumulativeIntegrity = 1f;
        float eastCumulativeIntegrity = 1f;

        Optional<JengaLayer> currentLayer = Optional.ofNullable(topTowerLayer); // Top layer is immune.

        // While there are still layers below and the layer isn't the one we're looking for.
        while (currentLayer.isPresent() && (currentLayer.get().getLayersBelowCount() != layersBelow)) {
            JengaLayer c = currentLayer.get();

            // Take integrity and apply it to the cumulative totals.
            // Current Rules:
            // - Keep below the LAYER THRESHOLD
            // - Don't drag anything wiith a value of 0 or below.
            // - Drag center blocks closer to 1f of stability
            if(c != topTowerLayer) {
                float[] integrity = calculateSingleLayerIntegrity(c);

                northCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(integrity[0], TOP_FALL_TWEAK, posInLayer)
                );
                southCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(integrity[1], TOP_FALL_TWEAK, posInLayer)
                );
                westCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(integrity[2], TOP_FALL_TWEAK, posInLayer)
                );
                eastCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(integrity[3], TOP_FALL_TWEAK, posInLayer)
                );
            }

            currentLayer = c.getLayerBelow(); // Switch out layer for next loop
        }


        if(currentLayer.isPresent()) { // Found the layer with the block!
            JengaLayer real = currentLayer.get();

            // Set to false if it matches the block's posInLayer. Otherwise use the actual value.
            boolean hasLeft = (posInLayer != 0) && real.hasLeft();
            boolean hasCenter = (posInLayer != 1) && real.hasCenter();
            boolean hasRight = (posInLayer != 2) && real.hasRight();

            // Fake jenga layer to simulate how the tower would react if a block was removed.
            EmulatedJengaLayer emu = new EmulatedJengaLayer(real, hasLeft, hasCenter, hasRight);

            // The top layer shouldn't affect integrity.
            if(real != topTowerLayer){
                float[] blockIntegrity = calculateSingleLayerIntegrity(emu); // Take integrity and apply it to the cumulative totals.

                // Current Rules:
                // - Keep below the LAYER THRESHOLD
                // - Don't drag anything wiith a value of 0 or below.
                // - Drag center blocks closer to 1f of stability
                northCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(blockIntegrity[0], CURRENT_FALL_TWEAK, posInLayer)
                );
                southCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(blockIntegrity[1], CURRENT_FALL_TWEAK, posInLayer)
                );
                westCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(blockIntegrity[2], CURRENT_FALL_TWEAK, posInLayer)
                );
                eastCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(blockIntegrity[3], CURRENT_FALL_TWEAK, posInLayer)
                );
            }


            // Review layers below and apply their integrity checks.
            Optional<JengaLayer> nextLayer = real.getLayerBelow();

            while (nextLayer.isPresent()) {
                JengaLayer c = nextLayer.get();

                // Take integrity and apply it to the cumulative totals.
                float[] bottomIntegrity = calculateSingleLayerIntegrity(c);

                // Current Rules:
                // - Keep below the LAYER THRESHOLD
                // - Don't drag anything wiith a value of 0 or below.
                // - Drag center blocks closer to 1f of stability
                northCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(bottomIntegrity[0], BOTTOM_FALL_TWEAK, posInLayer)
                );
                southCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(bottomIntegrity[1], BOTTOM_FALL_TWEAK, posInLayer)
                );
                westCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(bottomIntegrity[2], BOTTOM_FALL_TWEAK, posInLayer)
                );
                eastCumulativeIntegrity *= Math.min(
                        LAYER_THRESHOLD,
                        genTweakedValue(bottomIntegrity[3], BOTTOM_FALL_TWEAK, posInLayer)
                );

                nextLayer = c.getLayerBelow(); // Switch out layer for next loop
            }


            float[] finalIntegrity = new float[] { northCumulativeIntegrity, southCumulativeIntegrity, westCumulativeIntegrity, eastCumulativeIntegrity, 1.0f };
            int indexOfConcern = -1; // -1 means index not found. Shouldn't be possible to retain this.

            // Determine which index of finalIntegrity the block removal should
            // be affected by.
            if(emu.isAxisAlternate()) {
                switch (posInLayer) {
                    case 0:
                        indexOfConcern = 2;
                        break;
                    case 1:
                        // Create an average of both polar axis values. Then store it in index 4.
                        finalIntegrity[4] = (finalIntegrity[2] + finalIntegrity[3]) / 2f;
                        indexOfConcern = 4;
                        break;
                    case 2:
                        indexOfConcern = 3;
                        break;
                }
            } else {
                switch (posInLayer) {
                    case 0:
                        indexOfConcern = 0;
                        break;
                    case 1:
                        // Create an average of both polar axis values. Then store it in index 4.
                        finalIntegrity[4] = (finalIntegrity[0] + finalIntegrity[1]) / 2f;
                        indexOfConcern = 4;
                        break;
                    case 2:
                        indexOfConcern = 1;
                        break;
                }
            }

            if(indexOfConcern == -1) throw new IllegalStateException("Block's calculation index is weirdchamp. Ensure 'posInLayer' is 0, 1, or 2.");

            return finalIntegrity[indexOfConcern];
        }
        throw new IllegalStateException("Layer of block specified was not in the tower during physics calculation."); // Block was not in the tower? :)))))
    }

    protected float genTweakedValue(float integrity, float layerTweak, int blockIndex) {
        float centerMultiplier = (integrity > 0f) && (blockIndex == 1) ? CENTER_BLOCK_TWEAK : 1f;
        return multiplyFromMidpoint(1f, integrity, centerMultiplier * layerTweak);
    }

    /** Multiplies a value to a defined midpoint with a multiplier. */
    protected float multiplyFromMidpoint(float midpoint, float value, float multiplier) {
        float delta = value - midpoint;
        float tweaked = delta * multiplier;
        return midpoint + tweaked;
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
