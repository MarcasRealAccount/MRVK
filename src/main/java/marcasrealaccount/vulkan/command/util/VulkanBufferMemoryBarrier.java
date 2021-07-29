package marcasrealaccount.vulkan.command.util;

import marcasrealaccount.vulkan.memory.VulkanBuffer;

public class VulkanBufferMemoryBarrier {
	public int          srcAccessMask, dstAccessMask;
	public int          srcQueueFamilyIndex, dstQueueFamilyIndex;
	public VulkanBuffer buffer;
	public long         offset, size;

	public VulkanBufferMemoryBarrier(int srcAccessMask, int dstAccessMask, int srcQueueFamilyIndex, int dstQueueFamilyIndex, VulkanBuffer buffer,
			long offset, long size) {
		this.srcAccessMask       = srcAccessMask;
		this.dstAccessMask       = dstAccessMask;
		this.srcQueueFamilyIndex = srcQueueFamilyIndex;
		this.dstQueueFamilyIndex = dstQueueFamilyIndex;
		this.buffer              = buffer;
		this.offset              = offset;
		this.size                = size;
	}
}
