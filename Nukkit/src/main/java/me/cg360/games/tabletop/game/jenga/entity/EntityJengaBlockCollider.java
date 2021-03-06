package me.cg360.games.tabletop.game.jenga.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddPlayerPacket;
import me.cg360.games.tabletop.TabletopGamesNukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * A dummy entity that passes damage to itself onto it's parent EntityVisualJengaBlock
 */
public class EntityJengaBlockCollider extends EntityHuman implements Listener {

    protected static final String GEOMETRY;
    protected static final BufferedImage DATA;

    static {

        try {
            InputStream skinGeoIn = TabletopGamesNukkit.get().getResource("jenga/collider.json");
            InputStream skinDataIn = TabletopGamesNukkit.get().getResource("jenga/collider.png"); // it has no cubes so this doesn't matter.

            BufferedReader read = new BufferedReader(new InputStreamReader(skinGeoIn));
            Iterator<String> i = read.lines().iterator();
            String geoStr = "";

            while (i.hasNext()) {
                geoStr = geoStr.concat(i.next());
                if(i.hasNext()) geoStr = geoStr.concat("\n"); // Add a newline unless the end has been reached.
            }

            GEOMETRY = geoStr;
            DATA = ImageIO.read(skinDataIn);

        } catch (IOException err) {
            throw new IllegalStateException("Unable to load Jenga block model from resources");
        }
    }

    protected EntityVisualJengaBlock jengaBlockParent;
    protected double distance;

    public EntityJengaBlockCollider(EntityVisualJengaBlock jengaBlockParent, double distance) {
        super(jengaBlockParent.getChunk(), generateNBTFromParent(jengaBlockParent));
        this.jengaBlockParent = jengaBlockParent;
        this.distance = distance;
        this.setScale(jengaBlockParent.getScale());
        this.setImmobile(true);
        this.updateAngleToParent();
    }

    private static CompoundTag generateNBTFromParent(EntityVisualJengaBlock jengaBlockParent) {
        return new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", jengaBlockParent.getX()))
                        .add(new DoubleTag("", jengaBlockParent.getY()))
                        .add(new DoubleTag("", jengaBlockParent.getZ())))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", jengaBlockParent.getMotion().getX()))
                        .add(new DoubleTag("", jengaBlockParent.getMotion().getY()))
                        .add(new DoubleTag("", jengaBlockParent.getMotion().getZ())))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float) jengaBlockParent.getYaw()))
                        .add(new FloatTag("", (float) jengaBlockParent.getPitch())))
                .putBoolean("npc", true)
                .putFloat("scale", jengaBlockParent.getScale());
    }


    @Override
    protected void initEntity() {
        this.skin = new Skin(); // Update skin
        this.skin.setGeometryData(GEOMETRY);
        this.skin.setGeometryName("geometry.game.collider");
        this.skin.setSkinData(DATA);
        this.skin.setTrusted(true);

        CompoundTag skinDataTag = new CompoundTag()
                .putByteArray("Data", skin.getSkinData().data)
                .putInt("SkinImageWidth", skin.getSkinData().width)
                .putInt("SkinImageHeight", skin.getSkinData().height)
                .putString("ModelId", skin.getSkinId())
                .putString("CapeId", skin.getCapeId())
                .putByteArray("CapeData", skin.getCapeData().data)
                .putInt("CapeImageWidth", skin.getCapeData().width)
                .putInt("CapeImageHeight", skin.getCapeData().height)
                .putByteArray("SkinResourcePatch", skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", skin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", skin.isPremium())
                .putBoolean("PersonaSkin", skin.isPersona())
                .putBoolean("CapeOnClassicSkin", skin.isCapeOnClassic())
                .putBoolean("IsTrustedSkin", true);
        this.namedTag.putCompound("Skin", skinDataTag);
        super.initEntity();
        this.skin.generateSkinId(this.getUniqueId().toString());
    }

    @Override
    public void spawnTo(Player player) {

        if(this.namedTag == null) this.namedTag = new CompoundTag();

        this.skin.setTrusted(true);

        this.server.updatePlayerListData(this.getUniqueId(), this.getId(), this.getName(), this.getSkin());

        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.uuid = this.getUniqueId();
        addPlayerPacket.username = this.getName();
        addPlayerPacket.entityUniqueId = this.getId();
        addPlayerPacket.entityRuntimeId = this.getId();
        addPlayerPacket.x = (float) this.x;
        addPlayerPacket.y = (float) this.y;
        addPlayerPacket.z = (float) this.z;
        addPlayerPacket.speedX = (float) this.motionX;
        addPlayerPacket.speedY = (float) this.motionY;
        addPlayerPacket.speedZ = (float) this.motionZ;
        addPlayerPacket.yaw = (float) this.yaw;
        addPlayerPacket.pitch = (float) this.pitch;
        addPlayerPacket.item = this.getInventory().getItemInHand();
        addPlayerPacket.metadata = this.dataProperties;
        player.dataPacket(addPlayerPacket);

        this.server.removePlayerListData(this.getUniqueId(), new Player[] { player } );
        super.spawnTo(player);
    }

    public void updateAngleToParent() {
        double angleRadians = Math.toRadians(jengaBlockParent.getYaw() + 90); // It's off in spacing by 90 degrees :D
        Vector3 direction = new Vector3(Math.sin(angleRadians), 0, Math.cos(angleRadians));
        Vector3 parentDelta = direction.multiply(getFullDistance());

        this.setPositionAndRotation(jengaBlockParent.getPosition().add(parentDelta), jengaBlockParent.getYaw(), 0);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {

        if(source instanceof EntityDamageByEntityEvent) { // Implement others when needed
            EntityDamageByEntityEvent old = (EntityDamageByEntityEvent) source;
            EntityDamageByEntityEvent n = new EntityDamageByEntityEvent(old.getDamager(), this.jengaBlockParent, old.getCause(), old.getDamage());
            return this.jengaBlockParent.attack(n);
        }
        return false;
    }

    @Override
    public boolean attack(float damage) {
        return this.jengaBlockParent.attack(0);
    }

    @Override public float getHeight() { return 1f; }
    @Override public float getWidth() { return 1f; }
    @Override public float getLength() { return 1f; }

    public double getFullDistance() {
        return distance*jengaBlockParent.getScale();
    }
}
