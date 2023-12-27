package amymialee.peculiarpieces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.potion.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {
    @WrapOperation(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getType()Lnet/minecraft/entity/EntityType;"))
    protected  EntityType<?> PeculiarPieces$SplashingEnderman(Entity instance, Operation<EntityType<?>> original) {
        var this2 = ((PersistentProjectileEntity) ((Object) this));
        if (this2 instanceof ArrowEntity arrow && ((ArrowEntityAccessor) arrow).getPotion() == Potions.WATER) {
            return EntityType.CREEPER;
        }
        return original.call(instance);
    }
}