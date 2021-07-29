package marcasrealaccount.vulkan.memory;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VK12;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanMemoryAllocator extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public VulkanMemoryAllocator(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VmaAllocatorCreateInfo.mallocStack(stack);
			var pVma       = stack.mallocPointer(1);
			var pFunctions = VmaVulkanFunctions.mallocStack(stack);
			pFunctions.set(this.device.physicalDevice.instance.getHandle(), this.device.getHandle());

			createInfo.set(0, this.device.physicalDevice.getHandle(), this.device.getHandle(), 0, null, null, 0, null, pFunctions, null,
					this.device.physicalDevice.instance.getHandle(), VK12.VK_API_VERSION_1_2);

			if (Vma.vmaCreateAllocator(createInfo, pVma) == VK12.VK_SUCCESS) this.handle = pVma.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		Vma.vmaDestroyAllocator(this.handle);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}
}
