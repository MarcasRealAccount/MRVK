package marcasrealaccount.vulkan.device;

import org.lwjgl.vulkan.VkPhysicalDevice;

public interface IVulkanPhysicalDeviceScorer {
	public long score(VkPhysicalDevice physicalDevice);
}
