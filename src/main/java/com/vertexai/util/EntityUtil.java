package com.vertexai.util;

import com.vertexai.pathfinder.helper.BlockStateAccessor;
import com.vertexai.pathfinder.movement.MovementHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class EntityUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isNpc(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!(entity instanceof RemotePlayer)) {
            return false;
        }
        return !TablistUtil.getTabListPlayersSkyblock().contains(entity.getName().getString());
    }

    public static BlockPos getBlockStandingOn(Entity entity) {
        if (entity == null) return PlayerUtil.getBlockStandingOn();
        return new BlockPos((int) entity.getX(), (int) Math.ceil(entity.getY() - 0.25) - 1, (int) entity.getZ());
    }

    public static Optional<Entity> getEntityLookingAt() {
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult) {
            return Optional.of(((net.minecraft.world.phys.EntityHitResult) mc.hitResult).getEntity());
        }
        return Optional.empty();
    }

    public static boolean isStandDead(String name) {
        return getHealthFromStandName(name) == 0;
    }

    public static int getHealthFromStandName(String name) {
        int health = 0;
        try {
            String[] arr = name.split(" ");
            health = Integer.parseInt(arr[arr.length - 1].split("/")[0].replace(",", ""));
        } catch (Exception ignored) {
        }
        return health;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, Class<?> entityType) {
        if (e == null || mc.level == null) return null;
        AABB box = e.getBoundingBox().inflate(0.5D, 2.5D, 0.5D);
        List<Entity> possible = mc.level.getEntities(e, box, a -> {
            if (!a.isAlive() || a.equals(mc.player)) return false;

            boolean flag2 = !(a instanceof ArmorStand);
            boolean flag3 = !(a instanceof Fireball);
            boolean flag4 = !(a instanceof FishingHook);
            boolean flag5 = (entityType == null || entityType.isInstance(a));
            return flag2 && flag3 && flag4 && flag5;
        });

        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.distanceTo(e)));
        return null;
    }

    public static List<LivingEntity> getEntities(Set<String> entityNames, Set<LivingEntity> entitiesToIgnore) {
        List<LivingEntity> entities = new ArrayList<>();
        if (mc.level == null || mc.player == null || entityNames == null || entityNames.isEmpty()) {
            return entities;
        }

        net.minecraft.world.phys.AABB searchBox = mc.player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
        for (Entity entity : mc.level.getEntities((Entity) null, searchBox, e -> e != null && e.isAlive())) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive() || living.equals(mc.player) || living.getHealth() <= 0) continue;
            if (entitiesToIgnore != null && entitiesToIgnore.contains(living)) continue;

            // Mode 1: Check direct LivingEntity (Zombies, Spiders, Wolves, Endermen, Ghosts, etc.)
            if (!(living instanceof ArmorStand)) {
                String entityName = living.getName().getString().toLowerCase(Locale.ROOT);
                String customName = living.getCustomName() != null ? living.getCustomName().getString().toLowerCase(Locale.ROOT) : "";
                String typeName = living.getType().getDescription().getString().toLowerCase(Locale.ROOT);

                boolean matches = false;
                for (String targetName : entityNames) {
                    String lowerTarget = targetName.toLowerCase(Locale.ROOT);
                    if (entityName.contains(lowerTarget) || customName.contains(lowerTarget) || typeName.contains(lowerTarget)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    entities.add(living);
                    continue;
                }
            }

            // Mode 2: Check SkyBlock ArmorStand Hologram Nametags
            if (living instanceof ArmorStand armorStand) {
                String customName = armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : "";
                if (customName.isEmpty() || customName.contains(mc.player.getName().getString())) continue;

                boolean nameMatch = false;
                for (String entityName : entityNames) {
                    if (customName.toLowerCase(Locale.ROOT).contains(entityName.toLowerCase(Locale.ROOT))) {
                        nameMatch = true;
                        break;
                    }
                }

                if (nameMatch) {
                    Entity livingBase = getEntityCuttingOtherEntity(armorStand, null);
                    if (livingBase instanceof LivingEntity baseLiving) {
                        if ((entitiesToIgnore == null || !entitiesToIgnore.contains(baseLiving)) && !baseLiving.equals(mc.player) && baseLiving.isAlive()) {
                            if (!entities.contains(baseLiving)) {
                                entities.add(baseLiving);
                            }
                        }
                    }
                }
            }
        }

        return entities;
    }

    public static BlockPos nearbyBlock(LivingEntity entityLivingBase) {
        if (entityLivingBase == null || mc.level == null) return PlayerUtil.getBlockStandingOn();
        BlockPos mobPos = entityLivingBase.blockPosition();

        BlockStateAccessor bsa = new BlockStateAccessor(mc.level);
        if (MovementHelper.INSTANCE.canStandOn(bsa, mobPos.getX(), mobPos.getY(), mobPos.getZ(), bsa.get(mobPos.getX(), mobPos.getY(), mobPos.getZ()))) {
            return mobPos;
        }

        // Search 1-2 blocks below mob for solid standable ground
        for (int y = 0; y >= -3; y--) {
            BlockPos testPos = mobPos.offset(0, y, 0);
            if (MovementHelper.INSTANCE.canStandOn(bsa, testPos.getX(), testPos.getY(), testPos.getZ(), bsa.get(testPos.getX(), testPos.getY(), testPos.getZ()))) {
                return testPos;
            }
        }

        return mobPos;
    }
}
