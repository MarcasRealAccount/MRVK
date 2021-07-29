package marcasrealaccount.vulkan.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanSampler extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public int magFilter    = VK12.VK_FILTER_LINEAR;
	public int minFilter    = VK12.VK_FILTER_LINEAR;
	public int mipmapMode   = VK12.VK_SAMPLER_MIPMAP_MODE_LINEAR;
	public int addressModeU = VK12.VK_SAMPLER_ADDRESS_MODE_REPEAT;
	public int addressModeV = VK12.VK_SAMPLER_ADDRESS_MODE_REPEAT;
	public int addressModeW = VK12.VK_SAMPLER_ADDRESS_MODE_REPEAT;

	public boolean anisotropyEnable = false;
	public float   maxAnisotropy    = 0.0f;

	public boolean compareEnable = false;
	public int     compareOp     = VK12.VK_COMPARE_OP_ALWAYS;

	public float mipLodBias = 0.0f;
	public float minLod     = 0.0f;
	public float maxLod     = 0.0f;

	public int borderColor = VK12.VK_BORDER_COLOR_INT_OPAQUE_BLACK;

	public boolean unnormalizedCoordinates = false;

	public VulkanSampler(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkSamplerCreateInfo.mallocStack(stack);
			var pSampler   = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO, 0, 0, this.magFilter, this.minFilter, this.mipmapMode, this.addressModeU,
					this.addressModeV, this.addressModeW, this.mipLodBias, this.anisotropyEnable, this.maxAnisotropy, this.compareEnable,
					this.compareOp, this.minLod, this.maxLod, this.borderColor, this.unnormalizedCoordinates);

			if (VK12.vkCreateSampler(this.device.getHandle(), createInfo, null, pSampler) == VK12.VK_SUCCESS) this.handle = pSampler.get(0);
		}
	}

	@Override
	protected boolean destroyAbstract() {
		VK12.vkDestroySampler(this.device.getHandle(), this.handle, null);
		return true;
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

}
