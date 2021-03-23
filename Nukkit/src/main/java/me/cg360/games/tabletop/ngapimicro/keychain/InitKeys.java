package me.cg360.games.tabletop.ngapimicro.keychain;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import net.cg360.nsapi.commons.data.keyvalue.Key;

public class InitKeys {

    // Which player (if any) started the game.
    public static final Key<Player> HOST_PLAYER = new Key<>("host_player");

    // Where was the game started?
    public static final Key<Location> ORIGIN = new Key<>("origin");

    // How far around the origin should invites be sent to.
    public static final Key<Double> INVITE_RADIUS = new Key<>("invite_radius");

    // How long (in ticks) should "yes" responses be accepted for.
    public static final Key<Integer> INITIAL_INVITE_LENGTH = new Key<>("init_invite_length");

}
