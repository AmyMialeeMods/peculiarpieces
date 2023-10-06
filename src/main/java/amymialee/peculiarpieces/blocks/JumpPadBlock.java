package amymialee.peculiarpieces.blocks;

import amymialee.peculiarpieces.CustomCreativeItems;
import amymialee.peculiarpieces.callbacks.PlayerJumpConsumingBlock;
import amymialee.peculiarpieces.util.JumpPaddableEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup.Entries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class JumpPadBlock extends AbstractFlatBlock implements PlayerJumpConsumingBlock, CustomCreativeItems {
    public static final IntProperty POWER = IntProperty.of("power", 0, 3);
    public static final BooleanProperty POWERED = Properties.POWERED;

    public JumpPadBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWER, 0).with(POWERED, false));
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

    private void applyJump(BlockState state, LivingEntity entity) {
        if (state.get(POWERED)) {
            return;
        }
        var power = state.get(POWER) + 1;
        double d = (0.42f * power) + entity.getJumpBoostVelocityModifier();
        var vec3d = entity.getVelocity();
        entity.setVelocity(vec3d.x, vec3d.y + d, vec3d.z);
        if (entity.isSprinting()) {
            var f = entity.getYaw() * ((float)Math.PI / 180);
            entity.setVelocity(entity.getVelocity().add(-MathHelper.sin(f) * 0.2f * power, 0.0, MathHelper.cos(f) * 0.2f * power));
        }
        entity.velocityModified = true;
    }

    @Override
    public void onJump(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        applyJump(state, player);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Don't apply jump to player since that is handled through #onJump
        if (entity.isOnGround() && entity instanceof LivingEntity living && !(living instanceof PlayerEntity) && ((JumpPaddableEntity) living).canJumpOnPad()) {
            ((JumpPaddableEntity) living).setJumpOnPad(false);
            applyJump(state, living);
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
        var state = this.getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
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
        builder.add(POWER, POWERED);
    }
}