package marcasrealaccount.vulkan.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanFence extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public boolean signaled = false;

	public VulkanFence(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkFenceCreateInfo.mallocStack(stack);
			var pFence = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO, 0,
					this.signaled ? VK12.VK_FENCE_CREATE_SIGNALED_BIT : 0);

			if (VK12.vkCreateFence(this.device.getHandle(), createInfo, null, pFence) == VK12.VK_SUCCESS)
				this.handle = pFence.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyFence(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public void reset() {
		VK12.vkResetFences(this.device.getHandle(), this.handle);
	}

	public void waitFor(boolean waitAll, long timeout) {
		VK12.vkWaitForFences(this.device.getHandle(), this.handle, waitAll, timeout);
	}

	public boolean getState() {
		return VK12.vkGetFenceStatus(this.device.getHandle(), this.handle) == VK12.VK_SUCCESS;
	}
}
