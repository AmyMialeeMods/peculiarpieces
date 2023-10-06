package amymialee.peculiarpieces.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup.Entries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import amymialee.peculiarpieces.CustomCreativeItems;

public class PushPadBlock extends AbstractFlatBlock implements CustomCreativeItems {
    public static final IntProperty POWER = IntProperty.of("power", 0, 3);
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public PushPadBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWER, 0).with(POWERED, false).with(FACING, Direction.NORTH));
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        stack.getOrCreateNbt().putInt("pp:variant", state.get(POWER));
        return stack;
    }

    @Override
    public void appendStacks(Entries entries) {
        for (int i : POWER.getValues()) {
            var stack = new ItemStack(this);
            stack.getOrCreateNbt().putInt("pp:variant", i);
            entries.add(stack);
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {}

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!state.get(POWERED) && entity.isOnGround()) {
            var direction = state.get(FACING);
            if (!(direction == null)) {
                double power = (state.get(POWER) + 1) * 0.65f;
                if (state.get(POWER) == 3) {
                    power += 1;
                }
                var vector = Vec3d.of(direction.getVector());
                var velocity = entity.getVelocity();
                var push = new Vec3d(velocity.getX() + (power * 0.2 * vector.getX()), 0, velocity.getZ() + (power * 0.2 * vector.getZ()));
                switch (direction.getAxis()) {
                    case X -> entity.setVelocity(MathHelper.clamp(push.x, -power, power), velocity.y, velocity.z);
                    case Z -> entity.setVelocity(velocity.x, velocity.y, MathHelper.clamp(push.z, -power, power));
                }
                entity.velocityDirty = true;
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld || !player.isSneaking()) {
            return ActionResult.PASS;
        }
        world.setBlockState(pos, state.cycle(POWER), Block.NOTIFY_ALL);
        world.playSound(player, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.3f, (float) (3 * state.get(POWER)) / 15);
        return ActionResult.success(world.isClient);
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var stack = ctx.getStack();
        var state = this.getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos())).with(FACING, ctx.getHorizontalPlayerFacing());
        if (stack.hasNbt() && stack.getNbt() != null) {
            return state.with(POWER, Math.min(3, stack.getNbt().getInt("pp:variant")));
        }
        return state;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        var bl = world.isReceivingRedstonePower(pos);
        if (bl != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, bl), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER, POWERED, FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}