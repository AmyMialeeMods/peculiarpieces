package amymialee.peculiarpieces.mixin;

import amymialee.peculiarpieces.PeculiarPieces;
import amymialee.peculiarpieces.registry.PeculiarItems;
import amymialee.peculiarpieces.util.ExtraPlayerDataWrapper;
import amymialee.peculiarpieces.util.JumpPaddableEntity;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("ConstantConditions")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements JumpPaddableEntity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract void setHealth(float health);
    @Shadow public abstract boolean clearStatusEffects();
    @Shadow public abstract boolean addStatusEffect(StatusEffectInstance effect);
    @Shadow public abstract boolean isClimbing();
    @Shadow public abstract boolean isHoldingOntoLadder();
    @Shadow protected abstract SoundEvent getDrinkSound(ItemStack stack);
    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);

    @Shadow public abstract ItemStack getStackInHand(Hand hand);

    @Unique
    private Vec3d PeculiarPieces$lastJumpPadPos;
    @Unique
    private boolean PeculiarPieces$jumpOnPad = true;

    @Inject(method = "fall", at = @At("HEAD"))
    public void PeculiarPieces$FallHead(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && player instanceof ExtraPlayerDataWrapper extraPlayerDataWrapper) {
            var optionalComponent = TrinketsApi.getTrinketComponent(player);
            if (optionalComponent.isPresent() && optionalComponent.get().isEquipped(PeculiarItems.BOUNCY_BOOTS)) {
                if (!this.isSneaking()) {
                    // TODO[una] this moved to getMovementSpeed, but they seem to work fine even without this?
//                    this.airStrafingSpeed *= 4;
                    if (onGround) {
                        if (this.fallDistance > 0.0f) {
                            extraPlayerDataWrapper.setBouncePower(Math.pow(Math.abs(this.getVelocity().getY()), 1.5) - 0.05);
                            return;
                        }
                    }
                }
                this.fallDistance = 0;
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void PeculiarPieces$DamageInvulnerability(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasStatusEffect(PeculiarPieces.INVULNERABILITY_EFFECT)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    public void PeculiarPieces$ApplyInvulnerability(DamageSource source, float amount, CallbackInfo ci) {
        if (this.hasStatusEffect(PeculiarPieces.INVULNERABILITY_EFFECT)) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnConsumptionEffects", at = @At("HEAD"), cancellable = true)
    protected void PeculiarPieces$QuietHiddenPotions(ItemStack stack, int particleCount, CallbackInfo ci) {
        if (stack.getItem() == PeculiarItems.HIDDEN_POTION) {
            if (stack.getUseAction() == UseAction.DRINK) {
                this.playSound(this.getDrinkSound(stack), 0.1f, this.getWorld().random.nextFloat() * 0.1f + 0.95f);
            }
            ci.cancel();
        }
    }

    @ModifyVariable(method = "travel", at = @At("STORE"))
    public float PeculiarPieces$SlipperyShoesSlipping(float p) {
        if (((Object) this) instanceof LivingEntity livingEntity) {
            var optionalComponent = TrinketsApi.getTrinketComponent(livingEntity);
            if (optionalComponent.isPresent() && optionalComponent.get().isEquipped(PeculiarItems.SLIPPERY_SHOES)) {
                return 1f / 0.91f;
            }
            if (optionalComponent.isPresent() && optionalComponent.get().isEquipped(PeculiarItems.STEADY_SNEAKERS)) {
                return 0.6f;
            }
        }
        return p;
    }

    @Unique
    private BlockState peculiarPiecesRealBlockState; 

    @Inject(method = "applyClimbingSpeed", at = @At("HEAD"))
    private void PeculiarPieces$MoreScaffoldsStart(Vec3d motion, CallbackInfoReturnable<Vec3d> cir) {
        if (this.getBlockStateAtPos().isIn(PeculiarPieces.SCAFFOLDING)) {
            this.peculiarPiecesRealBlockState = this.getBlockStateAtPos();
            ((EntityAccessor)this).setBlockStateAtPos(Blocks.SCAFFOLDING.getDefaultState());
            
        }
    }
    @Inject(method = "applyClimbingSpeed", at = @At("RETURN"))
    private void PeculiarPieces$MoreScaffoldsEnd(Vec3d motion, CallbackInfoReturnable<Vec3d> cir) {
        ((EntityAccessor)this).setBlockStateAtPos(this.peculiarPiecesRealBlockState);
        this.peculiarPiecesRealBlockState = null;
    }

    @Inject(method = "tryUseTotem", at = @At("RETURN"), cancellable = true)
    private void PeculiarPieces$TotemTrinkets(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
                return;
            }
            if (((Object) this) instanceof LivingEntity livingEntity) {
                for (var hand : Hand.values()) {
                    var itemStack = this.getStackInHand(hand);
                    if (itemStack.isIn(PeculiarPieces.TOTEMS)) {
                        this.useTotem(livingEntity, itemStack);
                        if (itemStack.isOf(PeculiarItems.EVERLASTING_EMBLEM)) {
                            if (livingEntity instanceof PlayerEntity player) {
                                player.getItemCooldownManager().set(PeculiarItems.EVERLASTING_EMBLEM, (source.getAttacker() instanceof PlayerEntity ? 2 : 1) * 6 * 60 * 20);
                            }
                        } else if (!itemStack.isOf(PeculiarItems.PERPETUAL_FIGURE)) {
                            itemStack.decrement(1);
                        }
                        cir.setReturnValue(true);
                        return;
                    }
                }
                var optionalComponent = TrinketsApi.getTrinketComponent(livingEntity);
                if (optionalComponent.isPresent()) {
                    if (optionalComponent.get().isEquipped(PeculiarItems.TOKEN_OF_UNDYING)) {
                        var equipped = optionalComponent.get().getEquipped(PeculiarItems.TOKEN_OF_UNDYING);
                        var stack = equipped.get(0).getRight();
                        this.useTotem(livingEntity, stack);
                        stack.decrement(1);
                        cir.setReturnValue(true);
                    } else if (optionalComponent.get().isEquipped(PeculiarItems.EVERLASTING_EMBLEM)) {
                        if (livingEntity instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(PeculiarItems.EVERLASTING_EMBLEM)) {
                            var equipped = optionalComponent.get().getEquipped(PeculiarItems.EVERLASTING_EMBLEM);
                            var stack = equipped.get(0).getRight();
                            this.useTotem(livingEntity, stack);
                            player.getItemCooldownManager().set(PeculiarItems.EVERLASTING_EMBLEM, (source.getAttacker() instanceof PlayerEntity ? 2 : 1) * 6 * 60 * 20);
                            cir.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }

    private void useTotem(LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            Criteria.USED_TOTEM.trigger(serverPlayer, stack);
        }
        this.setHealth(1.0f);
        this.clearStatusEffects();
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        this.getWorld().sendEntityStatus(this, EntityStatuses.USE_TOTEM_OF_UNDYING);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void PeculiarPieces$endTick(CallbackInfo ci) {
        // If the entity can't use the jump pad and is greater than 1 block away from their original position
        // Adds in the radius of the entity's largest side to avoid some level of double jumps
        if (!this.canJumpOnPad() && this.isOnGround() && this.getPos().squaredDistanceTo(this.PeculiarPieces$lastJumpPadPos) > Math.pow(1 + Math.max(this.getBoundingBox().getZLength(), this.getBoundingBox().getXLength()) / 2, 2)) {
            this.PeculiarPieces$lastJumpPadPos = null;
            this.setJumpOnPad(true);
        }
    }

    @Override
    public void setJumpOnPad(boolean jumpOnPad) {
        this.PeculiarPieces$jumpOnPad = jumpOnPad;

        if (!this.canJumpOnPad()) {
            this.PeculiarPieces$lastJumpPadPos = this.getPos();
        }
    }

    @Override
    public boolean canJumpOnPad() {
        return this.PeculiarPieces$jumpOnPad;
    }
}
