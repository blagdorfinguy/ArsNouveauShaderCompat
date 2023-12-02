package com.hollingsworth.arsnouveau.common.block.tile;

import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.api.item.IWandable;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.ITickable;
import com.hollingsworth.arsnouveau.common.block.RedstoneRelay;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RedstoneRelayTile extends ModdedTile implements IWandable, ITooltipProvider, ITickable {
    public List<BlockPos> poweredFrom = new ArrayList<>();
    public List<BlockPos> powering = new ArrayList<>();

    private int localPower;
    private int powerFromParentRelays;
    private int currentPower;

    private @Nullable BlockPos currentParent;
    boolean updateListeners;

    public RedstoneRelayTile(BlockPos pos, BlockState state) {
        super(BlockRegistry.REDSTONE_RELAY_TILE.get(), pos, state);
    }

    public int getOutputPower(){
        return currentPower;
    }

    @Override
    public void tick() {
        if(updateListeners){
            // force update a tick later to account for connection checks
            this.setNewPower(this.currentPower);
            updateListeners = false;
        }
    }

    public void onParentPowerChange(BlockPos pos, int newParentPower){
        if(!this.poweredFrom.contains(pos)){
            return;
        }
        if(pos.equals(currentParent)){
            powerFromParentRelays = newParentPower;
            calculateNewPower();
        }else if (newParentPower > powerFromParentRelays){
            currentParent = pos.immutable();
            powerFromParentRelays = newParentPower;
            calculateNewPower();
        }
    }

    public void calculateNewPower(){
        int oldPower = currentPower;
        int newPower = localPower;

        for(BlockPos pos : poweredFrom){
            if(!level.isLoaded(pos)){
                continue;
            }
            if(level.getBlockEntity(pos) instanceof RedstoneRelayTile redstoneRelayTile){
                if(redstoneRelayTile.getOutputPower() > localPower){
                    newPower = redstoneRelayTile.getOutputPower();
                    currentParent = pos.immutable();
                    powerFromParentRelays = redstoneRelayTile.getOutputPower();
                }
            }
        }

        if(newPower != oldPower){
            setNewPower(newPower);
        }
    }

    protected void setNewPower(int power){
        this.currentPower = power;
        level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(RedstoneRelay.POWER, power), 3);
        updateBlock();
        level.updateNeighborsAt(worldPosition, BlockRegistry.REDSTONE_RELAY.get());
        updateListeners();
    }

    public void updateListeners(){
        for(BlockPos pos : powering){
            if(!level.isLoaded(pos)){
                continue;
            }
            if(level.getBlockEntity(pos) instanceof RedstoneRelayTile redstoneRelayTile){
                redstoneRelayTile.onParentPowerChange(worldPosition, currentPower);
            }
        }
    }

    public void onParentRemoved(BlockPos pos){
        poweredFrom.remove(pos);
        updateBlock();
        if(currentParent != null && currentParent.equals(pos)){
            calculateNewPower();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    public void onFinishedConnectionLast(@Nullable BlockPos storedPos, @Nullable LivingEntity storedEntity, Player playerEntity) {
        if (storedPos == null || level.isClientSide || storedPos.equals(getBlockPos()) || !(level.getBlockEntity(storedPos) instanceof RedstoneRelayTile))
            return;

        if (BlockUtil.distanceFrom(storedPos, this.worldPosition) <= getMaxDistance()) {
            storedPos = storedPos.immutable();
            if(this.poweredFrom.contains(storedPos)){
                this.poweredFrom.remove(storedPos);
            } else {
                this.poweredFrom.add(storedPos);
            }
            calculateNewPower();
            updateListeners = true;
            updateBlock();
        }
    }

    public int getMaxDistance(){
        return 30;
    }

    @Override
    public void onFinishedConnectionFirst(@javax.annotation.Nullable BlockPos storedPos, @javax.annotation.Nullable LivingEntity storedEntity, Player playerEntity) {
        if (storedPos == null || level.isClientSide || storedPos.equals(getBlockPos()) || !(level.getBlockEntity(storedPos) instanceof RedstoneRelayTile))
            return;

        if (BlockUtil.distanceFrom(storedPos, this.worldPosition) <= getMaxDistance()) {
            storedPos = storedPos.immutable();
            if(this.powering.contains(storedPos)){
                PortUtil.sendMessage(playerEntity, Component.translatable("ars_nouveau.connections.remove", DominionWand.getPosString(storedPos)));
                this.powering.remove(storedPos);
            } else {
                PortUtil.sendMessage(playerEntity, Component.translatable("ars_nouveau.connections.send", DominionWand.getPosString(storedPos)));
                this.powering.add(storedPos);
                ParticleUtil.beam(storedPos, worldPosition, level);
            }
            calculateNewPower();
            updateListeners = true;
            updateBlock();
        } else {
            PortUtil.sendMessage(playerEntity, Component.translatable("ars_nouveau.connections.fail"));
        }
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        poweredFrom = new ArrayList<>();
        powering = new ArrayList<>();
        currentParent = null;
        ListTag listTag = pTag.getList("poweredFrom", 10);
        for(int i = 0; i < listTag.size(); i++){
            CompoundTag tag = listTag.getCompound(i);
            poweredFrom.add(BlockPos.of(tag.getLong("pos")));
        }
        ListTag poweringTag = pTag.getList("powering", 10);
        for(int i = 0; i < poweringTag.size(); i++){
            CompoundTag tag = poweringTag.getCompound(i);
            powering.add(BlockPos.of(tag.getLong("pos")));
        }

        localPower = pTag.getInt("localPower");
        currentPower = pTag.getInt("currentPower");
        powerFromParentRelays = pTag.getInt("powerFromParentRelays");
        if(pTag.contains("currentParent")){
            currentParent = BlockPos.of(pTag.getLong("currentParent"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag listTag = new ListTag();
        for(BlockPos pos : poweredFrom){
            CompoundTag posTag = new CompoundTag();
            posTag.putLong("pos", pos.asLong());
            listTag.add(posTag);
        }
        tag.put("poweredFrom", listTag);

        ListTag poweringTag = new ListTag();
        for(BlockPos pos : powering){
            CompoundTag posTag = new CompoundTag();
            posTag.putLong("pos", pos.asLong());
            poweringTag.add(posTag);
        }
        tag.put("powering", poweringTag);

        tag.putInt("localPower", localPower);
        tag.putInt("currentPower", currentPower);
        tag.putInt("powerFromParentRelays", powerFromParentRelays);
        if(currentParent != null){
            tag.putLong("currentParent", currentParent.asLong());
        }
    }

    public int getLocalPower() {
        return localPower;
    }

    public void setLocalPower(int newLocalPower) {
        if(newLocalPower != localPower) {
            this.localPower = newLocalPower;
            setChanged();
            updateBlock();
            calculateNewPower();
        }
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("current power: " + currentPower));
        if (powering.isEmpty()) {
            tooltip.add(Component.translatable("ars_nouveau.relay.no_to"));
        } else {
            tooltip.add(Component.translatable("ars_nouveau.relay.one_to", powering.size()));
        }
        if (poweredFrom.isEmpty()) {
            tooltip.add(Component.translatable("ars_nouveau.relay.no_from"));
        } else {
            tooltip.add(Component.translatable("ars_nouveau.powered_from", poweredFrom.size()));
        }
    }
}