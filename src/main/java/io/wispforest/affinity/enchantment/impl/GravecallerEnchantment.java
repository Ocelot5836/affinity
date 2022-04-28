package io.wispforest.affinity.enchantment.impl;

import io.wispforest.affinity.enchantment.template.AbsoluteEnchantment;
import io.wispforest.affinity.enchantment.template.EnchantmentEquipEventReceiver;
import io.wispforest.affinity.misc.AffinityEntityAddon;
import io.wispforest.affinity.misc.EntityReferenceTracker;
import io.wispforest.affinity.misc.LivingEntityTickEvent;
import io.wispforest.affinity.object.AffinityEnchantments;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GravecallerEnchantment extends AbsoluteEnchantment implements EnchantmentEquipEventReceiver {

    public static final AffinityEntityAddon.DataKey<SpawnerLogic> SPAWNER_KEY = AffinityEntityAddon.DataKey.withDefaultFactory(SpawnerLogic::new);
    public static final AffinityEntityAddon.DataKey<Set<EntityReferenceTracker.Reference<Entity>>> MINIONS_KEY = AffinityEntityAddon.DataKey.withDefaultFactory(HashSet::new);
    public static final AffinityEntityAddon.DataKey<LivingEntity> MASTER_KEY = AffinityEntityAddon.DataKey.withNullDefault();

    public GravecallerEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.ARMOR, Type.ARMOR, 205);
    }

    public void serverTick(LivingEntity bearer) {
        final var undeadEntities = getUndeadEntities(bearer);
        final var minions = AffinityEntityAddon.getDataOrSetDefault(bearer, MINIONS_KEY);

        for (var undead : undeadEntities) {
            if (AffinityEntityAddon.hasData(undead, MASTER_KEY)) continue;

            AffinityEntityAddon.setData(undead, MASTER_KEY, bearer);
            minions.add(EntityReferenceTracker.tracked(undead));
        }

        var spawner = AffinityEntityAddon.getDataOrSetDefault(bearer, SPAWNER_KEY);
        spawner.serverTick((ServerWorld) bearer.world, bearer.getBlockPos());
    }

    public void clientTick(LivingEntity bearer) {

    }

    private List<Entity> getUndeadEntities(LivingEntity master) {
        return master.world.getOtherEntities(
                null,
                master.getBoundingBox().expand(20),
                entity -> entity instanceof LivingEntity living && living.getGroup() == EntityGroup.UNDEAD);
    }

    @Override
    public void onUnequip(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (!this.slotTypes.contains(slot)) return;

        if (!hasCompleteArmor(entity) && AffinityEntityAddon.hasData(entity, MINIONS_KEY)) {
            final var minions = AffinityEntityAddon.removeData(entity, MINIONS_KEY);
            for (var minion : minions) {
                if (!minion.present()) continue;
                AffinityEntityAddon.removeData(minion.get(), MASTER_KEY);
            }
        }
    }

    @Override
    public void onEquip(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {}

    public static boolean isMaster(Entity undead, Entity potentialMaster) {
        return AffinityEntityAddon.hasData(undead, MASTER_KEY) && AffinityEntityAddon.getData(undead, MASTER_KEY) == potentialMaster;
    }

    static {
        LivingEntityTickEvent.EVENT.register(entity -> {
            if (!AffinityEnchantments.GRAVECALLER.hasCompleteArmor(entity)) return;

            if (!entity.world.isClient) {
                AffinityEnchantments.GRAVECALLER.serverTick(entity);
            } else {
                AffinityEnchantments.GRAVECALLER.clientTick(entity);
            }
        });
    }

    public static class SpawnerLogic extends MobSpawnerLogic {

        private SpawnerLogic() {
            this.setEntityId(EntityType.ZOMBIE);
        }

        @Override
        public void sendStatus(World world, BlockPos pos, int i) {}
    }
}
