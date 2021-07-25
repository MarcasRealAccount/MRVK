package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkExtent2D;

public class VulkanExtent2D {
	public int width, height;

	public void set(VkExtent2D extent) {
		this.width = extent.width();
		this.height = extent.height();
	}
}
