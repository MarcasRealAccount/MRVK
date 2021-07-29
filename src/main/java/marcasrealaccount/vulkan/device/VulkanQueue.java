package marcasrealaccount.vulkan.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.command.VulkanCommandBuffer;
import marcasrealaccount.vulkan.surface.VulkanSwapchain;
import marcasrealaccount.vulkan.sync.VulkanFence;
import marcasrealaccount.vulkan.sync.VulkanSemaphore;

public class VulkanQueue extends VulkanHandle<VkQueue> {
	public final VulkanDevice device;

	public int queueFamilyIndex = 0;
	public int queueIndex       = 0;

	public VulkanQueue(VulkanDevice device) {
		super(null);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var pQueue = stack.mallocPointer(1);
			VK12.vkGetDeviceQueue(this.device.getHandle(), this.queueFamilyIndex, this.queueIndex, pQueue);
			if (pQueue.get(0) != 0) this.handle = new VkQueue(pQueue.get(0), this.device.getHandle());
		}
	}

	@Override
	protected void destroyAbstract() {}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public void waitIdle() {
		VK12.vkQueueWaitIdle(this.handle);
	}

	public boolean submitCommandBuffers(VulkanCommandBuffer[] commandBuffers, VulkanSemaphore[] waitSemaphores, VulkanSemaphore[] signalSemaphores,
			int[] waitDstStageMask, VulkanFence fence) {
		try (var stack = MemoryStack.stackPush()) {
			var submitInfo        = VkSubmitInfo.mallocStack(stack);
			var pCommandBuffers   = MemoryUtil.memAllocPointer(commandBuffers != null ? commandBuffers.length : 0);
			var pWaitSemaphores   = MemoryUtil.memAllocLong(waitSemaphores != null ? waitSemaphores.length : 0);
			var pSignalSemaphores = MemoryUtil.memAllocLong(signalSemaphores != null ? signalSemaphores.length : 0);
			var pWaitDstStageMask = MemoryUtil.memAllocInt(waitDstStageMask != null ? waitDstStageMask.length : 0);

			for (int i = 0; i < pCommandBuffers.capacity(); ++i) pCommandBuffers.put(i, commandBuffers[i].getHandle());
			for (int i = 0; i < pWaitSemaphores.capacity(); ++i) pWaitSemaphores.put(i, waitSemaphores[i].getHandle());
			for (int i = 0; i < pSignalSemaphores.capacity(); ++i) pSignalSemaphores.put(i, signalSemaphores[i].getHandle());
			for (int i = 0; i < pWaitDstStageMask.capacity(); ++i) pWaitDstStageMask.put(i, waitDstStageMask[i]);

			submitInfo.set(VK12.VK_STRUCTURE_TYPE_SUBMIT_INFO, 0, pWaitSemaphores.capacity(), pWaitSemaphores, pWaitDstStageMask, pCommandBuffers,
					pSignalSemaphores);

			int result = VK12.vkQueueSubmit(this.handle, submitInfo, fence != null ? fence.getHandle() : 0);

			MemoryUtil.memFree(pCommandBuffers);
			MemoryUtil.memFree(pWaitSemaphores);
			MemoryUtil.memFree(pSignalSemaphores);
			MemoryUtil.memFree(pWaitDstStageMask);

			return result == VK12.VK_SUCCESS;
		}
	}

	public int[] present(VulkanSwapchain[] swapchains, int[] imageIndices, VulkanSemaphore[] waitSemaphores) {
		try (var stack = MemoryStack.stackPush()) {
			var presentInfo     = VkPresentInfoKHR.mallocStack(stack);
			var pSwapchains     = MemoryUtil.memAllocLong(swapchains != null ? swapchains.length : 0);
			var pImageIndices   = MemoryUtil.memAllocInt(imageIndices != null ? imageIndices.length : 0);
			var pWaitSemaphores = MemoryUtil.memAllocLong(waitSemaphores != null ? waitSemaphores.length : 0);
			var pResults        = MemoryUtil.memAllocInt(swapchains != null ? swapchains.length : 0);

			for (int i = 0; i < pSwapchains.capacity(); ++i) pSwapchains.put(i, swapchains[i].getHandle());
			for (int i = 0; i < pImageIndices.capacity(); ++i) pImageIndices.put(i, imageIndices[i]);
			for (int i = 0; i < pWaitSemaphores.capacity(); ++i) pWaitSemaphores.put(i, waitSemaphores[i].getHandle());

			presentInfo.set(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR, 0, pWaitSemaphores, pSwapchains.capacity(), pSwapchains, pImageIndices,
					pResults);

			KHRSwapchain.vkQueuePresentKHR(this.handle, presentInfo);

			int[] results = new int[swapchains.length];
			pResults.get(0, results);

			MemoryUtil.memFree(pSwapchains);
			MemoryUtil.memFree(pImageIndices);
			MemoryUtil.memFree(pWaitSemaphores);
			MemoryUtil.memFree(pResults);

			return results;
		}
	}
}
