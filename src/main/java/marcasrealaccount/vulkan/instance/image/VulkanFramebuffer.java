package marcasrealaccount.vulkan.instance.image;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;
import marcasrealaccount.vulkan.instance.pipeline.VulkanRenderPass;

public class VulkanFramebuffer extends VulkanHandle<Long> {
	public final VulkanDevice device;
	public final VulkanRenderPass renderPass;

	private final ArrayList<VulkanImageView> usedAttachments = new ArrayList<>();

	public final ArrayList<VulkanImageView> attachments = new ArrayList<>();

	public int width = 1;
	public int height = 1;
	public int layers = 1;

	public VulkanFramebuffer(VulkanDevice device, VulkanRenderPass renderPass) {
		super(0L);
		this.device = device;
		this.renderPass = renderPass;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkFramebufferCreateInfo.mallocStack(stack);
			var pFramebuffer = stack.mallocLong(1);

			var pAttachments = MemoryUtil.memAllocLong(this.attachments.size());
			for (int i = 0; i < this.attachments.size(); ++i)
				pAttachments.put(i, this.attachments.get(i).getHandle());

			createInfo.set(VK12.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO, 0, 0, this.renderPass.getHandle(),
					pAttachments, this.width, this.height, this.layers);

			if (VK12.vkCreateFramebuffer(this.device.getHandle(), createInfo, null, pFramebuffer) == VK12.VK_SUCCESS) {
				this.handle = pFramebuffer.get(0);
				this.device.addInvalidate(this);
				this.renderPass.addInvalidate(this);
				for (var attachment : this.attachments) {
					attachment.addInvalidate(this);
					this.usedAttachments.add(attachment);
				}
			}

			MemoryUtil.memFree(pAttachments);
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyFramebuffer(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated) {
			this.device.removeInvalidate(this);
			this.renderPass.removeInvalidate(this);
			for (var attachment : this.usedAttachments)
				attachment.removeInvalidate(this);
		}
		this.usedAttachments.clear();
	}
}
