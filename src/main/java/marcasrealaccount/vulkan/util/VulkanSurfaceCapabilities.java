package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

public class VulkanSurfaceCapabilities {
	public int minImageCount, maxImageCount;
	public final VulkanExtent2D currentExtent = new VulkanExtent2D();
	public final VulkanExtent2D minImageExtent = new VulkanExtent2D(), maxImageExtent = new VulkanExtent2D();
	public int maxImageArrayLayers;
	public int supportedTransforms, currentTransform;
	public int supportedCompositeAlpha;
	public int supportedUsageFlags;

	public void set(VkSurfaceCapabilitiesKHR capabilities) {
		this.minImageCount = capabilities.minImageCount();
		this.maxImageCount = capabilities.maxImageCount();
		this.currentExtent.set(capabilities.currentExtent());
		this.minImageExtent.set(capabilities.minImageExtent());
		this.maxImageExtent.set(capabilities.maxImageExtent());
		this.maxImageArrayLayers = capabilities.maxImageArrayLayers();
		this.supportedTransforms = capabilities.supportedTransforms();
		this.currentTransform = capabilities.currentTransform();
		this.supportedCompositeAlpha = capabilities.supportedCompositeAlpha();
		this.supportedUsageFlags = capabilities.supportedUsageFlags();
	}
}
