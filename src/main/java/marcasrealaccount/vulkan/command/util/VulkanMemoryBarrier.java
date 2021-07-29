package marcasrealaccount.vulkan.command.util;

public class VulkanMemoryBarrier {
	public int srcAccessMask, dstAccessMask;

	public VulkanMemoryBarrier(int srcAccessMask, int dstAccessMask) {
		this.srcAccessMask = srcAccessMask;
		this.dstAccessMask = dstAccessMask;
	}
}
