package marcasrealaccount.vulkan.device.util;

import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import marcasrealaccount.vulkan.device.VulkanPhysicalDevice;

public class VulkanPhysicalDeviceMemoryProperties {
	public final ArrayList<MemoryType> types = new ArrayList<>();
	public final ArrayList<MemoryHeap> heaps = new ArrayList<>();

	public VulkanPhysicalDeviceMemoryProperties() {}

	public void getProperties(VulkanPhysicalDevice physicalDevice) {
		try (var stack = MemoryStack.stackPush()) {
			var pMemoryProperties = VkPhysicalDeviceMemoryProperties.mallocStack(stack);
			VK12.vkGetPhysicalDeviceMemoryProperties(physicalDevice.getHandle(), pMemoryProperties);

			for (int i = 0; i < pMemoryProperties.memoryTypeCount(); ++i) {
				var pMemoryType = pMemoryProperties.memoryTypes(i);
				this.types.add(new MemoryType(pMemoryType.propertyFlags(), pMemoryType.heapIndex()));
			}

			for (int i = 0; i < pMemoryProperties.memoryHeapCount(); ++i) {
				var pMemoryHeap = pMemoryProperties.memoryHeaps(i);
				this.heaps.add(new MemoryHeap(pMemoryHeap.flags(), pMemoryHeap.size()));
			}
		}
	}

	public int getMemoryTypeIndex(int typeFilter, int properties) {
		for (int i = 0; i < this.types.size(); ++i)
			if ((typeFilter & (1 << i)) != 0 && (this.types.get(i).propertyFlags & properties) == properties) return i;
		return -1;
	}

	public static class MemoryType {
		public int propertyFlags;
		public int heapIndex;

		public MemoryType(int propertyFlags, int heapIndex) {
			this.propertyFlags = propertyFlags;
			this.heapIndex     = heapIndex;
		}
	}

	public static class MemoryHeap {
		public int  heapFlags;
		public long heapSize;

		public MemoryHeap(int heapFlags, long heapSize) {
			this.heapFlags = heapFlags;
			this.heapSize  = heapSize;
		}
	}
}
