package marcasrealaccount.vulkan.device.util;

import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import marcasrealaccount.vulkan.device.VulkanPhysicalDevice;
import marcasrealaccount.vulkan.util.VulkanExtent2D;
import marcasrealaccount.vulkan.util.VulkanSurfaceCapabilities;
import marcasrealaccount.vulkan.util.VulkanSurfaceFormat;
import net.minecraft.client.util.Window;

public class VulkanSwapchainSupportDetails {
	public final VulkanSurfaceCapabilities capabilities = new VulkanSurfaceCapabilities();
	public ArrayList<VulkanSurfaceFormat>  formats;
	public ArrayList<Integer>              presentModes;

	public VulkanSurfaceFormat getSwapchainFormat() {
		for (var format : this.formats) {
			if (format.format == VK12.VK_FORMAT_B8G8R8_SRGB && format.colorSpace == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) return format;
		}
		return this.formats.get(0);
	}

	public int getSwapchainPresentMode(boolean vsync) {
		for (var presentMode : this.presentModes) {
			if ((vsync && presentMode == KHRSurface.VK_PRESENT_MODE_FIFO_KHR) || (!vsync && presentMode == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR))
				return presentMode;
		}
		return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
	}

	public VulkanExtent2D getSwapchainExtent(Window window) {
		if (this.capabilities.currentExtent.width != -1) return this.capabilities.currentExtent;

		var extent = new VulkanExtent2D();
		extent.width  = Math.min(Math.max(window.getFramebufferWidth(), this.capabilities.minImageExtent.width),
				this.capabilities.maxImageExtent.width);
		extent.height = Math.min(Math.max(window.getFramebufferHeight(), this.capabilities.minImageExtent.height),
				this.capabilities.maxImageExtent.height);
		return extent;
	}

	public void getSupport(VulkanPhysicalDevice physicalDevice) {
		try (var stack = MemoryStack.stackPush()) {
			var capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
			var count        = stack.mallocInt(1);

			KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.getHandle(), physicalDevice.surface.getHandle(), capabilities);
			this.capabilities.set(capabilities);

			KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getHandle(), physicalDevice.surface.getHandle(), count, null);
			var formats = VkSurfaceFormatKHR.malloc(count.get(0));
			KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getHandle(), physicalDevice.surface.getHandle(), count, formats);
			this.formats = new ArrayList<>(formats.capacity());
			for (int i = 0; i < formats.capacity(); ++i) {
				var format = new VulkanSurfaceFormat();
				format.set(formats.get(i));
				this.formats.add(format);
			}
			formats.free();

			KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.getHandle(), physicalDevice.surface.getHandle(), count, null);
			IntBuffer presentModes = MemoryUtil.memAllocInt(count.get(0));
			KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.getHandle(), physicalDevice.surface.getHandle(), count, presentModes);
			this.presentModes = new ArrayList<>(presentModes.capacity());
			for (int i = 0; i < presentModes.capacity(); ++i) this.presentModes.add(presentModes.get(i));
			MemoryUtil.memFree(presentModes);
		}
	}
}
