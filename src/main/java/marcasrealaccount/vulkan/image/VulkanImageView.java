package marcasrealaccount.vulkan.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkComponentMapping;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.util.VulkanComponentMapping;
import marcasrealaccount.vulkan.util.VulkanImageSubresourceRange;

public class VulkanImageView extends VulkanHandle<Long> {
	public final VulkanDevice device;
	public final VulkanImage  image;

	public EViewType                         viewType         = EViewType.TYPE_2D;
	public int                               format           = VK12.VK_FORMAT_B8G8R8A8_SRGB;
	public final VulkanComponentMapping      components       = new VulkanComponentMapping(VK12.VK_COMPONENT_SWIZZLE_IDENTITY,
			VK12.VK_COMPONENT_SWIZZLE_IDENTITY, VK12.VK_COMPONENT_SWIZZLE_IDENTITY, VK12.VK_COMPONENT_SWIZZLE_IDENTITY);
	public final VulkanImageSubresourceRange subresourceRange = new VulkanImageSubresourceRange(VK12.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);

	public VulkanImageView(VulkanDevice device, VulkanImage image) {
		super(0L);
		this.device = device;
		this.image  = image;

		this.device.addChild(this);
		this.image.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkImageViewCreateInfo.mallocStack(stack);
			var comps      = VkComponentMapping.mallocStack(stack);
			var sub        = VkImageSubresourceRange.mallocStack(stack);
			var pView      = stack.mallocLong(1);

			comps.set(this.components.r, this.components.g, this.components.b, this.components.a);

			sub.set(this.subresourceRange.aspectMask, this.subresourceRange.baseMipLevel, this.subresourceRange.levelCount,
					this.subresourceRange.baseArrayLayer, this.subresourceRange.layerCount);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO, 0, 0, this.image.getHandle(), this.viewType.getValue(), this.format, comps,
					sub);

			if (VK12.vkCreateImageView(this.device.getHandle(), createInfo, null, pView) == VK12.VK_SUCCESS) this.handle = pView.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyImageView(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
		this.image.removeChild(this);
	}

	public enum EViewType {
		TYPE_1D(VK12.VK_IMAGE_VIEW_TYPE_1D), TYPE_1D_ARRAY(VK12.VK_IMAGE_VIEW_TYPE_1D_ARRAY), TYPE_2D(VK12.VK_IMAGE_VIEW_TYPE_2D),
		TYPE_2D_ARRAY(VK12.VK_IMAGE_VIEW_TYPE_2D_ARRAY), TYPE_3D(VK12.VK_IMAGE_VIEW_TYPE_3D), TYPE_CUBE(VK12.VK_IMAGE_VIEW_TYPE_CUBE),
		TYPE_CUBE_ARRAY(VK12.VK_IMAGE_VIEW_TYPE_CUBE_ARRAY);

		private int value;

		private EViewType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
