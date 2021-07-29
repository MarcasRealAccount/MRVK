package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkExtent3D;

public class VulkanExtent3D {
	public int width, height, depth;

	public VulkanExtent3D(int width, int height, int depth) {
		this.width  = width;
		this.height = height;
		this.depth  = depth;
	}

	public void set(VkExtent3D extent) {
		this.width  = extent.width();
		this.height = extent.height();
		this.depth  = extent.depth();
	}
}
