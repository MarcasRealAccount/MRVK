package marcasrealaccount.vulkan.mixins.com.mojang.blaze3d.systems;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.systems.RenderSystem;

import marcasrealaccount.vulkan.Vulkan;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {
	private static final String FLIP_FRAME_METHOD = "flipFrame(J)V";

	private static final String GLFW_SWAP_BUFFERS_METHOD = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V";

	@Redirect(at = @At(value = "INVOKE", target = GLFW_SWAP_BUFFERS_METHOD, ordinal = 0), method = FLIP_FRAME_METHOD)
	private static void flipFrameSwapBuffers(long window) {
		Vulkan.INSTANCE.endFrame();
		Vulkan.INSTANCE.beginFrame();
	}
}
