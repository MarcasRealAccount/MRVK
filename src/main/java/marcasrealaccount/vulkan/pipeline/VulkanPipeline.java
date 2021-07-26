package marcasrealaccount.vulkan.pipeline;

import marcasrealaccount.vulkan.VulkanHandle;

public abstract class VulkanPipeline extends VulkanHandle<Long> {
	public VulkanPipeline(Long nullHandle) {
		super(nullHandle);
	}

	public VulkanPipeline(Long nullHandle, boolean destroyable) {
		super(nullHandle, destroyable);
	}

	public abstract int getBindPoint();
}
