package dev.marggx.plugin.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import dev.marggx.plugin.HeartOfLife;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class HOLDamageSystem extends DamageSystems.ApplyDamage {

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
        new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
        new SystemDependency<EntityStore, DamageSystems.ApplyDamage>(Order.BEFORE, DamageSystems.ApplyDamage.class)
    );

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EntityStatMap.getComponentType(), Player.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        EntityStatMap entityStatMapComponent = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());

        assert entityStatMapComponent != null;
        int healthStat = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = entityStatMapComponent.get(healthStat);
        Objects.requireNonNull(healthValue);
        float calcHealth = healthValue.get() - damage.getAmount();

        if (calcHealth > healthValue.getMin()) {
            return;
        }

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        assert player != null;

        ItemContainer utilities = player.getInventory().getUtility();
        AtomicBoolean hasFoundHOL = new AtomicBoolean(false);

        utilities.forEach((i, itemStack) -> {
            if (hasFoundHOL.get()) {
                return;
            }

            if (!itemStack.getItemId().equals(HeartOfLife.HOLItemId)) {
                return;
            }

            hasFoundHOL.set(true);
            utilities.removeItemStackFromSlot(i, itemStack, 1);
        });

        if (!hasFoundHOL.get()) {
            return;
        }

        float newDamage = healthValue.get() - (healthValue.getMin() + 1);
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        assert playerRef != null;

        String entityEffectId = "H_O_L_Health_Regen";
        EntityEffect entityEffect = EntityEffect.getAssetMap().getAsset(entityEffectId);
        assert entityEffect != null;

        EffectControllerComponent effectControllerComponent = archetypeChunk.getComponent(index, EffectControllerComponent.getComponentType());
        assert effectControllerComponent != null;

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        assert playerEntityRef != null;

        effectControllerComponent.addEffect(playerEntityRef, entityEffect, store);

        NotificationUtil.sendNotification(
            playerRef.getPacketHandler(),
            Message.translation("items.Heart_Of_Live.used").color("#00FF00"),
            NotificationStyle.Success
        );
        damage.setAmount(newDamage);
    }
}
