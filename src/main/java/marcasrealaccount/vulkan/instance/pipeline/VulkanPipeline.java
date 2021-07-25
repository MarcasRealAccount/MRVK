package marcasrealaccount.vulkan.instance.pipeline;

import marcasrealaccount.vulkan.instance.VulkanHandle;

public abstract class VulkanPipeline extends VulkanHandle<Long> {
	public VulkanPipeline(Long nullHandle) {
		super(nullHandle);
	}

	public VulkanPipeline(Long nullHandle, Long handle) {
		super(nullHandle, handle);
	}

	public abstract int getBindPoint();
}
