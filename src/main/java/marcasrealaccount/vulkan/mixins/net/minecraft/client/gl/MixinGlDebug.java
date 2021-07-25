package marcasrealaccount.vulkan.mixins.net.minecraft.client.gl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gl.GlDebug;

@Mixin(GlDebug.class)
public abstract class MixinGlDebug {
	@Inject(at = @At("HEAD"), cancellable = true, method = "enableDebug(IZ)V")
	private static void disableGLCalls(CallbackInfo info) {
		info.cancel();
	}
}
