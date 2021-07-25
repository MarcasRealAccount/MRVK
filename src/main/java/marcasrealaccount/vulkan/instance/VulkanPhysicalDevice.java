package marcasrealaccount.vulkan.instance;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;

import marcasrealaccount.vulkan.util.IVulkanPhysicalDeviceScorer;
import marcasrealaccount.vulkan.util.VulkanExtension;
import marcasrealaccount.vulkan.util.VulkanLayer;
import marcasrealaccount.vulkan.util.VulkanQueueFamilyIndices;
import marcasrealaccount.vulkan.util.VulkanSwapchainSupportDetails;

public class VulkanPhysicalDevice {
	public final VulkanInstance instance;
	public final VulkanSurface surface;
	public final VulkanQueueFamilyIndices indices;
	public final VulkanSwapchainSupportDetails swapchainSupportDetails;
	private VkPhysicalDevice handle;

	private ArrayList<VulkanExtension> availableExtensions;
	private ArrayList<VulkanLayer> availableLayers;

	public VulkanPhysicalDevice(VulkanInstance instance, VulkanSurface surface, VkPhysicalDevice handle) {
		this.instance = instance;
		this.surface = surface;
		this.handle = handle;
		this.indices = VulkanQueueFamilyIndices.getIndices(this.handle, this.surface);
		this.swapchainSupportDetails = VulkanSwapchainSupportDetails.getSupport(this.handle, this.surface);
	}

	public VkPhysicalDevice getHandle() {
		return this.handle;
	}

	public boolean hasExtension(String name, int minVersion) {
		queryAvailableExtensions();
		for (var extension : this.availableExtensions)
			if (extension.name.equals(name) && extension.version > minVersion)
				return true;
		return false;
	}

	public boolean hasLayer(String name, int minVersion) {
		queryAvailableLayers();
		for (var layer : this.availableLayers)
			if (layer.name.equals(name) && layer.version > minVersion)
				return true;
		return false;
	}

	public boolean validateExtensions(List<VulkanExtension> extensions) {
		for (var extension : extensions)
			if (!hasExtension(extension.name, extension.version))
				return false;
		return true;
	}

	public boolean validateLayer(List<VulkanLayer> layers) {
		for (var layer : layers)
			if (!hasLayer(layer.name, layer.version))
				return false;
		return true;
	}

	public ArrayList<VulkanExtension> getAvailableExtensions() {
		return availableExtensions;
	}

	public ArrayList<VulkanLayer> getAvailableLayers() {
		return availableLayers;
	}

	private void queryAvailableExtensions() {
		if (this.availableExtensions != null)
			return;

		try (var stack = MemoryStack.stackPush()) {
			var extensionCount = stack.mallocInt(1);
			VK12.vkEnumerateDeviceExtensionProperties(this.handle, "", extensionCount, null);
			var extensions = VkExtensionProperties.malloc(extensionCount.get(0));
			VK12.vkEnumerateDeviceExtensionProperties(this.handle, "", extensionCount, extensions);

			this.availableExtensions = new ArrayList<>(extensions.capacity());
			for (var extension : extensions)
				this.availableExtensions
						.add(new VulkanExtension(extension.extensionNameString(), extension.specVersion()));

			extensions.free();
		}
	}

	private void queryAvailableLayers() {
		if (this.availableLayers != null)
			return;

		try (var stack = MemoryStack.stackPush()) {
			var layerCount = stack.mallocInt(1);
			VK12.vkEnumerateDeviceLayerProperties(this.handle, layerCount, null);
			var layers = VkLayerProperties.malloc(layerCount.get(0));
			VK12.vkEnumerateDeviceLayerProperties(this.handle, layerCount, layers);

			this.availableLayers = new ArrayList<>(layers.capacity());
			for (var layer : layers)
				this.availableLayers.add(new VulkanLayer(layer.layerNameString(), layer.specVersion()));

			layers.free();
		}
	}

	public void addInvalidate(VulkanHandle<?> invalidate) {
		this.instance.addInvalidate(invalidate);
	}

	public void removeInvalidate(VulkanHandle<?> invalidate) {
		this.instance.removeInvalidate(invalidate);
	}

	public static VulkanPhysicalDevice pickBestPhysicalDevice(VulkanInstance instance, VulkanSurface surface,
			IVulkanPhysicalDeviceScorer scorer) {
		try (var stack = MemoryStack.stackPush()) {
			var deviceCount = stack.mallocInt(1);
			VK12.vkEnumeratePhysicalDevices(instance.getHandle(), deviceCount, null);
			if (deviceCount.get(0) == 0)
				return null;

			var pDevices = MemoryUtil.memAllocPointer(deviceCount.get(0));
			VK12.vkEnumeratePhysicalDevices(instance.getHandle(), deviceCount, pDevices);

			VulkanPhysicalDevice bestPhysicalDevice = null;

			long bestScore = -1;

			for (int i = 0; i < pDevices.capacity(); ++i) {
				var physicalDevice = new VulkanPhysicalDevice(instance, surface,
						new VkPhysicalDevice(pDevices.get(i), instance.getHandle()));
				long score = scorer.score(physicalDevice);
				if (score > bestScore) {
					bestScore = score;
					bestPhysicalDevice = physicalDevice;
				}
			}

			MemoryUtil.memFree(pDevices);

			return bestPhysicalDevice;
		}
	}
}
