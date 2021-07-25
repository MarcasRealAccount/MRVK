package marcasrealaccount.vulkan.util;

import org.lwjgl.vulkan.VkClearValue;

public class VulkanClearDepthStencil extends VulkanClearValue {
	public float depth;
	public int stencil;

	public VulkanClearDepthStencil(float depth, int stencil) {
		this.depth = depth;
		this.stencil = stencil;
	}

	@Override
	public void put(VkClearValue clearValue) {
		clearValue.depthStencil().set(this.depth, this.stencil);
	}
}
