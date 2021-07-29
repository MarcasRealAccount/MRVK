package marcasrealaccount.vulkan.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.command.util.VulkanBufferCopy;
import marcasrealaccount.vulkan.image.VulkanFramebuffer;
import marcasrealaccount.vulkan.memory.VulkanBuffer;
import marcasrealaccount.vulkan.pipeline.VulkanDescriptorSet;
import marcasrealaccount.vulkan.pipeline.VulkanPipeline;
import marcasrealaccount.vulkan.pipeline.VulkanPipelineLayout;
import marcasrealaccount.vulkan.pipeline.VulkanRenderPass;
import marcasrealaccount.vulkan.util.VulkanClearValue;
import marcasrealaccount.vulkan.util.VulkanScissor;
import marcasrealaccount.vulkan.util.VulkanViewport;

public class VulkanCommandBuffer extends VulkanHandle<VkCommandBuffer> {
	public final VulkanCommandPool commandPool;

	public final int level;

	public VulkanCommandBuffer(VulkanCommandPool commandPool, VkCommandBuffer handle, int level) {
		super(null, false);
		this.handle      = handle;
		this.commandPool = commandPool;
		this.level       = level;

		this.commandPool.addChild(this);
	}

	@Override
	protected void createAbstract() {}

	@Override
	protected void destroyAbstract() {}

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

	public void cmdBeginRenderPass(VulkanRenderPass renderPass, VulkanFramebuffer framebuffer, int x, int y, int width, int height,
			VulkanClearValue[] clearValues) {
		try (var stack = MemoryStack.stackPush()) {
			var beginInfo  = VkRenderPassBeginInfo.mallocStack(stack);
			var renderArea = VkRect2D.mallocStack(stack);

			renderArea.offset().set(x, y);
			renderArea.extent().set(width, height);

			var pClearValues = VkClearValue.malloc(clearValues.length);
			for (int i = 0; i < clearValues.length; ++i) clearValues[i].put(pClearValues.get(i));

			beginInfo.set(VK12.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO, 0, renderPass.getHandle(), framebuffer.getHandle(), renderArea,
					pClearValues);

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

	public void cmdBindVertexBuffers(int firstBinding, VulkanBuffer[] buffers, long[] offsets) {
		var pBuffers = MemoryUtil.memAllocLong(buffers.length);
		var pOffsets = MemoryUtil.memAllocLong(offsets.length);

		for (int i = 0; i < buffers.length; ++i) pBuffers.put(i, buffers[i].getHandle());
		for (int i = 0; i < offsets.length; ++i) pOffsets.put(i, offsets[i]);

		VK12.vkCmdBindVertexBuffers(this.handle, firstBinding, pBuffers, pOffsets);

		MemoryUtil.memFree(pBuffers);
		MemoryUtil.memFree(pOffsets);
	}

	public void cmdBindIndexBuffer(VulkanBuffer buffer, int offset, int indexType) {
		VK12.vkCmdBindIndexBuffer(this.handle, buffer.getHandle(), offset, indexType);
	}

	public void cmdBindDescriptorSets(int pipelineBindPoint, VulkanPipelineLayout pipelineLayout, int firstSet, VulkanDescriptorSet[] descriptorSets,
			int[] dynamicOffsets) {
		var pDescriptorSets = MemoryUtil.memAllocLong(descriptorSets != null ? descriptorSets.length : 0);
		var pDynamicOffsets = MemoryUtil.memAllocInt(dynamicOffsets != null ? dynamicOffsets.length : 0);

		for (int i = 0; i < pDescriptorSets.capacity(); ++i) pDescriptorSets.put(i, descriptorSets[i].getHandle());
		for (int i = 0; i < pDynamicOffsets.capacity(); ++i) pDynamicOffsets.put(i, dynamicOffsets[i]);

		VK12.vkCmdBindDescriptorSets(this.handle, pipelineBindPoint, pipelineLayout.getHandle(), firstSet, pDescriptorSets, pDynamicOffsets);

		MemoryUtil.memFree(pDescriptorSets);
		MemoryUtil.memFree(pDynamicOffsets);
	}

	public void cmdCopyBuffer(VulkanBuffer srcBuffer, VulkanBuffer dstBuffer, VulkanBufferCopy[] copyRegions) {
		var pCopyRegions = VkBufferCopy.malloc(copyRegions.length);

		for (int i = 0; i < pCopyRegions.capacity(); ++i) {
			var copyRegion  = copyRegions[i];
			var pCopyRegion = pCopyRegions.get(i);
			pCopyRegion.set(copyRegion.srcOffset, copyRegion.dstOffset, copyRegion.size);
		}

		VK12.vkCmdCopyBuffer(this.handle, srcBuffer.getHandle(), dstBuffer.getHandle(), pCopyRegions);

		pCopyRegions.free();
	}

	public void cmdDraw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
		VK12.vkCmdDraw(this.handle, vertexCount, instanceCount, firstVertex, firstInstance);
	}

	public void cmdDrawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
		VK12.vkCmdDrawIndexed(this.handle, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
	}

	public void cmdSetViewports(int firstViewport, VulkanViewport[] viewports) {
		try (var stack = MemoryStack.stackPush()) {
			var pViewports = VkViewport.mallocStack(viewports.length, stack);

			for (int i = 0; i < viewports.length; ++i) {
				var viewport  = viewports[i];
				var pViewport = pViewports.get(i);
				pViewport.set(viewport.x, viewport.y, viewport.width, viewport.height, viewport.minDepth, viewport.maxDepth);
			}

			VK12.vkCmdSetViewport(this.handle, firstViewport, pViewports);
		}
	}

	public void cmdSetScissors(int firstScissor, VulkanScissor[] scissors) {
		try (var stack = MemoryStack.stackPush()) {
			var pScissors = VkRect2D.mallocStack(scissors.length, stack);

			for (int i = 0; i < scissors.length; ++i) {
				var scissor  = scissors[i];
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
