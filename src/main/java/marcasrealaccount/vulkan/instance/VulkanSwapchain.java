package marcasrealaccount.vulkan.instance;

import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import marcasrealaccount.vulkan.instance.image.VulkanImage;
import marcasrealaccount.vulkan.instance.image.VulkanImageView;
import marcasrealaccount.vulkan.util.VulkanExtent2D;
import marcasrealaccount.vulkan.util.VulkanSurfaceFormat;
import net.minecraft.client.util.Window;

public class VulkanSwapchain extends VulkanHandle<Long> {
	public final VulkanDevice device;

	private Window window;
	private boolean vsync;

	private ArrayList<VulkanImage> images = null;
	private ArrayList<VulkanImageView> imageViews = null;
	private VulkanSurfaceFormat format = null;
	private int presentMode = 0;
	private VulkanExtent2D extent = null;

	public VulkanSwapchain(VulkanDevice device, Window window, boolean vsync) {
		super(0L);
		this.device = device;
		this.window = window;
		this.vsync = vsync;
	}

	@Override
	protected void createAbstract() {
		var physicalDevice = this.device.physicalDevice;
		var swapchainSupportDetails = physicalDevice.swapchainSupportDetails;
		var queueIndices = physicalDevice.indices;

		int imageCount = Math.min(swapchainSupportDetails.capabilities.minImageCount + 1,
				swapchainSupportDetails.capabilities.maxImageCount);

		this.format = swapchainSupportDetails.getSwapchainFormat();
		this.presentMode = swapchainSupportDetails.getSwapchainPresentMode(this.vsync);
		this.extent = swapchainSupportDetails.getSwapchainExtent(this.window);

		int imageSharingMode = VK12.VK_SHARING_MODE_EXCLUSIVE;

		try (var stack = MemoryStack.stackPush()) {
			var pSwapchain = stack.mallocLong(1);
			var pImageCount = stack.mallocInt(1);
			var createInfo = VkSwapchainCreateInfoKHR.mallocStack(stack);
			var pExtent = VkExtent2D.mallocStack(stack);
			IntBuffer indices = null;

			pExtent.set(this.extent.width, this.extent.height);

			if (!queueIndices.graphicsFamily.equals(queueIndices.presentFamily)) {
				imageSharingMode = VK12.VK_SHARING_MODE_CONCURRENT;
				indices = stack.mallocInt(2);
				indices.put(0, queueIndices.graphicsFamily.get());
				indices.put(1, queueIndices.presentFamily.get());
			}

			createInfo.set(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR, 0, 0,
					physicalDevice.surface.getHandle(), imageCount, this.format.format, this.format.colorSpace, pExtent,
					1, VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, imageSharingMode, indices,
					swapchainSupportDetails.capabilities.currentTransform, KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
					this.presentMode, true, this.handle);

			if (KHRSwapchain.vkCreateSwapchainKHR(this.device.getHandle(), createInfo, null,
					pSwapchain) == VK12.VK_SUCCESS) {
				this.handle = pSwapchain.get(0);
				this.device.addInvalidate(this);

				KHRSwapchain.vkGetSwapchainImagesKHR(this.device.getHandle(), this.handle, pImageCount, null);
				var pImages = MemoryUtil.memAllocLong(pImageCount.get(0));
				KHRSwapchain.vkGetSwapchainImagesKHR(this.device.getHandle(), this.handle, pImageCount, pImages);

				this.images = new ArrayList<>(pImages.capacity());
				this.imageViews = new ArrayList<>(pImages.capacity());
				for (int i = 0; i < pImages.capacity(); ++i) {
					var image = new VulkanImage(this.device, pImages.get(i));
					var imageView = new VulkanImageView(this.device, image);
					imageView.format = this.format.format;
					if (!imageView.create()) {
						this.handle = 0L;
						this.device.removeInvalidate(this);
						this.images = null;
						this.imageViews = null;
						break;
					}
					images.add(image);
					imageViews.add(imageView);
				}

				MemoryUtil.memFree(pImages);
			}
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		if (!recreate) {
			KHRSwapchain.vkDestroySwapchainKHR(this.device.getHandle(), this.handle, null);
			if (!wasInvalidated)
				this.device.removeInvalidate(this);
		}

		this.imageViews = null;
		this.images = null;
		this.format = null;
		this.presentMode = 0;
		this.extent = null;
	}

	public int getNumImages() {
		return this.images.size();
	}

	public VulkanSurfaceFormat getFormat() {
		return this.format;
	}

	public int getPresentMode() {
		return this.presentMode;
	}

	public VulkanExtent2D getExtent() {
		return this.extent;
	}

	public VulkanImage getImage(int index) {
		return index >= 0 && index < this.images.size() ? this.images.get(index) : null;
	}

	public VulkanImageView getImageView(int index) {
		return index >= 0 && index < this.imageViews.size() ? this.imageViews.get(index) : null;
	}

	public void setWindow(Window window) {
		if (this.window != window)
			this.invalidated = true;
		this.window = window;
	}

	public void setVSync(boolean vsync) {
		if (this.vsync != vsync)
			this.invalidated = true;
		this.vsync = vsync;
	}
}
