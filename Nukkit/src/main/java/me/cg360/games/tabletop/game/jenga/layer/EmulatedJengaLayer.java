package me.cg360.games.tabletop.game.jenga.layer;

import cn.nukkit.level.Location;
import me.cg360.games.tabletop.TabletopGamesNukkit;
import me.cg360.games.tabletop.game.jenga.entity.EntityVisualJengaBlock;

public class EmulatedJengaLayer extends JengaLayer {

    protected boolean hasBlockLeft;
    protected boolean hasBlockCenter;
    protected boolean hasBlockRight;

    public EmulatedJengaLayer(JengaLayer clone, boolean hasBlockLeft, boolean hasBlockCenter, boolean hasBlockRight) {
        this.left = null;
        this.center = null;
        this.right = null;
        this.hasBlockLeft = hasBlockLeft;
        this.hasBlockCenter = hasBlockCenter;
        this.hasBlockRight = hasBlockRight;

        this.layerBelow = clone.layerBelow;
        this.towerUUID = clone.towerUUID;
        this.layersBelowCount = clone.layersBelowCount;
        this.layerOrigin = clone.layerOrigin;
        this.scale = clone.scale;
        this.isAxisAlternate = clone.isAxisAlternate;
    }

    @Override
    protected EntityVisualJengaBlock spawnBlock(Location position, int positionWithinLayer) {
        TabletopGamesNukkit.getLog().warning("Attempted spawn of a block on an EmulatedJengaLayer");
        return null;
    }

    @Override public boolean hasLeft() { return this.hasBlockLeft; }
    @Override public boolean hasCenter() { return this.hasBlockCenter; }
    @Override public boolean hasRight() { return this.hasBlockRight; }
}
