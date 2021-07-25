package marcasrealaccount.vulkan.instance;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import marcasrealaccount.vulkan.instance.command.VulkanCommandBuffer;
import marcasrealaccount.vulkan.instance.synchronize.VulkanFence;
import marcasrealaccount.vulkan.instance.synchronize.VulkanSemaphore;

public class VulkanQueue extends VulkanHandle<VkQueue> {
	public final VulkanDevice device;

	public int queueFamilyIndex = 0;
	public int queueIndex = 0;

	public VulkanQueue(VulkanDevice device) {
		super(null);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var pQueue = stack.mallocPointer(1);
			VK12.vkGetDeviceQueue(this.device.getHandle(), this.queueFamilyIndex, this.queueIndex, pQueue);
			if (pQueue.get(0) != 0) {
				this.handle = new VkQueue(pQueue.get(0), this.device.getHandle());
				this.device.addInvalidate(this);
			}
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		if (!wasInvalidated)
			this.device.removeInvalidate(this);
	}

	public void waitIdle() {
		VK12.vkQueueWaitIdle(this.handle);
	}

	public boolean submitCommandBuffers(VulkanCommandBuffer[] commandBuffers, VulkanSemaphore[] waitSemaphores,
			VulkanSemaphore[] signalSemaphores, int[] waitDstStageMask, VulkanFence fence) {
		try (var stack = MemoryStack.stackPush()) {
			var submitInfo = VkSubmitInfo.mallocStack(stack);
			var pCommandBuffers = stack.mallocPointer(commandBuffers.length);
			var pWaitSemaphores = stack.mallocLong(waitSemaphores.length);
			var pSignalSemaphores = stack.mallocLong(signalSemaphores.length);
			var pWaitDstStageMask = stack.mallocInt(waitDstStageMask.length);

			for (int i = 0; i < commandBuffers.length; ++i)
				pCommandBuffers.put(i, commandBuffers[i].getHandle());
			for (int i = 0; i < waitSemaphores.length; ++i)
				pWaitSemaphores.put(i, waitSemaphores[i].getHandle());
			for (int i = 0; i < signalSemaphores.length; ++i)
				pSignalSemaphores.put(i, signalSemaphores[i].getHandle());
			for (int i = 0; i < waitDstStageMask.length; ++i)
				pWaitDstStageMask.put(i, waitDstStageMask[i]);

			submitInfo.set(VK12.VK_STRUCTURE_TYPE_SUBMIT_INFO, 0, pWaitSemaphores.capacity(), pWaitSemaphores,
					pWaitDstStageMask, pCommandBuffers, pSignalSemaphores);

			return VK12.vkQueueSubmit(this.handle, submitInfo,
					fence != null ? fence.getHandle() : 0) == VK12.VK_SUCCESS;
		}
	}

	public boolean present(VulkanSwapchain[] swapchains, int[] imageIndices, VulkanSemaphore[] waitSemaphores) {
		try (var stack = MemoryStack.stackPush()) {
			var presentInfo = VkPresentInfoKHR.mallocStack(stack);
			var pSwapchains = stack.mallocLong(swapchains.length);
			var pImageIndices = stack.mallocInt(imageIndices.length);
			var pWaitSemaphores = stack.mallocLong(waitSemaphores.length);
			var pResults = stack.mallocInt(swapchains.length);

			for (int i = 0; i < swapchains.length; ++i)
				pSwapchains.put(i, swapchains[i].getHandle());
			for (int i = 0; i < imageIndices.length; ++i)
				pImageIndices.put(i, imageIndices[i]);
			for (int i = 0; i < waitSemaphores.length; ++i)
				pWaitSemaphores.put(i, waitSemaphores[i].getHandle());

			presentInfo.set(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR, 0, pWaitSemaphores, pSwapchains.capacity(),
					pSwapchains, pImageIndices, pResults);

			KHRSwapchain.vkQueuePresentKHR(this.device.getPresentQueue().getHandle(), presentInfo);

			for (int i = 0; i < swapchains.length; ++i)
				if (pResults.get(i) != VK12.VK_SUCCESS)
					return false;
			return true;
		}
	}
}
