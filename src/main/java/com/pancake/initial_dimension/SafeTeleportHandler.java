package com.pancake.initial_dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.*;


@Mod.EventBusSubscriber
public class SafeTeleportHandler {

        @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player entity = event.getEntity();
        Level level = entity.level();
        new Thread(() -> {
            teleportToSafeLocation(level, entity, 240);
        }).start();
    }

   @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player entity = event.getEntity();
        Level level = entity.level();
        new Thread(() -> {
            teleportToSafeLocation(level, entity, 240);
        }).start();
    }
    //这段可能和campfire spawn冲突
    //但是经过测试，主要是判定safe location的条件需要兼容campfire，这里可以不改，也许可以考虑做成配置文件来判定

    private static void teleportToSafeLocation(Level level, Player player, int radius) {
        if (!isLocationSafe(player).isEmpty()) {
            BlockPos safePos = findSafeLocation(level, player, radius);
            if (safePos != null) {
                player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            } else {
                randomTeleportTo(level, player);
                safePos = findSafeLocation(level, player, radius);
                if (safePos != null) {
                    player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                } else {
                    createPlatformAtPlayer(level, player);
                }
            }
        }
    }

    private static void createPlatformAtPlayer(Level level, Player player) {
        BlockPos pos = player.blockPosition();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                BlockPos platformPos = pos.offset(x, -1, z);
                for (int i = 0; i < 1; i++) {
                    level.setBlockAndUpdate(platformPos.above(i), Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(platformPos, Blocks.STONE.defaultBlockState());
            }
        }
    }


    private static void randomTeleportTo(Level level, Player entity) {
        Random random = new Random();
        int x = random.nextInt(250);
        int z = random.nextInt(250);
        int y = random.nextInt(level.getHeight());
        entity.teleportTo(x, y, z);
    }


    private static List<Direction> isLocationSafe(Player player) {
        BlockPos pos = player.blockPosition();
        Level level = player.level();

        List<Direction> directions = new ArrayList<>();
        //兼容营火复活模组
        //这里是主要的兼容问题，增加判定条件以后不会出现复活乱传送的情况，可考虑增加更多兼容
        if(level.getBlockState(pos.below()).getBlock() == Blocks.CAMPFIRE || level.getBlockState(pos.below()).getBlock()==Blocks.SOUL_CAMPFIRE||
                level.getBlockState(pos.above(1)).getBlock() == Blocks.CAMPFIRE || level.getBlockState(pos.above(1)).getBlock()==Blocks.SOUL_CAMPFIRE)
        {
            return directions;
        }

        if (!level.noCollision(player)) {
            directions.add(Direction.NORTH);
            directions.add(Direction.SOUTH);
            directions.add(Direction.WEST);
            directions.add(Direction.EAST);
        }

        if (level.getBlockState(pos.below()).getBlock() == Blocks.AIR || level.getBlockState(pos.below()).getBlock() == Blocks.BEDROCK) {
            directions.add(Direction.DOWN);
        }

        if (!level.getFluidState(pos).isEmpty()) {
            directions.add(Direction.UP);
        }

        return directions;
    }

    private static BlockPos findSafeLocation(Level level, Player player, int radius) {
        List<Direction> directions = isLocationSafe(player);

        for (Direction direction : directions) {
            for (int i = 1; i <= 20; ++i) {
                BlockPos pos = player.blockPosition().relative(direction, i);
                if (isAreaSafe(level, pos)) {
                    return pos.relative(Direction.UP);
                }
            }
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.offer(player.blockPosition());

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (isAreaSafe(level, current)) {
                return current.relative(Direction.UP);
            }

            if (player.blockPosition().distManhattan(current) <= radius) {
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }

        return null;
    }


    private static boolean isAreaSafe(Level level, BlockPos pos) {
        // 玩家脚下必须可站立
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
            return false;
        }
        for (int i = 1; i <= 2; ++i) {
            if (!level.getBlockState(pos.above(i)).isAir() ||
                    !level.getFluidState(pos.above(i)).isEmpty() ||
                    level.getBlockState(pos).getBlock()==Blocks.LAVA||
                    level.getBlockState(pos).getBlock()==Blocks.NETHERRACK) {
                return false;
            }
        }

        return true;
    }
}