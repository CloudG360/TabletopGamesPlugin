package me.cg360.games.tabletop.ngapimicro.keychain;

import net.cg360.nsapi.commons.data.keyvalue.Key;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class InitKeys {

    // Which player (if any) started the game.
    public static final Key<Player> HOST_PLAYER = new Key<>("host_player");

    // Where was the game started?
    public static final Key<Location> ORIGIN = new Key<>("origin");

    // How far should the radius where players are part of the game reach
    public static final Key<Double> PLAY_AREA_RADIUS = new Key<>("play_area_radius");

    // How far around the origin should invites be sent to.
    public static final Key<Double> INVITE_RADIUS = new Key<>("invite_radius");

    // How long (in ticks) should "yes" responses be accepted for.
    public static final Key<Integer> INITIAL_INVITE_LENGTH = new Key<>("init_invite_length");

    // Are debug messages and behaviours enabled in the game?
    public static final Key<Boolean> DEBUG_MODE_ENABLED = new Key<>("is_debug_enabled");

}
