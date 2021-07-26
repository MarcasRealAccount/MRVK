package marcasrealaccount.vulkan.util;

import java.util.Optional;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import marcasrealaccount.vulkan.surface.VulkanSurface;

public class VulkanQueueFamilyIndices {
	public Optional<Integer> graphicsFamily;
	public Optional<Integer> presentFamily;

	public boolean isComplete() {
		return this.graphicsFamily.isPresent() && this.presentFamily.isPresent();
	}

	public static VulkanQueueFamilyIndices getIndices(VkPhysicalDevice physicalDevice, VulkanSurface surface) {
		var indices = new VulkanQueueFamilyIndices();

		try (var stack = MemoryStack.stackPush()) {
			var queueFamilyCount = stack.mallocInt(1);
			var presentSupport = stack.mallocInt(1);

			VK12.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
			var queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0));
			VK12.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);

			int i = 0;
			for (var queueFamily : queueFamilies) {
				if ((queueFamily.queueFlags() & VK12.VK_QUEUE_GRAPHICS_BIT) != 0)
					indices.graphicsFamily = Optional.of(i);

				KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.getHandle(), presentSupport);
				if (presentSupport.get(0) != 0)
					indices.presentFamily = Optional.of(i);

				if (indices.isComplete())
					break;

				++i;
			}
		}

		return indices;
	}
}
