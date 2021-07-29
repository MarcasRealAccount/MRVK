package marcasrealaccount.vulkan.device.util;

import java.util.Optional;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import marcasrealaccount.vulkan.device.VulkanPhysicalDevice;

public class VulkanQueueFamilyIndices {
	public Optional<Integer> graphicsFamily = Optional.empty();

	public boolean isComplete() {
		return this.graphicsFamily.isPresent();
	}

	public void getIndices(VulkanPhysicalDevice physicalDevice) {
		this.graphicsFamily = Optional.empty();

		int requiredFlags = VK12.VK_QUEUE_GRAPHICS_BIT;

		try (var stack = MemoryStack.stackPush()) {
			var queueFamilyCount = stack.mallocInt(1);
			var presentSupport   = stack.mallocInt(1);

			VK12.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.getHandle(), queueFamilyCount, null);
			var queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0));
			VK12.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.getHandle(), queueFamilyCount, queueFamilies);

			int i = 0;
			for (var queueFamily : queueFamilies) {
				KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.getHandle(), i, physicalDevice.surface.getHandle(), presentSupport);
				if ((queueFamily.queueFlags() & requiredFlags) == requiredFlags && presentSupport.get(0) != 0) this.graphicsFamily = Optional.of(i);

				if (this.isComplete()) break;

				++i;
			}
		}
	}
}
