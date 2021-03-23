package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import me.cg360.games.tabletop.ngapimicro.MicroGameBehaviour;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import me.cg360.games.tabletop.ngapimicro.rule.RuleReleasePlayerOnWorldChange;
import net.cg360.nsapi.commons.data.Settings;

import java.util.ArrayList;

public class GBehaveJenga extends MicroGameBehaviour {

    private ArrayList<Player> players;
    private String inviteMessage;

    @Override
    public void init(Settings settings) {
        this.players = new ArrayList<>();

        Player host = settings.get(InitKeys.HOST_PLAYER);
        this.inviteMessage = host == null ?
                TextFormat.BLUE + "You have been invited to a game of ":
                "";
    }



    @Override
    public WatchdogRule[] getRules() {
        return new WatchdogRule[] {
                new RuleReleasePlayerOnWorldChange(getWatchdog())
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
