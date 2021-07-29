package marcasrealaccount.vulkan.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.util.VulkanExtent3D;

public class VulkanImage extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public EImageType           imageType          = EImageType.TYPE_2D;
	public int                  format             = VK12.VK_FORMAT_B8G8R8A8_SRGB;
	public final VulkanExtent3D extent             = new VulkanExtent3D();
	public int                  mipLevels          = 0;
	public int                  arrayLayers        = 0;
	public int                  samples            = 0;
	public EImageTiling         tiling             = EImageTiling.LINEAR;
	public int                  usage              = VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
	public int                  sharingMode        = VK12.VK_SHARING_MODE_EXCLUSIVE;
	public int[]                queueFamilyIndices = null;
	public int                  initialLayout      = VK12.VK_IMAGE_LAYOUT_GENERAL;

	public VulkanImage(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	public VulkanImage(VulkanDevice device, long handle) {
		super(0L, false);
		this.handle = handle;
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkImageCreateInfo.mallocStack(stack);
			var pImage     = stack.mallocLong(1);
			var pExtent    = VkExtent3D.mallocStack(stack);

			pExtent.set(this.extent.width, this.extent.height, this.extent.depth);

			var pQueueFamilyIndices = MemoryUtil.memAllocInt(this.queueFamilyIndices.length);
			pQueueFamilyIndices.put(this.queueFamilyIndices);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO, 0, 0, this.imageType.getValue(), this.format, pExtent, this.mipLevels,
					this.arrayLayers, this.samples, this.tiling.getValue(), this.usage, this.sharingMode, pQueueFamilyIndices, this.initialLayout);

			if (VK12.vkCreateImage(this.device.getHandle(), createInfo, null, pImage) == VK12.VK_SUCCESS) this.handle = pImage.get(0);

			MemoryUtil.memFree(pQueueFamilyIndices);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyImage(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public enum EImageType {
		TYPE_1D(VK12.VK_IMAGE_TYPE_1D), TYPE_2D(VK12.VK_IMAGE_TYPE_2D), TYPE_3D(VK12.VK_IMAGE_TYPE_3D);

		private int value;

		private EImageType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	public enum EImageTiling {
		OPTIMAL(VK12.VK_IMAGE_TILING_OPTIMAL), LINEAR(VK12.VK_IMAGE_TILING_LINEAR);

		private int value;

		private EImageTiling(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
