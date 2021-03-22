package me.cg360.games.tabletop.ngapimicro;

import cn.nukkit.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    public void initRules() {
        for(WatchdogRule rule: rules) {
            rule.onStartWatchdog();
        }
    }


    /**
     * Stops the game that the watchdog is overlooking.
     */
    public void stopGame() {
        if(isRunning()) {
            this.isRunning = false;
            getBehaviour().onFinish(); // Do any cleanup.

            for(Map.Entry<Player, MicroGameWatchdog<?>> entry: new ArrayList<>(playerWatchdogs.entrySet())) {
                entry.getValue().releasePlayer(entry.getKey());
            }

            for(WatchdogRule rule: rules) {
                rule.onStopWatchdog();
            }
        }
    }

    /**
     * Internally called in order to track which players are in what games.
     * @param player the player being captured.
     * @return true if the player was captured successfully.
     */
    public boolean capturePlayer(Player player) {

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
    public void releasePlayer(Player player) {
        if(playerWatchdogs.containsKey(player) && (playerWatchdogs.get(player) == this)) { // Check player is actually in the lookup
            playerWatchdogs.remove(player);
            getBehaviour().onPlayerRelease(player);
        }
    }




    public T getBehaviour() { return behaviour; }
    public ArrayList<WatchdogRule> getRules() { return new ArrayList<>(rules); }
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

    /**
     * @return a duplicate map of the player watchdogs.
     */
    public static HashMap<Player, MicroGameWatchdog<?>> getPlayerWatchdogs() {
        return new HashMap<>(playerWatchdogs);
    }
}
