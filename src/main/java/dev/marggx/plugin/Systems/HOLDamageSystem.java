package dev.marggx.plugin.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

public class HOLDamageSystem extends DamageSystems.ApplyDamage {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        EntityStatMap entityStatMapComponent = (EntityStatMap)archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        Universe.get().sendMessage(Message.raw("Damage amount: " + damage.getAmount()));
        assert entityStatMapComponent != null;


        int healthStat = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = entityStatMapComponent.get(healthStat);
        Objects.requireNonNull(healthValue);
        boolean isDead = archetypeChunk.getArchetype().contains(DeathComponent.getComponentType());
        if (isDead) {
            damage.setCancelled(true);
        } else {
            damage.setAmount((float)Math.round(damage.getAmount()));
            float newValue = entityStatMapComponent.subtractStatValue(healthStat, damage.getAmount());
            if (newValue <= healthValue.getMin()) {
                damage.setCancelled(true);
            }
        }
    }

}
