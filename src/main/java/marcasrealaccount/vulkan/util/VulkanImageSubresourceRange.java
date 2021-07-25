package marcasrealaccount.vulkan.util;

public class VulkanImageSubresourceRange {
	public int aspectMask, baseMipLevel, levelCount, baseArrayLayer, layerCount;

	public VulkanImageSubresourceRange(int aspectMask, int baseMipLevel, int levelCount, int baseArrayLayer,
			int layerCount) {
		this.aspectMask = aspectMask;
		this.baseMipLevel = baseMipLevel;
		this.levelCount = levelCount;
		this.baseArrayLayer = baseArrayLayer;
		this.layerCount = layerCount;
	}
}
