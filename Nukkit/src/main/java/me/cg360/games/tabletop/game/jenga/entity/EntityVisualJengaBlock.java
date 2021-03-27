package me.cg360.games.tabletop.game.jenga.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.event.level.LevelUnloadEvent;
import cn.nukkit.event.server.ServerStopEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddPlayerPacket;
import me.cg360.games.tabletop.TabletopGamesNukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class EntityVisualJengaBlock extends EntityHuman implements Listener {

    protected static final String GEOMETRY;
    protected static final BufferedImage DATA;

    static {

        try {
            InputStream skinGeoIn = TabletopGamesNukkit.get().getResource("jenga/jenga_block.json");
            InputStream skinDataIn = TabletopGamesNukkit.get().getResource("jenga/jenga_block.png");

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

    protected ArrayList<EntityJengaBlockCollider> colliders = new ArrayList<>();

    public EntityVisualJengaBlock(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }


    @Override
    protected void initEntity() {
        this.skin = new Skin(); // Update skin
        this.skin.setGeometryData(GEOMETRY);
        this.skin.setGeometryName("geometry.game.jenga_block");
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

        TabletopGamesNukkit.get().getServer().getPluginManager().registerEvents(this, TabletopGamesNukkit.get());
    }

    @Override
    public void close() {
        if(!closed) {
            HandlerList.unregisterAll(this);

            if(colliders != null) {
                for (EntityJengaBlockCollider collider : colliders) collider.close();
            }
        }
        super.close();
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

    @Override // Calculate offsets.
    public boolean setPosition(Vector3 pos) {
        boolean result = super.setPosition(pos);
        if(colliders != null) {
            for (EntityJengaBlockCollider collider : colliders) {
                collider.updateAngleToParent();
            }
        }
        return result;
    }

    @Override
    public void setRotation(double yaw, double pitch) {
        super.setRotation(yaw, pitch);
        if(colliders != null) {
            for (EntityJengaBlockCollider collider : colliders) {
                collider.updateAngleToParent();
            }
        }
    }

    @Override
    public boolean setMotion(Vector3 motion) {
        if(colliders != null) {
            for (EntityJengaBlockCollider collider : colliders) {
                collider.setMotion(motion);
            }
        }
        return super.setMotion(motion);
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        if(colliders != null) {
            for (EntityJengaBlockCollider collider : colliders) {
                collider.updateAngleToParent();
            }
        }
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        source.setCancelled(true);
        getServer().getPluginManager().callEvent(source);
        return false;
    }

    @Override
    public boolean attack(float damage) {
        this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.CUSTOM, 0));
        return false;
    }

    @Override public float getHeight() { return 0f; }
    @Override public float getWidth() { return 0f; }
    @Override public float getLength() { return 0f; }



    public ArrayList<EntityJengaBlockCollider> getColliders() {
        return colliders;
    }



    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity: new ArrayList<>(event.getChunk().getEntities().values())) {
            if(entity == this) {
                this.close();

                if(colliders != null) {
                    for (EntityJengaBlockCollider c: colliders) c.close();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLevelUnload(LevelUnloadEvent event) {
        for (Entity entity : event.getLevel().getEntities()) {
            if(entity == this) {
                this.close();

                if(colliders != null) {
                    for (EntityJengaBlockCollider c: colliders) c.close();
                }
            }
        }
    }

    @EventHandler
    public void onServerStop(ServerStopEvent event) {
        this.close();

        if(colliders != null) {
            for (EntityJengaBlockCollider c: colliders) c.close();
        }
    }
}
