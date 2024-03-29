package amymialee.peculiarpieces.blockentities;

import amymialee.peculiarpieces.blocks.FishTankBlock;
import amymialee.peculiarpieces.mixin.EntityAccessor;
import amymialee.peculiarpieces.mixin.EntityBucketItemAccessor;
import amymialee.peculiarpieces.registry.PeculiarBlocks;
import amymialee.peculiarpieces.screens.FishTankScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import static net.minecraft.entity.passive.TropicalFishEntity.BUCKET_VARIANT_TAG_KEY;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class FishTankBlockEntity extends LockableContainerBlockEntity {
    private DefaultedList<ItemStack> inventory;
    private FishEntity cachedEntity;
    private ItemStack cachedStack;
    private float yaw;

    public FishTankBlockEntity(BlockPos pos, BlockState state) {
        super(PeculiarBlocks.FISH_TANK_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    }

    public FishEntity getCachedEntity() {
        if (this.getStack(0) != this.cachedStack) {
            this.cachedEntity = null;
            this.cachedStack = this.getStack(0);
        }
        if (this.cachedEntity == null) {
            if (this.cachedStack.getItem() instanceof EntityBucketItem bucket) {
                var entity = ((EntityBucketItemAccessor) bucket).getEntityType().create(this.getWorld());
                if (entity instanceof FishEntity fish) {
                    this.cachedEntity = fish;
                }
            } else {
                return null;
            }
            this.cachedEntity.setPosition(Vec3d.of(this.getPos()));
            ((EntityAccessor) this.cachedEntity).setTouchingWater(true);
            this.cachedEntity.setFromBucket(true);
            if (this.cachedEntity instanceof TropicalFishEntity tropicalFish) {
                tropicalFish.setVariant(TropicalFishEntity.Variety.fromId(this.cachedStack.getOrCreateNbt().getInt(BUCKET_VARIANT_TAG_KEY)));
            }
        }
        return this.cachedEntity;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        this.yaw = nbt.getFloat("pp:yaw");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
        nbt.putFloat("pp:yaw", this.yaw);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        var nbtCompound = super.toInitialChunkDataNbt();
        this.writeNbt(nbtCompound);
        return nbtCompound;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("peculiarpieces.container.fish_tank");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FishTankScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (var itemStack : this.inventory) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < this.inventory.size()) {
            return this.inventory.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        var stack = Inventories.splitStack(this.inventory, slot, amount);
        this.updateState();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        var stack = Inventories.removeStack(this.inventory, slot);
        this.updateState();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.inventory.size()) {
            this.inventory.set(slot, stack);
        }
        this.updateState();
    }

    @Override
    public void clear() {
        this.inventory.clear();
        this.updateState();
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void updateState() {
        if (this.world != null && !this.world.isClient()) {
            var present = !this.getStack(0).isEmpty();
            var oldState = this.world.getBlockState(this.pos);
            if (oldState.get(FishTankBlock.FILLED) != present) {
                this.world.setBlockState(this.pos, this.world.getBlockState(this.pos).with(FishTankBlock.FILLED, present));
            }
            this.world.updateListeners(this.pos, oldState, this.world.getBlockState(this.pos), Block.NOTIFY_LISTENERS);
        }
    }
}