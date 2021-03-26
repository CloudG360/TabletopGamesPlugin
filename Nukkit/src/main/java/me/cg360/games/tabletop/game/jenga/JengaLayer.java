package me.cg360.games.tabletop.game.jenga;

import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import me.cg360.games.tabletop.game.jenga.entity.EntityJengaBlock;
import net.cg360.nsapi.commons.Check;

public class JengaLayer {

    protected Location layerOrigin;

    protected float scale;
    protected boolean isAxisAlternate; // Otherwise facing Z

    protected EntityJengaBlock left; // Lowest on the layer's axis
    protected EntityJengaBlock center;
    protected EntityJengaBlock right; // Highest on the layer's axis.


    /** @param belowInStack the layer in the stack that this stack will be placed on top of. */
    public JengaLayer(JengaLayer belowInStack) {
        this(belowInStack.getLayerOrigin().getLocation(), belowInStack.getScale(), !belowInStack.isAxisAlternate()); // Retain scale, flip alternate axis value.
        this.layerOrigin = layerOrigin.add(0, scale, 0); //Update location, stack y up by 1.
    }

    /**
     * Creates an empty JengaBlock layer.
     * @param layerOrigin the central position of the layer.
     * @param scale the scale of the blocks (and positions) of the layer)
     * @param isAxisAlternate True if the blocks are placed up the X axis. False if they're placed up the Z axis.
     */
    public JengaLayer(Location layerOrigin, float scale, boolean isAxisAlternate) {
        Check.nullParam(layerOrigin, "layerOrigin");
        Check.nullParam(layerOrigin.getLevel(), "layerOrigin.level");

        this.layerOrigin = layerOrigin;

        this.scale = scale;
        this.isAxisAlternate = isAxisAlternate;

        this.left = null;
        this.center = null;
        this.right = null;
    }



    /** Spawns the Jenga block of the lower position of the layer's set axis */
    public boolean spawnLeft() {

        if((left == null) || left.isClosed()) {
            Location loc;

            if (isAxisAlternate) {
                loc = new Location(
                        layerOrigin.getX() - scale,
                        layerOrigin.getY(),
                        layerOrigin.getZ(),
                        layerOrigin.getLevel()
                );

            } else {
                loc = new Location(
                        layerOrigin.getX(),
                        layerOrigin.getY(),
                        layerOrigin.getZ() - scale,
                        layerOrigin.getLevel()
                );
            }

            this.left = spawnBlock(loc);
            return true;
        }
        return false;
    }

    /** Spawns the Jenga block in the center of the layer. */
    public boolean spawnCenter() {

        if((center == null) || center.isClosed()) {
            // Spawns in the center anyway, no need for an if statement.
            Location loc = new Location(layerOrigin.getX(), layerOrigin.getY(), layerOrigin.getZ(), layerOrigin.getLevel());

            this.center = spawnBlock(loc);
            return true;
        }
        return false;
    }

    /** Spawns the Jenga block of the higher position of the layer's set axis */
    public boolean spawnRight() {

        if((right == null) || right.isClosed()) {
            Location loc;

            if (isAxisAlternate) {
                loc = new Location(
                        layerOrigin.getX() + scale,
                        layerOrigin.getY(),
                        layerOrigin.getZ(),
                        layerOrigin.getLevel()
                );

            } else {
                loc = new Location(
                        layerOrigin.getX(),
                        layerOrigin.getY(),
                        layerOrigin.getZ() + scale,
                        layerOrigin.getLevel()
                );
            }

            this.right = spawnBlock(loc);
            return true;
        }
        return false;
    }

    /** Spawns all the blocks of the layer if not already spawned. */
    public void fillLayer() {
        spawnLeft();
        spawnCenter();
        spawnRight();
    }

    /** @return the id of the spawned block.*/
    protected EntityJengaBlock spawnBlock(Location position) {

        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", position.getX()))
                        .add(new DoubleTag("", position.getY()))
                        .add(new DoubleTag("", position.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", isAxisAlternate ? 90f : 0f))
                        .add(new FloatTag("", 0f)))
                .putBoolean("npc", true)
                .putFloat("scale", scale);
        nbt.putBoolean("ishuman", true);

        FullChunk chunk = position.getLevel().getChunk((int) Math.floor(position.getX() / 16), (int) Math.floor(position.getZ() / 16), true);
        EntityJengaBlock jengaHuman = new EntityJengaBlock(chunk, nbt);

        jengaHuman.setPositionAndRotation(position, isAxisAlternate ? 90f : 0f, 0);
        jengaHuman.setImmobile(true);
        jengaHuman.setNameTagAlwaysVisible(false);
        jengaHuman.setNameTagVisible(false);
        jengaHuman.setScale(scale);

        jengaHuman.spawnToAll();

        return jengaHuman;
    }



    public Location getLayerOrigin() { return layerOrigin; }
    public float getScale() { return scale; }
    public boolean isAxisAlternate() { return isAxisAlternate; }

    public EntityJengaBlock getLeft() { return left; }
    public EntityJengaBlock getCenter() { return center; }
    public EntityJengaBlock getRight() { return right; }

}
