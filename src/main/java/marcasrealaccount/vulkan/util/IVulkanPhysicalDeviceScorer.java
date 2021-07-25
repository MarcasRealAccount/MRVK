package marcasrealaccount.vulkan.util;

import marcasrealaccount.vulkan.instance.VulkanPhysicalDevice;

public interface IVulkanPhysicalDeviceScorer {
	public long score(VulkanPhysicalDevice physicalDevice);
}
