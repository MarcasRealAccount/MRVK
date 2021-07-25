package marcasrealaccount.vulkan.mixins.com.mojang.blaze3d.platform;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.TextureUtil;

@Mixin(TextureUtil.class)
public abstract class MixinTextureUtil {
	@Inject(at = @At("HEAD"), cancellable = true, method = "initTexture(Ljava/nio/IntBuffer;II)V")
	private static void disableGLCalls(CallbackInfo info) {
		info.cancel();
	}
}
