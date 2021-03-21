package me.cg360.games.tabletop.game;

import cn.nukkit.Player;
import me.cg360.games.tabletop.game.rule.WatchdogRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public final class MicroGameWatchdog<T extends MicroGameBehaviour> {

    private static HashMap<Player, MicroGameWatchdog<?>> playerWatchdogs = new HashMap<>();

    private T behaviour;
    private ArrayList<WatchdogRule> rules;
    private boolean isRunning;

    protected MicroGameWatchdog(T behaviour) {
        this.behaviour = behaviour;
        this.rules = new ArrayList<>(Arrays.asList(behaviour.getRules()));
        this.isRunning = true;
    }

    /**
     * Internally called in order to track which players are in what games.
     * @param player the player being captured.
     * @return true if the player was captured successfully.
     */
    protected boolean capturePlayer(Player player) {

        if(!playerWatchdogs.containsKey(player)) {
            playerWatchdogs.put(player, this);
            getBehaviour().onPlayerCapture(player);
            return true;
        }
        return false;
    }

    /**
     * Internally called in order to track if a player is naturally removed
     * from a game.
     * @param player the player being released
     */
    protected void releasePlayer(Player player) {
        playerWatchdogs.remove(player);
        getBehaviour().onPlayerRelease(player);
    }

    public void stopGame() {
        if(isRunning()) {
            this.isRunning = false;
            // Do any cleanup.
            getBehaviour().finish();
            getBehaviour().onFinish();
        }
    }



    public T getBehaviour() { return behaviour; }
    public boolean isRunning() { return isRunning; }



    /**
     * Forcefully removes a player from any micro-game as long as the micro
     * game reports players correctly.
     * @param player the player to be removed.
     */
    public static boolean removePlayerFromGames(Player player) {
        if(playerWatchdogs.containsKey(player)) {
            MicroGameWatchdog<?> watchdog = playerWatchdogs.remove(player);
            watchdog.releasePlayer(player);
            return true;
        }
        return false;
    }
}
