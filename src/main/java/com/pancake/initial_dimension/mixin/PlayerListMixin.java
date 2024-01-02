package com.pancake.initial_dimension.mixin;

import com.mojang.serialization.Dynamic;
import com.pancake.initial_dimension.InitialDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag modifyLoad(PlayerList instance, ServerPlayer player) {
        System.out.println("modifyLoad Pos" + player.level().dimension());

        ResourceKey<Level> levelResourceKey = InitialDimension.Config.getDimension();
        CompoundTag compoundtag = instance.load(player);
        ResourceKey<Level> resourcekey = compoundtag != null ?
                DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, compoundtag.get("Dimension")))
                        .resultOrPartial(InitialDimension.LOGGER::error)
                        .orElse(Level.OVERWORLD)
                : levelResourceKey;
        if(resourcekey == levelResourceKey) {
            if(compoundtag == null)
                compoundtag = new CompoundTag();
            compoundtag.putString("Dimension", levelResourceKey.location().toString());
            return compoundtag;
        }

        return instance.load(player);
    }

    @Redirect(method = "respawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"), require = 0)
    private ServerLevel changeRespawnOverworld(MinecraftServer minecraftServer) {
        ResourceKey<Level> dimension = InitialDimension.Config.getDimension();
        return minecraftServer.getLevel(dimension);
    }

    @Redirect(method = "getPlayerForLogin", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"), require = 0)
    private ServerLevel changePlayerForLoginOverworld(MinecraftServer minecraftServer) {
        ResourceKey<Level> dimension = InitialDimension.Config.getDimension();
        return minecraftServer.getLevel(dimension);
    }
}