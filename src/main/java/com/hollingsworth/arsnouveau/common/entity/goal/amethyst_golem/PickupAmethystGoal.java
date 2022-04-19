package com.hollingsworth.arsnouveau.common.entity.goal.amethyst_golem;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.entity.AmethystGolem;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.function.Supplier;

public class PickupAmethystGoal extends Goal {

    public AmethystGolem golem;
    public Supplier<Boolean> canUse;
    ItemEntity targetEntity;
    int usingTicks;
    boolean isDone;

    public PickupAmethystGoal(AmethystGolem golem, Supplier<Boolean> canUse){
        this.golem = golem;
        this.canUse = canUse;
    }

    @Override
    public boolean canContinueToUse() {
        return targetEntity != null && !isDone;
    }

    @Override
    public void tick() {
        super.tick();

        usingTicks--;
        if(usingTicks <= 0){
            isDone = true;
            collectStacks();
            return;
        }
        if(targetEntity == null)
            return;
        Path path = golem.getNavigation().createPath(targetEntity, 2);
        if(path != null){
            golem.getNavigation().moveTo(path, 1.3f);
        }
        if(BlockUtil.distanceFrom(golem.blockPosition(), targetEntity.blockPosition()) <= 1.5){
            collectStacks();
            isDone = true;
            golem.pickupCooldown = 60 + golem.getRandom().nextInt(10);
        }
    }

    public void collectStacks(){

        for(ItemEntity i : golem.level.getEntitiesOfClass(ItemEntity.class,new AABB(golem.getHome()).inflate(10))){
            if(i.getItem().getItem() != Items.AMETHYST_SHARD)
                continue;
            int maxTake = golem.getHeldStack().getMaxStackSize() - golem.getHeldStack().getCount();
            if(golem.getHeldStack().isEmpty()){
                golem.setHeldStack(i.getItem().copy());
                i.getItem().setCount(0);
                continue;
            }

            int toTake = Math.min(i.getItem().getCount(), maxTake);
            i.getItem().shrink(toTake);
            golem.getHeldStack().grow(toTake);

        }
    }

    @Override
    public void start() {
        this.isDone = false;
        this.usingTicks = 80;
        for(ItemEntity entity : golem.level.getEntitiesOfClass(ItemEntity.class, new AABB(golem.getHome()).inflate(10))){
            Path path = golem.getNavigation().createPath(entity.blockPosition(), 2);
            if(path != null && path.canReach() && entity.getItem().getItem() == Items.AMETHYST_SHARD){
                targetEntity = entity;
                return;
            }
        }
        if(targetEntity == null)
            isDone = true;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean canUse() {
        if(golem.getHome() == null)
            return false;
        BlockEntity entity = golem.getLevel().getBlockEntity(golem.getHome());
        return canUse.get() && golem.pickupCooldown <= 0 && entity != null && entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent();
    }
}