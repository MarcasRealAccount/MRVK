package marcasrealaccount.vulkan.instance.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanHandle;

public class VulkanShaderModule extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public byte[] code = null;

	public VulkanShaderModule(VulkanDevice device) {
		super(0L);
		this.device = device;
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo = VkShaderModuleCreateInfo.mallocStack(stack);
			var pShaderModule = stack.mallocLong(1);

			var pCode = MemoryUtil.memAlloc(code.length);
			pCode.put(0, code);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO, 0, 0, pCode);

			if (VK12.vkCreateShaderModule(this.device.getHandle(), createInfo, null,
					pShaderModule) == VK12.VK_SUCCESS) {
				this.handle = pShaderModule.get(0);
				this.device.addInvalidate(this);
			}

			MemoryUtil.memFree(pCode);
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyShaderModule(this.device.getHandle(), this.handle, null);
		if (!wasInvalidated)
			this.device.removeInvalidate(this);
	}
}
