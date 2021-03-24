package me.cg360.games.tabletop.ngapimicro.rule;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.level.Location;
import cn.nukkit.utils.TextFormat;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.ngapimicro.MicroGameWatchdog;
import me.cg360.games.tabletop.ngapimicro.WatchdogRule;
import net.cg360.nsapi.commons.Check;

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
        this.inviteMessage = inviteMessage == null ? TextFormat.BLUE + "Would you like to join a micro-game?" : inviteMessage;
        this.setCaptureRadius(radius);

        this.sendInvitesImmediately = sendInvitesImmediately;
        this.isEnabled = true;

        this.inviteMap = new HashMap<>();
    }



    @Override
    protected void onStartWatchdog(MicroGameWatchdog<?> watchdog) {
        this.watchdog = watchdog;
        TabletopGamesNukkit.get().getServer().getPluginManager().registerEvents(this, TabletopGamesNukkit.get());

        if(sendInvitesImmediately) sendInvites();
    }

    @Override
    protected void onStopWatchdog() {
        HandlerList.unregisterAll(this);
    }


    public void sendInvites() {

        if(isEnabled) {

            for (Player player : origin.getLevel().getPlayers().values()) {

                if (player.getPosition().getLocation().distance(origin) <= captureRadius) { // within radius.
                    sendPlayerInvite(player);
                }
            }
        }
    }


    protected void sendPlayerInvite(Player player) {
        MicroGameWatchdog<?> w = MicroGameWatchdog.getPlayerWatchdogs().get(player);

        //TODO: Config option to only send if the player is not in a game.
        if( (w != watchdog)) { // Checks player isn't already in the current game
            FormWindowModal modal = new FormWindowModal("Micro-game Invite", inviteMessage, TextFormat.GREEN + "Yes!", TextFormat.RED + "No.");
            int id = player.showFormWindow(modal);

            inviteMap.put(id, player);
        }
    }



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
                    owner.sendMessage(Util.fMessage("Invite", TextFormat.BLUE, "Accepted invite!", TextFormat.GREEN));
                    watchdog.capturePlayer(owner);

                } else {
                    owner.sendMessage(Util.eMessage("You're already in the game you were invited to! Magic!"));
                }

            } else {
                owner.sendMessage(Util.fMessage("Invite", TextFormat.BLUE, "Declined invite.", TextFormat.RED));
            }
        }
    }



    public void setCaptureRadius(double captureRadius) {
        this.captureRadius = captureRadius >= 0 ? captureRadius : 0;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
