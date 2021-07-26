package marcasrealaccount.vulkan.surface;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK12;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.instance.VulkanInstance;
import net.minecraft.client.util.Window;

public class VulkanSurface extends VulkanHandle<Long> {
	public final VulkanInstance instance;

	public long windowHandle = 0;

	public VulkanSurface(VulkanInstance instance) {
		super(0L);
		this.instance = instance;

		this.instance.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var pSurface = stack.mallocLong(1);
			if (GLFWVulkan.glfwCreateWindowSurface(this.instance.getHandle(), this.windowHandle, null,
					pSurface) == VK12.VK_SUCCESS)
				this.handle = pSurface.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		KHRSurface.vkDestroySurfaceKHR(this.instance.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.instance.removeChild(this);
	}

	public void setWindow(Window window) {
		this.windowHandle = window.getHandle();
	}
}
