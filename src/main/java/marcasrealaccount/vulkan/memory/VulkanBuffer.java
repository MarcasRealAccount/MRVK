package marcasrealaccount.vulkan.memory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;

public class VulkanBuffer extends VulkanHandle<Long> {
	public final VulkanMemoryAllocator memoryAllocator;

	private long allocHandle = 0L;

	public long                   size        = 0;
	public int                    usage       = VK12.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
	public int                    memoryUsage = Vma.VMA_MEMORY_USAGE_GPU_ONLY;
	public final HashSet<Integer> indices     = new HashSet<>();

	public VulkanBuffer(VulkanMemoryAllocator memoryAllocator) {
		super(0L);
		this.memoryAllocator = memoryAllocator;

		this.memoryAllocator.addChild(this);
	}

	@Override
	protected void createAbstract() {
		int sharingMode = VK12.VK_SHARING_MODE_EXCLUSIVE;

		try (var stack = MemoryStack.stackPush()) {
			var       createInfo           = VkBufferCreateInfo.mallocStack(stack);
			var       allocationCreateInfo = VmaAllocationCreateInfo.mallocStack(stack);
			var       pBuffer              = stack.mallocLong(1);
			var       pAllocation          = stack.mallocPointer(1);
			IntBuffer pIndices             = null;

			if (this.indices.size() > 1) {
				sharingMode = VK12.VK_SHARING_MODE_CONCURRENT;
				pIndices    = MemoryUtil.memAllocInt(0);
				int i = 0;
				for (var index : this.indices) {
					pIndices.put(i, index);
					++i;
				}
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO, 0, 0, this.size, this.usage, sharingMode, pIndices);

			allocationCreateInfo.set(0, this.memoryUsage, 0, 0, 0, 0, 0, 0.0f);

			if (Vma.vmaCreateBuffer(this.memoryAllocator.getHandle(), createInfo, allocationCreateInfo, pBuffer, pAllocation,
					null) == VK12.VK_SUCCESS) {
				this.handle      = pBuffer.get(0);
				this.allocHandle = pAllocation.get(0);
			}

			if (pIndices != null) MemoryUtil.memFree(pIndices);
		}
	}

	@Override
	protected void destroyAbstract() {
		Vma.vmaDestroyBuffer(this.memoryAllocator.getHandle(), this.handle, this.allocHandle);
		this.allocHandle = 0L;
	}

	@Override
	protected void removeAbstract() {
		this.memoryAllocator.removeChild(this);
	}

	public long getAllocationHandle() {
		return this.allocHandle;
	}

	public ByteBuffer mapMemory() {
		try (var stack = MemoryStack.stackPush()) {
			var ppData = stack.mallocPointer(1);

			if (Vma.vmaMapMemory(this.memoryAllocator.getHandle(), this.allocHandle, ppData) == VK12.VK_SUCCESS)
				return MemoryUtil.memByteBuffer(ppData.get(0), (int) Math.min(this.size, Integer.MAX_VALUE));
			return ByteBuffer.wrap(null);
		}
	}

	public void unmapMemory() {
		Vma.vmaUnmapMemory(this.memoryAllocator.getHandle(), this.allocHandle);
	}
}
