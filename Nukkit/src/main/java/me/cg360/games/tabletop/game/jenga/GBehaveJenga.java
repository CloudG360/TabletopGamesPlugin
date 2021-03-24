package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import me.cg360.games.tabletop.ngapimicro.MicroGameBehaviour;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import me.cg360.games.tabletop.ngapimicro.rule.RuleAcquirePlayersFromRadius;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnQuit;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnWorldChange;
import net.cg360.nsapi.commons.Check;
import net.cg360.nsapi.commons.data.Settings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class GBehaveJenga extends MicroGameBehaviour {

    protected static final String PERSISTANT_UUID_KEY = "human_uuid";
    public static final float BLOCK_SCALE = 1f;

    protected Settings initSettings;
    protected ArrayList<Player> players;

    protected Location origin;
    protected String inviteMessage;
    protected int inviteLengthTicks;

    protected RuleAcquirePlayersFromRadius recruitmentRule;

    protected Skin jengaBlockSkin = null;
    protected ArrayList<String> persistentBlockIDs;


    @Override
    public void init(Settings settings) {

        // -- Basic inits --

        this.initSettings = settings;
        this.players = new ArrayList<>();
        this.persistentBlockIDs = new ArrayList<>();

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

        this.inviteLengthTicks = Math.max(initSettings.getOrElse(InitKeys.INITIAL_INVITE_LENGTH, 400), 20);
        this.inviteMessage = host == null ?
                // Null player host.
                String.format("%sYou have been invited to a game of %s%s%s! Would you like to join? You have %s%s%ss to join.",
                        TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"),
                        TextFormat.BLUE, TextFormat.AQUA, new DecimalFormat("0.0").format(((float) inviteLengthTicks) / 20f), TextFormat.BLUE):
                // Player host, use their unformatted name.
                String.format("%s%s%s has invited you to a game of %s%s! %sWould you like to join? You have %s%s%ss to join.",
                        TextFormat.AQUA, host.getName(), TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"),
                        TextFormat.BLUE, TextFormat.AQUA, new DecimalFormat("0.0").format(((float) inviteLengthTicks) / 20f), TextFormat.BLUE);


        // -- Generate Skin --

        try {
            this.jengaBlockSkin = new Skin();
            InputStream skinGeoIn = TabletopGamesNukkit.get().getResource("jenga/jenga_block.json");
            InputStream skinDataIn = TabletopGamesNukkit.get().getResource("jenga/jenga_block.png");

            BufferedReader read = new BufferedReader(new InputStreamReader(skinGeoIn));
            Iterator<String> i = read.lines().iterator();
            String geoStr = "";

            while (i.hasNext()) {
                geoStr = geoStr.concat(i.next());
                if(i.hasNext()) geoStr = geoStr.concat("\n"); // Add a newline unless the end has been reached.
            }

            BufferedImage skinData = ImageIO.read(skinDataIn);

            this.jengaBlockSkin.setArmSize("wide");
            this.jengaBlockSkin.setTrusted(true);
            this.jengaBlockSkin.setGeometryData(geoStr);
            this.jengaBlockSkin.setGeometryName("geometry.game.jenga_block");
            this.jengaBlockSkin.setSkinData(skinData);
            this.jengaBlockSkin.generateSkinId("JengaBlock");

        } catch (IOException err) {
            throw new IllegalStateException("Unable to load Jenga block model from resources");
        }
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
        spawnBlock(origin.getLocation());
    }

    /** @return the id of the spawned block.*/
    protected String spawnBlock(Location position) {
        String uuid = UUID.randomUUID().toString();

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
                        .add(new FloatTag("", (float) position.getYaw()))
                        .add(new FloatTag("", (float) position.getPitch())))
                .putBoolean("npc", true)
                .putFloat("scale", BLOCK_SCALE)
                .putString(PERSISTANT_UUID_KEY, uuid);
        CompoundTag skinDataTag = new CompoundTag()
                .putByteArray("Data", jengaBlockSkin.getSkinData().data)
                .putInt("SkinImageWidth", jengaBlockSkin.getSkinData().width)
                .putInt("SkinImageHeight", jengaBlockSkin.getSkinData().height)
                .putString("ModelId", jengaBlockSkin.getSkinId())
                .putString("CapeId", jengaBlockSkin.getCapeId())
                .putByteArray("CapeData", jengaBlockSkin.getCapeData().data)
                .putInt("CapeImageWidth", jengaBlockSkin.getCapeData().width)
                .putInt("CapeImageHeight", jengaBlockSkin.getCapeData().height)
                .putByteArray("SkinResourcePatch", jengaBlockSkin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", jengaBlockSkin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", jengaBlockSkin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", jengaBlockSkin.isPremium())
                .putBoolean("PersonaSkin", jengaBlockSkin.isPersona())
                .putBoolean("CapeOnClassicSkin", jengaBlockSkin.isCapeOnClassic());
        nbt.putCompound("Skin", skinDataTag);
        nbt.putBoolean("ishuman", true);

        FullChunk chunk = position.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
        EntityHuman jengaHuman = new EntityHuman(chunk, nbt);
        jengaHuman.setPositionAndRotation(position, position.getYaw(), position.getPitch());
        jengaHuman.setImmobile(true);
        jengaHuman.setNameTagAlwaysVisible(false);
        jengaHuman.setNameTagVisible(false);
        jengaHuman.setNameTag(uuid);
        jengaHuman.setSkin(jengaBlockSkin);
        jengaHuman.setScale(BLOCK_SCALE);

        persistentBlockIDs.add(uuid);
        return uuid;
    }



}
