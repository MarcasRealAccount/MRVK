package marcasrealaccount.vulkan.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.image.VulkanFramebuffer;
import marcasrealaccount.vulkan.pipeline.VulkanPipeline;
import marcasrealaccount.vulkan.pipeline.VulkanRenderPass;
import marcasrealaccount.vulkan.util.VulkanClearValue;
import marcasrealaccount.vulkan.util.VulkanScissor;
import marcasrealaccount.vulkan.util.VulkanViewport;

public class VulkanCommandBuffer extends VulkanHandle<VkCommandBuffer> {
	public final VulkanCommandPool commandPool;

	public final int level;

	public VulkanCommandBuffer(VulkanCommandPool commandPool, VkCommandBuffer handle, int level) {
		super(null, false);
		this.handle = handle;
		this.commandPool = commandPool;
		this.level = level;

		this.commandPool.addChild(this);
	}

	@Override
	protected void createAbstract() {
	}

	@Override
	protected void destroyAbstract() {
	}

	@Override
	protected void removeAbstract() {
		this.commandPool.removeChild(this);
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

	public void cmdSetViewports(int firstViewport, VulkanViewport[] viewports) {
		try (var stack = MemoryStack.stackPush()) {
			var pViewports = VkViewport.mallocStack(viewports.length, stack);

			for (int i = 0; i < viewports.length; ++i) {
				var viewport = viewports[i];
				var pViewport = pViewports.get(i);
				pViewport.set(viewport.x, viewport.y, viewport.width, viewport.height, viewport.minDepth,
						viewport.maxDepth);
			}

			VK12.vkCmdSetViewport(this.handle, firstViewport, pViewports);
		}
	}

	public void cmdSetScissors(int firstScissor, VulkanScissor[] scissors) {
		try (var stack = MemoryStack.stackPush()) {
			var pScissors = VkRect2D.mallocStack(scissors.length, stack);

			for (int i = 0; i < scissors.length; ++i) {
				var scissor = scissors[i];
				var pScissor = pScissors.get(i);
				pScissor.offset().set(scissor.x, scissor.y);
				pScissor.extent().set(scissor.width, scissor.height);
			}

			VK12.vkCmdSetScissor(this.handle, firstScissor, pScissors);
		}
	}

	public void cmdSetLineWidth(float lineWidth) {
		VK12.vkCmdSetLineWidth(this.handle, lineWidth);
	}
}
