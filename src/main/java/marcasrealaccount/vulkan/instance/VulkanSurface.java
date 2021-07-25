package marcasrealaccount.vulkan.instance;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK12;

import net.minecraft.client.util.Window;

public class VulkanSurface extends VulkanHandle<Long> {
	public final VulkanInstance instance;
	private Window window;

	public VulkanSurface(VulkanInstance instance, Window window) {
		super(0L);
		this.instance = instance;
		this.window = window;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var pSurface = stack.mallocLong(1);
			if (GLFWVulkan.glfwCreateWindowSurface(this.instance.getHandle(), this.window.getHandle(), null,
					pSurface) == VK12.VK_SUCCESS) {
				this.handle = pSurface.get(0);
				this.instance.addInvalidate(this);
			}
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		KHRSurface.vkDestroySurfaceKHR(this.instance.getHandle(), this.handle, null);
		if (!wasInvalidated)
			this.instance.removeInvalidate(this);
	}

	public void setWindow(Window window) {
		this.window = window;
	}
}
