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
import net.cg360.nsapi.commons.data.Settings;

import java.text.DecimalFormat;
import java.util.*;

public class GBehaveJenga extends MicroGameBehaviour implements Listener {


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
        spawnBlock(origin.getLocation().add(0, 0, -BLOCK_SCALE), false);
        spawnBlock(origin.getLocation().add(0, 0, 0), false);
        spawnBlock(origin.getLocation().add(0, 0, BLOCK_SCALE), false);

        spawnBlock(origin.getLocation().add(-BLOCK_SCALE, 1, 0), true);
        spawnBlock(origin.getLocation().add(0, 1, 0), true);
        spawnBlock(origin.getLocation().add(BLOCK_SCALE, 1, 0), true);

        spawnBlock(origin.getLocation().add(0, 2, -BLOCK_SCALE), false);
        spawnBlock(origin.getLocation().add(0, 2, 0), false);
        spawnBlock(origin.getLocation().add(0, 2, BLOCK_SCALE), false);

        spawnBlock(origin.getLocation().add(-BLOCK_SCALE, 3, 0), true);
        spawnBlock(origin.getLocation().add(0, 3, 0), true);
        spawnBlock(origin.getLocation().add(BLOCK_SCALE, 3, 0), true);

        spawnBlock(origin.getLocation().add(0, 4, -BLOCK_SCALE), false);
        spawnBlock(origin.getLocation().add(0, 4, 0), false);
        spawnBlock(origin.getLocation().add(0, 4, BLOCK_SCALE), false);

        spawnBlock(origin.getLocation().add(-BLOCK_SCALE, 5, 0), true);
        spawnBlock(origin.getLocation().add(0, 5, 0), true);
        spawnBlock(origin.getLocation().add(BLOCK_SCALE, 5, 0), true);

        spawnBlock(origin.getLocation().add(0, 6, -BLOCK_SCALE), false);
        spawnBlock(origin.getLocation().add(0, 6, 0), false);
        spawnBlock(origin.getLocation().add(0, 6, BLOCK_SCALE), false);

        spawnBlock(origin.getLocation().add(-BLOCK_SCALE, 7, 0), true);
        spawnBlock(origin.getLocation().add(0, 7, 0), true);
        spawnBlock(origin.getLocation().add(BLOCK_SCALE, 7, 0), true);
    }

    /** @return the id of the spawned block.*/
    protected EntityJengaBlock spawnBlock(Location position, boolean alternate) {

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
                        .add(new FloatTag("", alternate ? 90f : 0f))
                        .add(new FloatTag("", 0f)))
                .putBoolean("npc", true)
                .putFloat("scale", BLOCK_SCALE);
        nbt.putBoolean("ishuman", true);

        FullChunk chunk = position.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
        EntityJengaBlock jengaHuman = new EntityJengaBlock(chunk, nbt);

        jengaHuman.setPositionAndRotation(position, alternate ? 90f : 0f, 0);
        jengaHuman.setImmobile(true);
        jengaHuman.setNameTagAlwaysVisible(false);
        jengaHuman.setNameTagVisible(false);
        jengaHuman.setScale(BLOCK_SCALE);

        jengaHuman.spawnToAll();

        return jengaHuman;
    }
}
