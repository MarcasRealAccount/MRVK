package marcasrealaccount.vulkan.memory;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanMemory extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public long size;
	public int  type;
	public int  properties;

	public VulkanMemory(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var allocInfo = VkMemoryAllocateInfo.mallocStack(stack);
			var pMemory   = stack.mallocLong(1);

			allocInfo.set(VK12.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO, 0, this.size,
					this.device.physicalDevice.memoryProperties.getMemoryTypeIndex(this.type, this.properties));

			if (VK12.vkAllocateMemory(this.device.getHandle(), allocInfo, null, pMemory) == VK12.VK_SUCCESS) this.handle = pMemory.get(0);
		}
	}

	@Override
	protected boolean destroyAbstract() {
		VK12.vkFreeMemory(this.device.getHandle(), this.handle, null);
		return true;
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public ByteBuffer mapMemory(int offset, long size, int flags) {
		try (var stack = MemoryStack.stackPush()) {
			var ppData = stack.mallocPointer(1);

			if (VK12.vkMapMemory(this.device.getHandle(), this.handle, offset, size, flags, ppData) == VK12.VK_SUCCESS)
				return MemoryUtil.memByteBuffer(ppData.get(0), (int) Math.min(size, Integer.MAX_VALUE));
			return ByteBuffer.wrap(null);
		}
	}

	public void unmapMemory() {
		VK12.vkUnmapMemory(this.device.getHandle(), this.handle);
	}
}
