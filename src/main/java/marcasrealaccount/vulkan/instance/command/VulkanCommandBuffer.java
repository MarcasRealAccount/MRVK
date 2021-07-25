package marcasrealaccount.vulkan.instance.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;
import marcasrealaccount.vulkan.instance.image.VulkanFramebuffer;
import marcasrealaccount.vulkan.instance.pipeline.VulkanPipeline;
import marcasrealaccount.vulkan.instance.pipeline.VulkanRenderPass;
import marcasrealaccount.vulkan.util.VulkanClearValue;

public class VulkanCommandBuffer extends VulkanHandle<VkCommandBuffer> {
	public final VulkanDevice device;
	public final VulkanCommandPool commandPool;

	public final int level;

	public VulkanCommandBuffer(VulkanDevice device, VulkanCommandPool commandPool, VkCommandBuffer handle, int level) {
		super(null, handle);
		this.device = device;
		this.commandPool = commandPool;
		this.level = level;
	}

	@Override
	protected void createAbstract() {
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
	}

	public boolean begin() {
		try (var stack = MemoryStack.stackPush()) {
			var beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);

			beginInfo.set(VK12.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO, 0, 0, null);

			return VK12.vkBeginCommandBuffer(this.handle, beginInfo) == VK12.VK_SUCCESS;
		}
	}

	public boolean end() {
		return VK12.vkEndCommandBuffer(this.handle) == VK12.VK_SUCCESS;
	}

	public void cmdBeginRenderPass(VulkanRenderPass renderPass, VulkanFramebuffer framebuffer, int x, int y, int width,
			int height, VulkanClearValue[] clearValues) {
		try (var stack = MemoryStack.stackPush()) {
			var beginInfo = VkRenderPassBeginInfo.mallocStack(stack);
			var renderArea = VkRect2D.mallocStack(stack);

			renderArea.offset().set(x, y);
			renderArea.extent().set(width, height);

			var pClearValues = VkClearValue.malloc(clearValues.length);
			for (int i = 0; i < clearValues.length; ++i)
				clearValues[i].put(pClearValues.get(i));

			beginInfo.set(VK12.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO, 0, renderPass.getHandle(),
					framebuffer.getHandle(), renderArea, pClearValues);

			VK12.vkCmdBeginRenderPass(this.handle, beginInfo, VK12.VK_SUBPASS_CONTENTS_INLINE);

			pClearValues.free();
		}
	}

	public void cmdEndRenderPass() {
		VK12.vkCmdEndRenderPass(this.handle);
	}

	public void cmdBindPipeline(VulkanPipeline pipeline) {
		VK12.vkCmdBindPipeline(this.handle, pipeline.getBindPoint(), pipeline.getHandle());
	}

	public void cmdDraw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
		VK12.vkCmdDraw(this.handle, vertexCount, instanceCount, firstVertex, firstInstance);
	}
}
