package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.PlayerListPacket;
import cn.nukkit.network.protocol.PlayerSkinPacket;
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
import net.cg360.nsapi.commons.data.Settings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

public class GBehaveJenga extends MicroGameBehaviour implements Listener {

    protected static final String PERSISTANT_UUID_KEY = "human_uuid";
    public static final float BLOCK_SCALE = 1f;

    protected Settings initSettings;
    protected ArrayList<Player> players;

    protected Location origin;
    protected String inviteMessage;
    protected int inviteLengthTicks;

    protected RuleAcquirePlayersFromRadius recruitmentRule;

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
        this.recruitmentRule = new RuleAcquirePlayersFromRadius(inviteMessage, origin, initSettings.getOrElse(InitKeys.INVITE_RADIUS, 10d), true);

        TabletopGamesNukkit.getScheduler().scheduleDelayedTask(TabletopGamesNukkit.get(), () -> {

            this.recruitmentRule.setEnabled(false); // Disable invites after invite interval, open for at least 1 second.
            if(getWatchdog().isRunning()) onFinishRecruitment(); // Check it's still running.

        }, inviteLengthTicks);

        return new WatchdogRule[] {
                new RuleReleasePlayerOnQuit(), // This one is important :)
                new RuleReleasePlayerOnWorldChange(),
                this.recruitmentRule
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
        spawnBlock(origin.getLocation().add(0, 0, -BLOCK_SCALE));
        spawnBlock(origin.getLocation());
        spawnBlock(origin.getLocation().add(0, 0, BLOCK_SCALE));
    }

    /** @return the id of the spawned block.*/
    protected String spawnBlock(Location position) {
        UUID uniqueID = UUID.randomUUID();
        String uuid = uniqueID.toString();

        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", position.getX()))
                        .add(new DoubleTag("", position.getY()))
                        .add(new DoubleTag("", position.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", 0f))
                        .add(new FloatTag("", 0f)))
                .putBoolean("npc", true)
                .putFloat("scale", BLOCK_SCALE)
                .putString(PERSISTANT_UUID_KEY, uuid);
        nbt.putBoolean("ishuman", true);

        FullChunk chunk = position.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
        EntityJengaBlock jengaHuman = new EntityJengaBlock(chunk, nbt);

        jengaHuman.setPositionAndRotation(position, 0, 0);
        jengaHuman.setImmobile(true);
        jengaHuman.setNameTagAlwaysVisible(false);
        jengaHuman.setNameTagVisible(false);
        jengaHuman.setNameTag(uuid);
        jengaHuman.setScale(BLOCK_SCALE);

        blockEntityIDs.put(uuid, jengaHuman.getId());
        jengaHuman.spawnToAll();

        return uuid;
    }



    @EventHandler(priority = EventPriority.HIGHEST) // Should be *sure* the chunk isn't being unloaded.
    public void onEntitySpawn(EntitySpawnEvent event) {
        CompoundTag nbt = event.getEntity().namedTag;

        if((nbt != null)) {
            String uuid = nbt.getString(PERSISTANT_UUID_KEY);

            if((uuid != null) && (uuid.length() != 0)) {

                if(blockEntityIDs.containsKey(uuid)) {
                    blockEntityIDs.put(uuid, event.getEntity().getId()); // Update entity ID in the case it's somehow overriden?
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Should be *sure* the chunk isn't being unloaded.
    public void onChunkUnload(ChunkUnloadEvent event) {

        for (Entity entity: new ArrayList<>(event.getChunk().getEntities().values())) {
            CompoundTag nbt = entity.namedTag;

            if((nbt != null)) {
                String uuid = nbt.getString(PERSISTANT_UUID_KEY);

                if((uuid != null) && (uuid.length() != 0)) {

                    if(blockEntityIDs.containsKey(uuid)) {
                        entity.close();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Cancel damage at the last minute :)
    public void onDamage(EntityDamageEvent event) {
        CompoundTag nbt = event.getEntity().namedTag;

        if((nbt != null)) {
            String uuid = nbt.getString(PERSISTANT_UUID_KEY);

            if((uuid != null) && (uuid.length() != 0)) {

                if(blockEntityIDs.containsKey(uuid)) {
                    event.setCancelled(true);
                }
            }
        }
    }


}
