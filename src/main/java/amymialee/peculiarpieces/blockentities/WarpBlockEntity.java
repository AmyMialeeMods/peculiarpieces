package amymialee.peculiarpieces.blockentities;

import amymialee.peculiarpieces.PeculiarPieces;
import amymialee.peculiarpieces.items.PositionPearlItem;
import amymialee.peculiarpieces.registry.PeculiarBlocks;
import amymialee.peculiarpieces.registry.PeculiarItems;
import amymialee.peculiarpieces.screens.WarpScreenHandler;
import amymialee.peculiarpieces.util.ExtraPlayerDataWrapper;
import amymialee.peculiarpieces.util.WarpInstance;
import amymialee.peculiarpieces.util.WarpManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.Optional;

public class WarpBlockEntity extends LootableContainerBlockEntity {
    private DefaultedList<ItemStack> inventory;

    public WarpBlockEntity(BlockPos pos, BlockState state) {
        super(PeculiarBlocks.WARP_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    }

    public void onEntityCollided(Entity entity) {
        var stack = this.inventory.get(0);
        if (stack.isOf(PeculiarItems.POS_PEARL) || stack.isOf(PeculiarItems.POS_PAPER)) {
            var compound = stack.getNbt();
            if (compound != null && compound.contains("pp:target")) {
                var pos = PositionPearlItem.readStone(stack);
                WarpManager.queueTeleport(WarpInstance.of(entity).position(pos).particles());
            }
        } else if (stack.isOf(PeculiarItems.CHECKPOINT_PEARL)) {
            if (entity instanceof PlayerEntity player && player instanceof ExtraPlayerDataWrapper checkPlayer) {
                var checkpointPos = checkPlayer.getCheckpointPos();
                if (checkpointPos != null) {
                    var instance = WarpInstance.of(entity).position(checkpointPos).particles();
                    var worldRegistryKey = checkPlayer.getCheckpointWorld();
                    if (worldRegistryKey != null) {
                        instance.world(worldRegistryKey);
                    }
                    WarpManager.queueTeleport(instance);
                    player.sendMessage(Text.translatable("%s.checkpoint_returned".formatted(PeculiarPieces.MOD_ID)).formatted(Formatting.GRAY), true);
                }
            }
        } else if (stack.isOf(PeculiarItems.SKY_PEARL)) {
            if (this.world != null) {
                var vec3d = Vec3d.ofBottomCenter(this.pos);
                WarpManager.queueTeleport(WarpInstance.of(entity).position(new Vec3d(vec3d.getX(), this.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, this.getPos()).getY(), vec3d.getZ())).particles());
            }
        } else if (stack.isOf(PeculiarItems.SPAWNPOINT_PEARL)) {
            if (!entity.getWorld().isClient) {
                if (this.world instanceof ServerWorld serverWorld && entity instanceof ServerPlayerEntity player) {
                    if (player.getSpawnPointPosition() != null) {
                        var spawnpoint = PlayerEntity.findRespawnPosition(serverWorld, player.getSpawnPointPosition(), 0, false, true);
                        if (spawnpoint.isPresent()) {
                            var spawnDim = player.getSpawnPointDimension();
                            if (spawnDim != player.getWorld().getRegistryKey()) {
                                var level = serverWorld.getServer().getWorld(spawnDim);
                                if (!(level == null)) {
                                    player.moveToWorld(level);
                                }
                            }
                            WarpManager.queueTeleport(WarpInstance.of(entity).position(spawnpoint.get()).particles());
                        } else {
                            WarpManager.queueTeleport(WarpInstance.of(entity).position(serverWorld.getSpawnPos()).particles());
                        }
                    } else {
                        WarpManager.queueTeleport(WarpInstance.of(entity).position(serverWorld.getSpawnPos()).particles());
                    }
                    player.sendMessage(Text.translatable("%s.spawnpoint_returned".formatted(PeculiarPieces.MOD_ID)).formatted(Formatting.GRAY), true);
                }
            }
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("peculiarpieces.container.warp_block");
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new WarpScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return this.inventory.size();
    }
}