package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkSurfaceFormatKHR;

public class VulkanSurfaceFormat {
	public int format;
	public int colorSpace;

	public void set(VkSurfaceFormatKHR format) {
		this.format = format.format();
		this.colorSpace = format.colorSpace();
	}
}
