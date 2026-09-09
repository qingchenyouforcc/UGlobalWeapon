package org.mmga.uglobal.client.mixin;
import net.minecraft.item.ItemStack;
import org.mmga.uglobal.client.RpgViewEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class WeaponGlintMixin {
    @Inject(method="hasGlint",at=@At("HEAD"),cancellable=true)
    private void uglobalweapon$matteWeapon(CallbackInfoReturnable<Boolean> cir) {
        if(RpgViewEffects.isRpg((ItemStack)(Object)this)) cir.setReturnValue(false);
    }
}
