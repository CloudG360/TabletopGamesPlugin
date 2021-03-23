package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import me.cg360.games.tabletop.ngapimicro.MicroGameBehaviour;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import me.cg360.games.tabletop.ngapimicro.rule.RuleAcquirePlayersFromRadius;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnWorldChange;
import net.cg360.nsapi.commons.Check;
import net.cg360.nsapi.commons.data.Settings;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class GBehaveJenga extends MicroGameBehaviour {

    protected Settings initSettings;
    protected ArrayList<Player> players;

    protected Location origin;
    protected String inviteMessage;
    protected int inviteLengthTicks;

    protected RuleAcquirePlayersFromRadius recruitmentRule;

    @Override
    public void init(Settings settings) {
        this.initSettings = settings;
        this.players = new ArrayList<>();

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
    }



    @Override
    public WatchdogRule[] getRules() {
        this.recruitmentRule = new RuleAcquirePlayersFromRadius(getWatchdog(), inviteMessage, origin, initSettings.getOrElse(InitKeys.INVITE_RADIUS, 10d), true);

        TabletopGamesNukkit.getScheduler().scheduleDelayedTask(TabletopGamesNukkit.get(), () -> {
            this.recruitmentRule.setEnabled(false); // Disable invites after invite interval, open for at least 1 second.
        }, inviteLengthTicks);

        return new WatchdogRule[] {
                new RuleReleasePlayerOnWorldChange(getWatchdog()),
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
}
