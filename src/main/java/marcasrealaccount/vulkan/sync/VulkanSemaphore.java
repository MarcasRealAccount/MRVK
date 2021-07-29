package marcasrealaccount.vulkan.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanSemaphore extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public VulkanSemaphore(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkSemaphoreCreateInfo.mallocStack(stack);
			var pSemaphore = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO, 0, 0);

			if (VK12.vkCreateSemaphore(this.device.getHandle(), createInfo, null, pSemaphore) == VK12.VK_SUCCESS) this.handle = pSemaphore.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroySemaphore(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public void waitFor(long timeout) {
		waitForSemaphores(new VulkanSemaphore[] { this }, timeout);
	}

	public long getValue() {
		try (var stack = MemoryStack.stackPush()) {
			var pValue = stack.mallocLong(1);
			VK12.vkGetSemaphoreCounterValue(this.device.getHandle(), this.handle, pValue);
			return pValue.get(0);
		}
	}

	public static void waitForSemaphores(VulkanSemaphore[] semaphores, long timeout) {
		if (semaphores.length == 0) return;

		VulkanDevice device = semaphores[0].device;
		for (int i = 0; i < semaphores.length; ++i) if (semaphores[i].device != device) return;

		try (var stack = MemoryStack.stackPush()) {
			var waitInfo    = VkSemaphoreWaitInfo.mallocStack(stack);
			var pSemaphores = MemoryUtil.memAllocLong(semaphores.length);
			var pValues     = MemoryUtil.memAllocLong(semaphores.length);

			for (int i = 0; i < semaphores.length; ++i) pSemaphores.put(i, semaphores[i].getHandle());

			waitInfo.set(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO, 0, 0, pSemaphores.capacity(), pSemaphores, pValues);

			VK12.vkWaitSemaphores(device.getHandle(), waitInfo, timeout);

			MemoryUtil.memFree(pSemaphores);
			MemoryUtil.memFree(pValues);
		}
	}
}
