package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkExtent3D;

public class VulkanExtent3D {
	public int width, height, depth;

	public void set(VkExtent3D extent) {
		this.width  = extent.width();
		this.height = extent.height();
		this.depth  = extent.depth();
	}
}
