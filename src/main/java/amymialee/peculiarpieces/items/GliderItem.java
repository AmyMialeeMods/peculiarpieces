package amymialee.peculiarpieces.items;

import amymialee.peculiarpieces.registry.PeculiarItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GliderItem extends Item {
    public GliderItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (!stack.isEmpty()) this.toggle(stack);
        return TypedActionResult.success(stack);
    }

    private void toggle(ItemStack stack) {
        var compound = stack.getOrCreateNbt();
        compound.putBoolean("pp:gliding", !compound.getBoolean("pp:gliding"));
    }

    public static boolean hasGlider(LivingEntity player) {
        var main = player.getMainHandStack();
        if (isActive(main)) {
            return true;
        }
        var off = player.getOffHandStack();
        return isActive(off);
    }

    public static ItemStack getGlider(LivingEntity player) {
        var main = player.getMainHandStack();
        if (isActive(main)) {
            return main;
        }
        var off = player.getOffHandStack();
        if (isActive(off)) {
            return off;
        }
        return null;
    }

    public static boolean isActiveGlider(ItemStack stack) {
        return stack.getItem() == PeculiarItems.HANG_GLIDER && isActive(stack);
    }

    public static boolean isActive(ItemStack stack) {
        return !stack.isEmpty() && stack.getNbt() != null && stack.getNbt().getBoolean("pp:gliding");
    }

    public static boolean isGliding(LivingEntity player) {
        if (!player.isOnGround() && !player.isSubmergedInWater() && !player.isSleeping()) {
            var velocity = player.getVelocity();
            return hasGlider(player) && velocity.getY() < 0;
        }
        return false;
    }
}