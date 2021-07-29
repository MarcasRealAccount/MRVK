package marcasrealaccount.vulkan.command.util;

import marcasrealaccount.vulkan.image.VulkanImage;
import marcasrealaccount.vulkan.util.VulkanImageSubresourceRange;

public class VulkanImageMemoryBarrier {
	public int         srcAccessMask, dstAccessMask;
	public int         srcQueueFamilyIndex, dstQueueFamilyIndex;
	public int         oldLayout, newLayout;
	public VulkanImage image;

	public VulkanImageSubresourceRange subresourceRange;

	public VulkanImageMemoryBarrier(int srcAccessMask, int dstAccessMask, int srcQueueFamilyIndex, int dstQueueFamilyIndex, int oldLayout,
			int newLayout, VulkanImage image, VulkanImageSubresourceRange subresourceRange) {
		this.srcAccessMask       = srcAccessMask;
		this.dstAccessMask       = dstAccessMask;
		this.srcQueueFamilyIndex = srcQueueFamilyIndex;
		this.dstQueueFamilyIndex = dstQueueFamilyIndex;
		this.oldLayout           = oldLayout;
		this.newLayout           = newLayout;
		this.image               = image;
		this.subresourceRange    = subresourceRange;
	}
}
