package marcasrealaccount.vulkan.image;

import java.nio.IntBuffer;
import java.util.HashSet;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.memory.VulkanMemoryAllocator;
import marcasrealaccount.vulkan.util.VulkanExtent3D;

public class VulkanImage extends VulkanHandle<Long> {
	public final VulkanMemoryAllocator memoryAllocator;

	private long allocHandle = 0L;

	public EImageType             imageType     = EImageType.TYPE_2D;
	public int                    format        = VK12.VK_FORMAT_R8G8B8A8_SRGB;
	public final VulkanExtent3D   extent        = new VulkanExtent3D(1, 1, 1);
	public int                    mipLevels     = 1;
	public int                    arrayLayers   = 1;
	public int                    samples       = VK12.VK_SAMPLE_COUNT_1_BIT;
	public EImageTiling           tiling        = EImageTiling.OPTIMAL;
	public int                    usage         = VK12.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
	public int                    memoryUsage   = Vma.VMA_MEMORY_USAGE_GPU_ONLY;
	public final HashSet<Integer> indices       = new HashSet<>();
	public int                    initialLayout = VK12.VK_IMAGE_LAYOUT_UNDEFINED;

	public VulkanImage(VulkanMemoryAllocator memoryAllocator) {
		super(0L);
		this.memoryAllocator = memoryAllocator;

		this.memoryAllocator.addChild(this);
	}

	public VulkanImage(VulkanMemoryAllocator memoryAllocator, long handle) {
		super(0L, false);
		this.handle          = handle;
		this.memoryAllocator = memoryAllocator;

		this.memoryAllocator.addChild(this);
	}

	@Override
	protected void createAbstract() {
		int sharingMode = VK12.VK_SHARING_MODE_EXCLUSIVE;

		try (var stack = MemoryStack.stackPush()) {
			var       createInfo           = VkImageCreateInfo.mallocStack(stack);
			var       allocationCreateInfo = VmaAllocationCreateInfo.mallocStack(stack);
			var       pImage               = stack.mallocLong(1);
			var       pAllocation          = stack.mallocPointer(1);
			var       pExtent              = VkExtent3D.mallocStack(stack);
			IntBuffer pIndices             = null;

			pExtent.set(this.extent.width, this.extent.height, this.extent.depth);

			if (this.indices.size() > 1) {
				sharingMode = VK12.VK_SHARING_MODE_CONCURRENT;
				pIndices    = MemoryUtil.memAllocInt(0);
				int i = 0;
				for (var index : this.indices) {
					pIndices.put(i, index);
					++i;
				}
			}

			createInfo.set(VK12.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO, 0, 0, this.imageType.getValue(), this.format, pExtent, this.mipLevels,
					this.arrayLayers, this.samples, this.tiling.getValue(), this.usage, sharingMode, pIndices, this.initialLayout);

			allocationCreateInfo.set(0, this.memoryUsage, 0, 0, 0, 0, 0, 0.0f);

			if (Vma.vmaCreateImage(this.memoryAllocator.getHandle(), createInfo, allocationCreateInfo, pImage, pAllocation,
					null) == VK12.VK_SUCCESS) {
				this.handle      = pImage.get(0);
				this.allocHandle = pAllocation.get(0);
			}

			if (pIndices != null) MemoryUtil.memFree(pIndices);
		}
	}

	@Override
	protected void destroyAbstract() {
		Vma.vmaDestroyImage(this.memoryAllocator.getHandle(), this.handle, this.allocHandle);
		this.allocHandle = 0L;
	}

	@Override
	protected void removeAbstract() {
		this.memoryAllocator.removeChild(this);
	}

	public long getAllocHandle() {
		return this.allocHandle;
	}

	public enum EImageType {
		TYPE_1D(VK12.VK_IMAGE_TYPE_1D), TYPE_2D(VK12.VK_IMAGE_TYPE_2D), TYPE_3D(VK12.VK_IMAGE_TYPE_3D);

		private int value;

		private EImageType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	public enum EImageTiling {
		OPTIMAL(VK12.VK_IMAGE_TILING_OPTIMAL), LINEAR(VK12.VK_IMAGE_TILING_LINEAR);

		private int value;

		private EImageTiling(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
