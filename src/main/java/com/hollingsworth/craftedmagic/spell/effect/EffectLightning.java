package com.hollingsworth.craftedmagic.spell.effect;

import com.hollingsworth.craftedmagic.ModConfig;
import com.hollingsworth.craftedmagic.spell.augment.AugmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;

public class EffectLightning extends EffectType{
    public EffectLightning() {
        super(ModConfig.EffectLightningID, "Lightning");
    }

    @Override
    public void onResolve(RayTraceResult rayTraceResult, World world, LivingEntity shooter, ArrayList<AugmentType> augments) {
        Vec3d pos = rayTraceResult.getHitVec();
        LightningBoltEntity lightningBoltEntity =new LightningBoltEntity(world, pos.getX(), pos.getY(), pos.getZ(), false);
        lightningBoltEntity.setCaster((ServerPlayerEntity) shooter);
        ((ServerWorld) world).addLightningBolt(lightningBoltEntity);
    }

    @Override
    public int getManaCost() {
        return 50;
    }
}
