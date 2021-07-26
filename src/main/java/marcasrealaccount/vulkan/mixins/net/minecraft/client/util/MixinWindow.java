package marcasrealaccount.vulkan.mixins.net.minecraft.client.util;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import marcasrealaccount.vulkan.Vulkan;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;

@Mixin(Window.class)
public abstract class MixinWindow {
	private static final String INITIALIZER_METHOD = "<init>(Lnet/minecraft/client/WindowEventHandler;Lnet/minecraft/client/util/MonitorTracker;Lnet/minecraft/client/WindowSettings;Ljava/lang/String;Ljava/lang/String;)V";
	private static final String CLOSE_METHOD = "close()V";
	private static final String SET_VSYNC_METHOD = "setVsync(Z)V";

	private static final String GLFW_CREATE_WINDOW_METHOD = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J";
	private static final String GLFW_MAKE_CONTEXT_CURRENT_METHOD = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V";
	private static final String GLFW_FREE_CALLBACKS_METHOD = "Lorg/lwjgl/glfw/Callbacks;glfwFreeCallbacks(J)V";
	private static final String GLFW_SWAP_INTERVAL_METHOD = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V";
	private static final String GL_CREATE_CAPABILITIES_METHOD = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;";

	@Shadow
	private long handle;
	@Shadow
	private boolean vsync;

	@Inject(at = @At(value = "INVOKE", target = GLFW_CREATE_WINDOW_METHOD, ordinal = 0), method = INITIALIZER_METHOD)
	private void initializerChangeWindowHints(WindowEventHandler eventHandler, MonitorTracker monitorTracker,
			WindowSettings settings, @Nullable String videoMode, String title, CallbackInfo info) {
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
	}

	@Redirect(at = @At(value = "INVOKE", target = GLFW_MAKE_CONTEXT_CURRENT_METHOD, ordinal = 0), method = INITIALIZER_METHOD)
	private void initializerInitVulkan(long window) {
		Vulkan.INSTANCE.initVulkan((Window) (Object) this, this.vsync);
	}

	@Redirect(at = @At(value = "INVOKE", target = GL_CREATE_CAPABILITIES_METHOD, ordinal = 0), method = INITIALIZER_METHOD)
	private GLCapabilities initializerCreateCapabilities() {
		return null;
	}

	@Inject(at = @At(value = "TAIL"), method = INITIALIZER_METHOD)
	private void initializerRunVulkanTest(CallbackInfo info) {
		GLFW.glfwSetWindowIconifyCallback(this.handle, this::onWindowMinimized);

		Vulkan.INSTANCE.testVulkan((Window) (Object) this);
	}

	@Inject(at = @At(value = "INVOKE", target = GLFW_FREE_CALLBACKS_METHOD, ordinal = 0), method = CLOSE_METHOD)
	private void closeVulkan(CallbackInfo info) {
		Vulkan.INSTANCE.destroy();
	}

	@Redirect(at = @At(value = "INVOKE", target = GLFW_SWAP_INTERVAL_METHOD, ordinal = 0), method = SET_VSYNC_METHOD)
	private void setInterval(int interval) {
		Vulkan.INSTANCE.setVSync(interval != 0);
	}

	private void onWindowMinimized(long window, boolean minimized) {
		Vulkan.INSTANCE.setMinimized(minimized);
	}

	@Shadow
	public abstract boolean shouldClose();

	@Shadow
	public abstract void close();
}
