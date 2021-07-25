package marcasrealaccount.vulkan.instance.synchronize;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;

public class VulkanFence extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public boolean signaled = false;

	public VulkanFence(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkFenceCreateInfo.mallocStack(stack);
			var pFence = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO, 0,
					this.signaled ? VK12.VK_FENCE_CREATE_SIGNALED_BIT : 0);

			if (VK12.vkCreateFence(this.device.getHandle(), createInfo, null, pFence) == VK12.VK_SUCCESS) {
				this.handle = pFence.get(0);
				this.device.addInvalidate(this);
			}
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyFence(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated)
			this.device.removeInvalidate(this);
	}

	public void reset() {
		VK12.vkResetFences(this.device.getHandle(), this.handle);
	}
}
