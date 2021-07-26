package marcasrealaccount.vulkan.mixins.net.minecraft.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import marcasrealaccount.vulkan.Vulkan;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
	private static final String ON_RESOLUTION_CHANGED_METHOD = "onResolutionChanged()V";
	private static final String ON_CURSOR_ENTER_CHANGED_METHOD = "onCursorEnterChanged()V";

	@Inject(at = @At("HEAD"), cancellable = true, method = ON_RESOLUTION_CHANGED_METHOD)
	private void onResolutionChangedUpdateSwapchain(CallbackInfo info) {
		Vulkan.INSTANCE.recreateSwapchain();
		info.cancel();
	}

	@Inject(at = @At("HEAD"), cancellable = true, method = ON_CURSOR_ENTER_CHANGED_METHOD)
	private void onCursorEnterChangedDisable(CallbackInfo info) {
		info.cancel();
	}
}
