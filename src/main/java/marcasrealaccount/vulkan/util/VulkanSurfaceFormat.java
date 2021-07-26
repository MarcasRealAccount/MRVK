package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkSurfaceFormatKHR;

public class VulkanSurfaceFormat {
	public int format;
	public int colorSpace;

	public void set(VkSurfaceFormatKHR format) {
		this.format = format.format();
		this.colorSpace = format.colorSpace();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof VulkanSurfaceFormat))
			return false;

		var objf = (VulkanSurfaceFormat) obj;
		return objf.format == this.format && objf.colorSpace == this.colorSpace;
	}
}
