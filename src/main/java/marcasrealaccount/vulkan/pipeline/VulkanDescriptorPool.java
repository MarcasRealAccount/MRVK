package marcasrealaccount.vulkan.pipeline;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanDescriptorPool extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public int                       maxSets   = 4;
	public final ArrayList<PoolSize> poolSizes = new ArrayList<>();

	private final ArrayList<VulkanDescriptorSet> descriptorSets = new ArrayList<>();

	public VulkanDescriptorPool(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo      = VkDescriptorPoolCreateInfo.mallocStack(stack);
			var pDescriptorPool = stack.mallocLong(1);
			var pPoolSizes      = VkDescriptorPoolSize.malloc(this.poolSizes.size());

			for (int i = 0; i < this.poolSizes.size(); ++i) {
				var poolSize  = this.poolSizes.get(i);
				var pPoolSize = pPoolSizes.get(i);
				pPoolSize.set(poolSize.type, poolSize.descriptorCount);
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO, 0, 0, this.maxSets, pPoolSizes);

			if (VK12.vkCreateDescriptorPool(this.device.getHandle(), createInfo, null, pDescriptorPool) == VK12.VK_SUCCESS)
				this.handle = pDescriptorPool.get(0);

			pPoolSizes.free();
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyDescriptorPool(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public ArrayList<VulkanDescriptorSet> allocateSets(VulkanDescriptorSetLayout[] layouts) {
		var sets = new ArrayList<VulkanDescriptorSet>();

		try (var stack = MemoryStack.stackPush()) {
			var allocInfo       = VkDescriptorSetAllocateInfo.mallocStack(stack);
			var pSetLayouts     = MemoryUtil.memAllocLong(layouts != null ? layouts.length : 0);
			var pDescriptorSets = MemoryUtil.memAllocLong(pSetLayouts.capacity());

			for (int i = 0; i < pSetLayouts.capacity(); ++i) pSetLayouts.put(i, layouts[i].getHandle());

			allocInfo.set(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO, 0, this.handle, pSetLayouts);

			if (VK12.vkAllocateDescriptorSets(this.device.getHandle(), allocInfo, pDescriptorSets) == VK12.VK_SUCCESS) {
				for (int i = 0; i < pDescriptorSets.capacity(); ++i) {
					var set = new VulkanDescriptorSet(this, pDescriptorSets.get(i));
					sets.add(set);
					this.descriptorSets.add(set);
				}
			}

			MemoryUtil.memFree(pSetLayouts);
			MemoryUtil.memFree(pDescriptorSets);
		}

		return sets;
	}

	public int getNumSets() {
		return this.descriptorSets.size();
	}

	public VulkanDescriptorSet getSet(int index) {
		return index >= 0 && index < this.descriptorSets.size() ? this.descriptorSets.get(index) : null;
	}

	public static class PoolSize {
		public int type, descriptorCount;

		public PoolSize(int type, int descriptorCount) {
			this.type            = type;
			this.descriptorCount = descriptorCount;
		}
	}
}
