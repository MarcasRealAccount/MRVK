package marcasrealaccount.vulkan.util;

public class VulkanImageSubresourceLayers {
	public int aspectMask;
	public int mipLevel;
	public int baseArrayLayer;
	public int layerCount;

	public VulkanImageSubresourceLayers(int aspectMask, int mipLevel, int baseArrayLayer, int layerCount) {
		this.aspectMask     = aspectMask;
		this.mipLevel       = mipLevel;
		this.baseArrayLayer = baseArrayLayer;
		this.layerCount     = layerCount;
	}
}
