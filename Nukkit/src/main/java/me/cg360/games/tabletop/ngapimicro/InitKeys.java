package me.cg360.games.tabletop.ngapimicro;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import net.cg360.nsapi.commons.data.keyvalue.Key;

public class InitKeys {

    public static final Key<Player> HOST_PLAYER = new Key<>("host_player");

    // Where was the game started?
    public static final Key<Location> ORIGIN = new Key<>("origin");

    // Where was the game started?
    public static final Key<Double> INVITE_RADIUS = new Key<>("invite_radius");

}
