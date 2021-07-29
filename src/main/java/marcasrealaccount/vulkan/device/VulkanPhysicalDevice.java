package marcasrealaccount.vulkan.device;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.util.IVulkanPhysicalDeviceScorer;
import marcasrealaccount.vulkan.device.util.VulkanPhysicalDeviceMemoryProperties;
import marcasrealaccount.vulkan.device.util.VulkanQueueFamilyIndices;
import marcasrealaccount.vulkan.device.util.VulkanSwapchainSupportDetails;
import marcasrealaccount.vulkan.instance.VulkanInstance;
import marcasrealaccount.vulkan.surface.VulkanSurface;
import marcasrealaccount.vulkan.util.VulkanExtension;
import marcasrealaccount.vulkan.util.VulkanLayer;

public class VulkanPhysicalDevice extends VulkanHandle<VkPhysicalDevice> {
	public final VulkanInstance instance;
	public final VulkanSurface  surface;

	public IVulkanPhysicalDeviceScorer scorer = null;

	public final VulkanQueueFamilyIndices             indices                 = new VulkanQueueFamilyIndices();
	public final VulkanSwapchainSupportDetails        swapchainSupportDetails = new VulkanSwapchainSupportDetails();
	public final VulkanPhysicalDeviceMemoryProperties memoryProperties        = new VulkanPhysicalDeviceMemoryProperties();

	private ArrayList<VulkanExtension> availableExtensions;
	private ArrayList<VulkanLayer>     availableLayers;

	public VulkanPhysicalDevice(VulkanInstance instance, VulkanSurface surface) {
		super(null, false);
		this.instance = instance;
		this.surface  = surface;

		this.instance.addChild(this);
		this.surface.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var deviceCount = stack.mallocInt(1);
			VK12.vkEnumeratePhysicalDevices(this.instance.getHandle(), deviceCount, null);
			if (deviceCount.get(0) == 0) return;

			var pDevices = MemoryUtil.memAllocPointer(deviceCount.get(0));
			VK12.vkEnumeratePhysicalDevices(this.instance.getHandle(), deviceCount, pDevices);

			VkPhysicalDevice bestPhysicalDevice = null;
			long             bestScore          = -1;

			for (int i = 0; i < pDevices.capacity(); ++i) {
				var  physicalDevice = new VkPhysicalDevice(pDevices.get(i), this.instance.getHandle());
				long score          = this.scorer.score(physicalDevice);
				if (score > bestScore) {
					bestScore          = score;
					bestPhysicalDevice = physicalDevice;
				}
			}

			MemoryUtil.memFree(pDevices);

			this.handle = bestPhysicalDevice;
		}
		update();
	}

	@Override
	protected boolean destroyAbstract() {
		return true;
	}

	@Override
	protected void removeAbstract() {
		this.instance.removeChild(this);
		this.surface.removeChild(this);
	}

	public void update() {
		if (this.handle != null) {
			this.indices.getIndices(this);
			this.swapchainSupportDetails.getSupport(this);
			this.memoryProperties.getProperties(this);
		}
	}

	public boolean hasExtension(String name, int minVersion) {
		queryAvailableExtensions();
		for (var extension : this.availableExtensions) if (extension.name.equals(name) && extension.version > minVersion) return true;
		return false;
	}

	public boolean hasLayer(String name, int minVersion) {
		queryAvailableLayers();
		for (var layer : this.availableLayers) if (layer.name.equals(name) && layer.version > minVersion) return true;
		return false;
	}

	public boolean validateExtensions(List<VulkanExtension> extensions) {
		for (var extension : extensions) if (!hasExtension(extension.name, extension.version)) return false;
		return true;
	}

	public boolean validateLayer(List<VulkanLayer> layers) {
		for (var layer : layers) if (!hasLayer(layer.name, layer.version)) return false;
		return true;
	}

	public ArrayList<VulkanExtension> getAvailableExtensions() {
		return availableExtensions;
	}

	public ArrayList<VulkanLayer> getAvailableLayers() {
		return availableLayers;
	}

	private void queryAvailableExtensions() {
		if (this.availableExtensions != null) return;

		try (var stack = MemoryStack.stackPush()) {
			var extensionCount = stack.mallocInt(1);
			VK12.vkEnumerateDeviceExtensionProperties(this.handle, "", extensionCount, null);
			var extensions = VkExtensionProperties.malloc(extensionCount.get(0));
			VK12.vkEnumerateDeviceExtensionProperties(this.handle, "", extensionCount, extensions);

			this.availableExtensions = new ArrayList<>(extensions.capacity());
			for (var extension : extensions)
				this.availableExtensions.add(new VulkanExtension(extension.extensionNameString(), extension.specVersion()));

			extensions.free();
		}
	}

	private void queryAvailableLayers() {
		if (this.availableLayers != null) return;

		try (var stack = MemoryStack.stackPush()) {
			var layerCount = stack.mallocInt(1);
			VK12.vkEnumerateDeviceLayerProperties(this.handle, layerCount, null);
			var layers = VkLayerProperties.malloc(layerCount.get(0));
			VK12.vkEnumerateDeviceLayerProperties(this.handle, layerCount, layers);

			this.availableLayers = new ArrayList<>(layers.capacity());
			for (var layer : layers) this.availableLayers.add(new VulkanLayer(layer.layerNameString(), layer.specVersion()));

			layers.free();
		}
	}
}
