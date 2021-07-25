package marcasrealaccount.vulkan.instance.pipeline;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;

public class VulkanPipelineLayout extends VulkanHandle<Long> {
	public final VulkanDevice device;

	private final ArrayList<VulkanDescriptorSetLayout> usedSetLayouts = new ArrayList<>();

	public final ArrayList<VulkanDescriptorSetLayout> setLayouts = new ArrayList<>();
	public final ArrayList<PushConstantRange> pushConstantRanges = new ArrayList<>();

	public VulkanPipelineLayout(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkPipelineLayoutCreateInfo.mallocStack(stack);
			var pPipelineLayout = stack.mallocLong(1);

			var pSetLayouts = MemoryUtil.memAllocLong(this.setLayouts.size());
			for (int i = 0; i < this.setLayouts.size(); ++i)
				pSetLayouts.put(i, this.setLayouts.get(i).getHandle());

			var pPushConstantRanges = VkPushConstantRange.malloc(this.pushConstantRanges.size());
			for (int i = 0; i < this.pushConstantRanges.size(); ++i) {
				var pushConstantRange = this.pushConstantRanges.get(i);
				var pPushConstantRange = pPushConstantRanges.get(i);
				pPushConstantRange.set(pushConstantRange.stageFlags, pushConstantRange.offset, pushConstantRange.size);
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO, 0, 0, pSetLayouts, pPushConstantRanges);

			if (VK12.vkCreatePipelineLayout(this.device.getHandle(), createInfo, null,
					pPipelineLayout) == VK12.VK_SUCCESS) {
				this.handle = pPipelineLayout.get(0);
				this.device.addInvalidate(this);
				for (var setLayout : this.setLayouts) {
					setLayout.addInvalidate(this);
					this.usedSetLayouts.add(setLayout);
				}
			}

			MemoryUtil.memFree(pSetLayouts);
			pPushConstantRanges.free();
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyPipelineLayout(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated) {
			this.device.removeInvalidate(this);
			for (var setLayout : this.usedSetLayouts)
				setLayout.removeInvalidate(this);
		}
		this.usedSetLayouts.clear();
	}

	public static class PushConstantRange {
		public int stageFlags;
		public int offset;
		public int size;

		public PushConstantRange(int stageFlags, int offset, int size) {
			this.stageFlags = stageFlags;
			this.offset = offset;
			this.size = size;
		}
	}
}
