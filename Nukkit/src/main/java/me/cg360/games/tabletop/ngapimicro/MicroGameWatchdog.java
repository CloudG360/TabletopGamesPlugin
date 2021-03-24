package me.cg360.games.tabletop.ngapimicro;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import net.cg360.nsapi.commons.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class MicroGameWatchdog<T extends MicroGameBehaviour> {

    private static HashMap<Player, MicroGameWatchdog<?>> playerWatchdogs = new HashMap<>();

    private MicroGameProfile<T> gameProfile;
    private T behaviour;

    private ArrayList<WatchdogRule> rules;
    private boolean isRunning;

    protected MicroGameWatchdog(MicroGameProfile<T> gameProfile, T behaviour) {
        Check.nullParam(gameProfile, "gameProfile");
        Check.nullParam(behaviour, "behaviour");

        this.gameProfile = gameProfile;
        this.behaviour = behaviour;
        this.isRunning = true;
    }

    public void initRules() {
        if(rules == null) {
            WatchdogRule[] rules = behaviour.getRules();
            this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(rules));

            for (WatchdogRule rule : this.rules) {
                rule.onStartWatchdog(this);
            }
        }
    }


    /**
     * Stops the game that the watchdog is overlooking.
     */
    public void stopGame() {

        if(isRunning()) {
            this.isRunning = false;

            try {
                getBehaviour().onFinish(); // Do any cleanup.

            } catch (Exception err) {
                err.printStackTrace();
                TabletopGamesNukkit.getLog().warning("Error occured whilst stopping game. Yikes.");
            }

            for(Map.Entry<Player, MicroGameWatchdog<?>> entry: new ArrayList<>(playerWatchdogs.entrySet())) {
                entry.getValue().releasePlayer(entry.getKey());
            }

            for(WatchdogRule rule: rules) {

                try { rule.onStopWatchdog();
                } catch (Exception err) { err.printStackTrace(); }

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
            player.sendMessage(Util.fMessage(
                    "JOIN",
                    TextFormat.GREEN,
                    String.format("You joined a game of %s'%s'.", TextFormat.AQUA, gameProfile.getProperties().getOrElse(GamePropertyKeys.DISPLAY_NAME, "???"))));

            try { getBehaviour().onPlayerCapture(player); }
            catch (Exception err) { err.printStackTrace(); }

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

            try { getBehaviour().onPlayerRelease(player); }
            catch (Exception err) { err.printStackTrace(); }

            player.sendMessage(Util.fMessage(
                    "LEAVE",
                    TextFormat.RED,
                    String.format("You left a game of %s'%s'.", TextFormat.AQUA, gameProfile.getProperties().getOrElse(GamePropertyKeys.DISPLAY_NAME, "???"))));
        }
    }



    public MicroGameProfile<T> getGameProfile() { return gameProfile; }
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
