package marcasrealaccount.vulkan.device.util;

import org.lwjgl.vulkan.VkPhysicalDevice;

public interface IVulkanPhysicalDeviceScorer {
	public long score(VkPhysicalDevice physicalDevice);
}
