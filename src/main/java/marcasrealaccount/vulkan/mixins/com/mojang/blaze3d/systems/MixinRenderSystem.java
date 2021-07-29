package marcasrealaccount.vulkan.mixins.com.mojang.blaze3d.systems;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import marcasrealaccount.vulkan.Vulkan;
import net.minecraft.client.render.Tessellator;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {
	private static final String FLIP_FRAME_METHOD = "flipFrame(J)V";

	@Inject(at = @At("HEAD"), cancellable = true, method = FLIP_FRAME_METHOD)
	private static void flipFrameOverride(long window, CallbackInfo info) {
		Vulkan.INSTANCE.endFrame();
		GLFW.glfwPollEvents();
		Vulkan.INSTANCE.beginFrame();
		replayQueue();
		Tessellator.getInstance().getBuffer().clear();
		info.cancel();
	}

	@Shadow
	public static void replayQueue() {}
}
