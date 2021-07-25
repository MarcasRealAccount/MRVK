package marcasrealaccount.vulkan.instance.synchronize;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;

public class VulkanSemaphore extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public VulkanSemaphore(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkSemaphoreCreateInfo.mallocStack(stack);
			var pSemaphore = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO, 0, 0);

			if (VK12.vkCreateSemaphore(this.device.getHandle(), createInfo, null, pSemaphore) == VK12.VK_SUCCESS) {
				this.handle = pSemaphore.get(0);
				this.device.addInvalidate(this);
			}
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroySemaphore(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated)
			this.device.removeInvalidate(this);
	}
}
