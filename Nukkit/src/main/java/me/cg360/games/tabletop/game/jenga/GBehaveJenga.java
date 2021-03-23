package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import me.cg360.games.tabletop.ngapimicro.MicroGameBehaviour;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import me.cg360.games.tabletop.ngapimicro.rule.RuleAcquirePlayersFromRadius;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnWorldChange;
import net.cg360.nsapi.commons.Check;
import net.cg360.nsapi.commons.data.Settings;

import java.util.ArrayList;

public class GBehaveJenga extends MicroGameBehaviour {

    protected Settings initSettings;
    protected ArrayList<Player> players;

    protected Location origin;
    protected String inviteMessage;

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

        this.inviteMessage = host == null ?
                // Null player host.
                String.format("%sYou have been invited to a game of %s%s%s! Would you like to join?",
                        TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"), TextFormat.BLUE):
                // Player host, use their unformatted name.
                String.format("%s%s%s has invited you to a game of %s%s! %sWould you like to join?",
                        TextFormat.AQUA, host.getName(), TextFormat.BLUE, TextFormat.AQUA, properties.getOrElse(GamePropertyKeys.DISPLAY_NAME, "Jenga"), TextFormat.BLUE);
    }



    @Override
    public WatchdogRule[] getRules() {
        return new WatchdogRule[] {
                new RuleReleasePlayerOnWorldChange(getWatchdog()),
                new RuleAcquirePlayersFromRadius(getWatchdog(), inviteMessage, origin, initSettings.getOrElse(InitKeys.INVITE_RADIUS, 10d), true)
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
