package marcasrealaccount.vulkan.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import marcasrealaccount.vulkan.util.VulkanExtension;
import marcasrealaccount.vulkan.util.VulkanLayer;

public class VulkanDevice extends VulkanHandle<VkDevice> {
	public final VulkanPhysicalDevice physicalDevice;

	private final ArrayList<VulkanExtension> extensions = new ArrayList<>();
	private final ArrayList<VulkanLayer> layers = new ArrayList<>();

	private VulkanQueue graphicsQueue = null;
	private VulkanQueue presentQueue = null;

	public VulkanDevice(VulkanPhysicalDevice physicalDevice) {
		super(null);
		this.physicalDevice = physicalDevice;
	}

	@Override
	protected void createAbstract() {
		if (!physicalDevice.validateExtensions(extensions))
			throw new RuntimeException("One or more device extensions are invalid");
		if (!physicalDevice.validateLayer(layers))
			throw new RuntimeException("One or more device layers are invalid");

		var indices = physicalDevice.indices;
		HashSet<Integer> uniqueQueueFamilies = new HashSet<Integer>();
		uniqueQueueFamilies.add(indices.graphicsFamily.get());
		uniqueQueueFamilies.add(indices.presentFamily.get());

		try (var stack = MemoryStack.stackPush()) {
			var queuePriority = stack.mallocFloat(1);
			var createInfo = VkDeviceCreateInfo.mallocStack(stack);
			var queueCreateInfos = VkDeviceQueueCreateInfo.mallocStack(uniqueQueueFamilies.size(), stack);
			var deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
			var pDevice = stack.mallocPointer(1);

			queuePriority.put(0, 1.0f);
			int i = 0;
			for (int queueFamily : uniqueQueueFamilies) {
				var queueCreateInfo = queueCreateInfos.get(i);
				queueCreateInfo.set(VK12.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO, 0, 0, queueFamily, queuePriority);
				++i;
			}

			var pExtensionNames = MemoryUtil.memAllocPointer(this.extensions.size());
			for (i = 0; i < this.extensions.size(); ++i)
				pExtensionNames.put(i, MemoryUtil.memUTF8(this.extensions.get(i).name));
			var pLayerNames = MemoryUtil.memAllocPointer(this.layers.size());
			for (i = 0; i < this.layers.size(); ++i)
				pLayerNames.put(i, MemoryUtil.memUTF8(this.layers.get(i).name));

			createInfo.set(VK12.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO, 0, 0, queueCreateInfos, pLayerNames,
					pExtensionNames, deviceFeatures);

			if (VK12.vkCreateDevice(this.physicalDevice.getHandle(), createInfo, null, pDevice) == VK12.VK_SUCCESS) {
				this.handle = new VkDevice(pDevice.get(0), this.physicalDevice.getHandle(), createInfo);
				this.physicalDevice.addInvalidate(this);

				this.graphicsQueue = new VulkanQueue(this);
				this.graphicsQueue.queueFamilyIndex = indices.graphicsFamily.get();
				this.graphicsQueue.create();

				this.presentQueue = new VulkanQueue(this);
				this.presentQueue.queueFamilyIndex = indices.presentFamily.get();
				this.presentQueue.create();
			} else {
				this.handle = null;
			}

			for (i = 0; i < pExtensionNames.capacity(); ++i)
				MemoryUtil.memFree(pExtensionNames.getByteBuffer(i, 1));
			MemoryUtil.memFree(pExtensionNames);
			for (i = 0; i < pLayerNames.capacity(); ++i)
				MemoryUtil.memFree(pLayerNames.getByteBuffer(i, 1));
			MemoryUtil.memFree(pLayerNames);
		}
	}

	@Override
	protected void closeAbstract(boolean recreate, boolean wasInvalidated) {
		VK12.vkDestroyDevice(this.handle, null);
		if (!wasInvalidated)
			this.physicalDevice.removeInvalidate(this);
		this.graphicsQueue = null;
		this.presentQueue = null;
	}

	public VulkanQueue getGraphicsQueue() {
		return this.graphicsQueue;
	}

	public VulkanQueue getPresentQueue() {
		return this.presentQueue;
	}

	public void forceUseExtension(String name, int minVersion) {
		this.extensions.add(new VulkanExtension(name, minVersion));
	}

	public void forceUselayer(String name, int minVersion) {
		this.layers.add(new VulkanLayer(name, minVersion));
	}

	public boolean useExtension(String name, int minVersion) {
		if (this.physicalDevice.hasExtension(name, minVersion)) {
			this.extensions.add(new VulkanExtension(name, minVersion));
			return true;
		}
		return false;
	}

	public boolean uselayer(String name, int minVersion) {
		if (this.physicalDevice.hasLayer(name, minVersion)) {
			this.layers.add(new VulkanLayer(name, minVersion));
			return true;
		}
		return false;
	}

	public boolean usesExtension(String name, int minVersion) {
		for (var extension : this.extensions)
			if (extension.name.equals(name) && extension.version > minVersion)
				return true;
		return false;
	}

	public boolean usesLayer(String name, int minVersion) {
		for (var layer : this.layers)
			if (layer.name.equals(name) && layer.version > minVersion)
				return true;
		return false;
	}

	public List<VulkanExtension> getExtensions() {
		return this.extensions;
	}

	public List<VulkanLayer> getLayers() {
		return this.layers;
	}
}
