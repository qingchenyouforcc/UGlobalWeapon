package org.mmga.uglobal.client.mixin;
import net.minecraft.client.MinecraftClient;
import org.mmga.uglobal.client.ChargeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class ChargeInputMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void uglobalweapon$chargeInput(CallbackInfo ci) {ChargeInput.tick((MinecraftClient)(Object)this);}
}
