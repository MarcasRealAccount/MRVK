package marcasrealaccount.vulkan.surface;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.image.VulkanImage;

public class VulkanSwapchain extends VulkanHandle<Long> {
	public final VulkanDevice  device;
	public final VulkanSurface surface;

	private ArrayList<VulkanImage> images = null;

	public int                    imageCount       = 2;
	public int                    imageArrayLayers = 1;
	public int                    imageUsage       = VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
	public int                    compositeAlpha   = KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
	public int                    preTransform     = KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
	public int                    format           = VK12.VK_FORMAT_B8G8R8_SRGB;
	public int                    colorSpace       = KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
	public int                    presentMode      = KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
	public int                    width            = 0, height = 0;
	public boolean                clipped          = true;
	public final HashSet<Integer> indices          = new HashSet<>();

	public VulkanSwapchain(VulkanDevice device) {
		super(0L);
		this.device  = device;
		this.surface = this.device.physicalDevice.surface;

		this.device.addChild(this);
		this.surface.addChild(this);
	}

	@Override
	protected void createAbstract() {
		int imageSharingMode = VK12.VK_SHARING_MODE_EXCLUSIVE;

		try (var stack = MemoryStack.stackPush()) {
			var       pSwapchain  = stack.mallocLong(1);
			var       pImageCount = stack.mallocInt(1);
			var       createInfo  = VkSwapchainCreateInfoKHR.mallocStack(stack);
			var       pExtent     = VkExtent2D.mallocStack(stack);
			IntBuffer pIndices    = null;

			pExtent.set(this.width, this.height);

			if (this.indices.size() > 1) {
				imageSharingMode = VK12.VK_SHARING_MODE_CONCURRENT;
				pIndices         = MemoryUtil.memAllocInt(this.indices.size());
				int i = 0;
				for (var index : this.indices) {
					pIndices.put(i, index);
					++i;
				}
			}

			createInfo.set(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR, 0, 0, this.surface.getHandle(), this.imageCount, this.format,
					this.colorSpace, pExtent, this.imageArrayLayers, this.imageUsage, imageSharingMode, pIndices, this.preTransform,
					this.compositeAlpha, this.presentMode, this.clipped, this.handle);

			if (KHRSwapchain.vkCreateSwapchainKHR(this.device.getHandle(), createInfo, null, pSwapchain) == VK12.VK_SUCCESS) {
				this.handle = pSwapchain.get(0);

				KHRSwapchain.vkGetSwapchainImagesKHR(this.device.getHandle(), this.handle, pImageCount, null);
				var pImages = MemoryUtil.memAllocLong(pImageCount.get(0));
				KHRSwapchain.vkGetSwapchainImagesKHR(this.device.getHandle(), this.handle, pImageCount, pImages);

				this.images = new ArrayList<>(pImages.capacity());
				for (int i = 0; i < pImages.capacity(); ++i) images.add(new VulkanImage(this.device, pImages.get(i)));

				MemoryUtil.memFree(pImages);
			}

			if (pIndices != null) MemoryUtil.memFree(pIndices);
		}
	}

	@Override
	protected void destroyAbstract() {
		if (!this.recreate) KHRSwapchain.vkDestroySwapchainKHR(this.device.getHandle(), this.handle, null);
		this.images = null;
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
		this.surface.removeChild(this);
	}

	public int getNumImages() {
		return this.images.size();
	}

	public VulkanImage getImage(int index) {
		return index >= 0 && index < this.images.size() ? this.images.get(index) : null;
	}
}
