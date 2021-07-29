package marcasrealaccount.vulkan.pipeline;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanRenderPass extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public final ArrayList<Attachment> attachments  = new ArrayList<>();
	public final ArrayList<Subpass>    subpasses    = new ArrayList<>();
	public final ArrayList<Dependency> dependencies = new ArrayList<>();

	public VulkanRenderPass(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo  = VkRenderPassCreateInfo.mallocStack(stack);
			var pRenderPass = stack.mallocLong(1);

			var pAttachments = VkAttachmentDescription.malloc(this.attachments.size());
			for (int i = 0; i < this.attachments.size(); ++i) {
				var attachment  = this.attachments.get(i);
				var pAttachment = pAttachments.get(i);
				pAttachment.set(0, attachment.format, attachment.samples, attachment.loadOp, attachment.storeOp, attachment.stencilLoadOp,
						attachment.stencilStoreOp, attachment.initialLayout, attachment.finalLayout);
			}

			var pSubpasses = VkSubpassDescription.malloc(this.subpasses.size());
			for (int i = 0; i < this.subpasses.size(); ++i) {
				var subpass  = this.subpasses.get(i);
				var pSubpass = pSubpasses.get(i);

				var pInputAttachments = VkAttachmentReference.malloc(subpass.attachmentRefs.size());
				for (int j = 0; j < subpass.attachmentRefs.size(); ++j) {
					var inputAttachment  = subpass.attachmentRefs.get(j);
					var pInputAttachment = pInputAttachments.get(j);
					pInputAttachment.set(inputAttachment.attachment, inputAttachment.layout);
				}

				var pColorAttachments = VkAttachmentReference.malloc(subpass.colorAttachmentRefs.size());
				for (int j = 0; j < subpass.colorAttachmentRefs.size(); ++j) {
					var colorAttachment  = subpass.colorAttachmentRefs.get(j);
					var pColorAttachment = pColorAttachments.get(j);
					pColorAttachment.set(colorAttachment.attachment, colorAttachment.layout);
				}

				VkAttachmentReference.Buffer pResolveAttachments = null;
				if (!subpass.resolveAttachmentRefs.isEmpty()) {
					pResolveAttachments = VkAttachmentReference.malloc(subpass.resolveAttachmentRefs.size());
					for (int j = 0; j < subpass.resolveAttachmentRefs.size(); ++j) {
						var resolveAttachment  = subpass.resolveAttachmentRefs.get(j);
						var pResolveAttachment = pResolveAttachments.get(j);
						pResolveAttachment.set(resolveAttachment.attachment, resolveAttachment.layout);
					}
				}

				VkAttachmentReference pDepthStencilAttachment = null;
				if (subpass.useDepthStencilAttachment) {
					pDepthStencilAttachment = VkAttachmentReference.malloc();
					pDepthStencilAttachment.set(subpass.depthStencilAttachment.attachment, subpass.depthStencilAttachment.layout);
				}

				var pPreserveAttachments = MemoryUtil.memAllocInt(subpass.preserveAttachments.size());
				for (int j = 0; j < subpass.preserveAttachments.size(); ++j) pPreserveAttachments.put(j, subpass.preserveAttachments.get(j));

				pSubpass.set(0, subpass.pipelineBindPoint, pInputAttachments, subpass.colorAttachmentRefs.size(), pColorAttachments,
						pResolveAttachments, pDepthStencilAttachment, pPreserveAttachments);
			}

			var pDependencies = VkSubpassDependency.malloc(this.dependencies.size());
			for (int i = 0; i < this.dependencies.size(); ++i) {
				var dependency  = this.dependencies.get(i);
				var pDependency = pDependencies.get(i);
				pDependency.set(dependency.srcSubpass, dependency.dstSubpass, dependency.srcStageMask, dependency.dstStageMask,
						dependency.srcAccessMask, dependency.dstAccessMask, dependency.dependencyFlags);
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO, 0, 0, pAttachments, pSubpasses, pDependencies);

			if (VK12.vkCreateRenderPass(this.device.getHandle(), createInfo, null, pRenderPass) == VK12.VK_SUCCESS) this.handle = pRenderPass.get(0);

			pAttachments.free();
			for (var subpass : pSubpasses) {
				subpass.pInputAttachments().free();
				subpass.pColorAttachments().free();
				if (subpass.pResolveAttachments() != null) subpass.pResolveAttachments().free();
				if (subpass.pDepthStencilAttachment() != null) subpass.pDepthStencilAttachment().free();
				MemoryUtil.memFree(subpass.pPreserveAttachments());
			}
			pSubpasses.free();
			pDependencies.free();
		}
	}

	@Override
	protected boolean destroyAbstract() {
		VK12.vkDestroyRenderPass(this.device.getHandle(), this.handle, null);
		return true;
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public static class Attachment {
		public int format         = VK12.VK_FORMAT_B8G8R8A8_SRGB;
		public int samples        = VK12.VK_SAMPLE_COUNT_1_BIT;
		public int loadOp         = VK12.VK_ATTACHMENT_LOAD_OP_CLEAR;
		public int storeOp        = VK12.VK_ATTACHMENT_STORE_OP_STORE;
		public int stencilLoadOp  = VK12.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
		public int stencilStoreOp = VK12.VK_ATTACHMENT_STORE_OP_DONT_CARE;
		public int initialLayout  = VK12.VK_IMAGE_LAYOUT_UNDEFINED;
		public int finalLayout    = KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
	}

	public static class Subpass {
		public int                            pipelineBindPoint         = VK12.VK_PIPELINE_BIND_POINT_GRAPHICS;
		public final ArrayList<AttachmentRef> attachmentRefs            = new ArrayList<>();
		public final ArrayList<AttachmentRef> colorAttachmentRefs       = new ArrayList<>();
		public final ArrayList<AttachmentRef> resolveAttachmentRefs     = new ArrayList<>();
		public final boolean                  useDepthStencilAttachment = false;
		public final AttachmentRef            depthStencilAttachment    = new AttachmentRef();
		public final ArrayList<Integer>       preserveAttachments       = new ArrayList<>();

		public static class AttachmentRef {
			public int attachment = 0;
			public int layout     = VK12.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
		}
	}

	public static class Dependency {
		public int srcSubpass      = 0;
		public int dstSubpass      = 0;
		public int srcStageMask    = 0;
		public int dstStageMask    = 0;
		public int srcAccessMask   = 0;
		public int dstAccessMask   = 0;
		public int dependencyFlags = 0;
	}
}
