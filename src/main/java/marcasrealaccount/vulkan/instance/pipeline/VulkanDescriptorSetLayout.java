package marcasrealaccount.vulkan.instance.pipeline;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;

public class VulkanDescriptorSetLayout extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public final ArrayList<Binding> bindings = new ArrayList<>();

	public VulkanDescriptorSetLayout(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkDescriptorSetLayoutCreateInfo.mallocStack(stack);
			var pSetLayout = stack.mallocLong(1);

			var pBindings = VkDescriptorSetLayoutBinding.malloc(this.bindings.size());
			for (int i = 0; i < this.bindings.size(); ++i) {
				var binding = this.bindings.get(i);
				var pBinding = pBindings.get(i);
				pBinding.set(binding.binding, binding.descriptorType, binding.descriptorCount, binding.stageFlags,
						null); // TODO: Implement immutable image samplers
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO, 0, 0, pBindings);

			if (VK12.vkCreateDescriptorSetLayout(this.device.getHandle(), createInfo, null,
					pSetLayout) == VK12.VK_SUCCESS) {
				this.handle = pSetLayout.get(0);
				this.device.addInvalidate(this);
			}

			pBindings.free();
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyDescriptorSetLayout(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated)
			this.device.removeInvalidate(this);
	}

	public static class Binding {
		public int binding;
		public int descriptorType;
		public int descriptorCount;
		public int stageFlags;
	}
}
