package me.cg360.games.tabletop.ngapimicro.rule;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import net.cg360.nsapi.commons.Check;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class RuleAcquirePlayersFromRadius extends WatchdogRule implements Listener {

    protected Location origin;
    protected String inviteMessage;
    protected double captureRadius;

    protected boolean sendInvitesImmediately;
    protected boolean isEnabled;

    protected MicroGameWatchdog<?> watchdog;
    protected HashMap<Integer, Player> inviteMap;


    public RuleAcquirePlayersFromRadius(String inviteMessage, Location origin, double radius, boolean sendInvitesImmediately) {
        Check.nullParam(origin, "origin");

        this.origin = origin;
        this.inviteMessage = inviteMessage == null ? ChatColor.BLUE + "Would you like to join a micro-game?" : inviteMessage;
        this.setCaptureRadius(radius);

        this.sendInvitesImmediately = sendInvitesImmediately;
        this.isEnabled = true;

        this.inviteMap = new HashMap<>();
    }



    @Override
    protected void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesSpigot.get().getServer().getPluginManager().registerEvents(this, TabletopGamesSpigot.get());

        if(sendInvitesImmediately) sendInvites();
    }

    @Override
    protected void onStopWatchdog() {
        HandlerList.unregisterAll(this);
    }


    public void sendInvites() {

        if(isEnabled && (origin.getWorld() != null)) {

            for (Player player : origin.getWorld().getPlayers()) {

                if (player.getLocation().distance(origin) <= captureRadius) { // within radius.
                    sendPlayerInvite(player);
                }
            }
        }
    }


    protected void sendPlayerInvite(Player player) {
        MicroGameWatchdog<?> w = MicroGameWatchdog.getPlayerWatchdogs().get(player);

        //TODO: !! Alternate invite system !!
    }


    // Custom event maybe?
    /*
    @EventHandler
    public void onFormWindow(PlayerFormRespondedEvent event) {
        int formID = event.getFormID();

        if(inviteMap.containsKey(formID)) {
            Player owner = inviteMap.get(formID);
            boolean accepted = false;

            if(!event.wasClosed()) {

                if(event.getResponse() instanceof FormResponseModal) {
                    FormResponseModal modalResponse = (FormResponseModal) event.getResponse();
                    accepted = (modalResponse.getClickedButtonId() == 0);
                }
            }

            if(accepted) {

                if (!isEnabled) {
                    owner.sendMessage(Util.eMessage("Invites for this game have closed. Try again next time! :)"));
                    return;
                }
                MicroGameWatchdog<?> w = MicroGameWatchdog.getPlayerWatchdogs().get(owner);

                if(w != watchdog) { // Ensures the player still isn't this watchdog.
                    MicroGameWatchdog.removePlayerFromGames(owner); // Remove from any existing game.
                    owner.sendMessage(Util.fMessage("Invite", ChatColor.BLUE, "Accepted invite!", ChatColor.GREEN));
                    watchdog.capturePlayer(owner);

                } else {
                    owner.sendMessage(Util.eMessage("You're already in the game you were invited to! Magic!"));
                }

            } else {
                owner.sendMessage(Util.fMessage("Invite", ChatColor.BLUE, "Declined invite.", ChatColor.RED));
            }
        }
    }
     */



    public void setCaptureRadius(double captureRadius) {
        this.captureRadius = captureRadius >= 0 ? captureRadius : 0;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
