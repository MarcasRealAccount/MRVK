package marcasrealaccount.vulkan.instance;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import marcasrealaccount.vulkan.Reference;
import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.debug.VulkanDebug;
import marcasrealaccount.vulkan.util.VulkanExtension;
import marcasrealaccount.vulkan.util.VulkanLayer;
import net.minecraft.MinecraftVersion;

public class VulkanInstance extends VulkanHandle<VkInstance> {
	private static ArrayList<VulkanExtension> AvailableExtensions = null;
	private static ArrayList<VulkanLayer>     AvailableLayers     = null;

	private final ArrayList<VulkanExtension> extensions = new ArrayList<>();
	private final ArrayList<VulkanLayer>     layers     = new ArrayList<>();

	public VulkanInstance() {
		super(null);
	}

	@Override
	protected void createAbstract() {
		if (!validateExtensions(this.extensions)) throw new RuntimeException("One or more instance extensions are invalid");
		if (!validateLayers(this.layers)) throw new RuntimeException("One or more instance layers are invalid");

		var version     = MinecraftVersion.GAME_VERSION.getReleaseTarget();
		var versions    = version.split(".");
		var versionsInt = new int[versions.length];
		for (int i = 0; i < versions.length; ++i) versionsInt[i] = Integer.parseInt(versions[i]);
		int versionMajor = versionsInt.length > 0 ? versionsInt[0] : 0;
		int versionMinor = versionsInt.length > 1 ? versionsInt[1] : 0;
		int versionPatch = versionsInt.length > 2 ? versionsInt[2] : 0;
		try (var stack = MemoryStack.stackPush()) {
			var applicationName = stack.UTF8("Minecraft");
			var engineName      = stack.UTF8("MRVK");
			var appInfo         = VkApplicationInfo.mallocStack(stack);
			var createInfo      = VkInstanceCreateInfo.mallocStack(stack);
			var pInstance       = stack.mallocPointer(1);

			appInfo.set(VK12.VK_STRUCTURE_TYPE_APPLICATION_INFO, 0, applicationName, VK12.VK_MAKE_VERSION(versionMajor, versionMinor, versionPatch),
					engineName, VK12.VK_MAKE_VERSION(Reference.MAJOR, Reference.MINOR, 0), VK12.VK_API_VERSION_1_2);

			long pNext = 0;
			if (VulkanDebug.isEnabled()) {
				// var validationFeatures = VkValidationFeaturesEXT.mallocStack(stack);
				// var enabledValidationFeatures = stack.mallocInt(4);
				//
				// enabledValidationFeatures.put(0, EXTValidationFeatures.VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT);
				// enabledValidationFeatures.put(1, EXTValidationFeatures.VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_EXT);
				// enabledValidationFeatures.put(2,
				// EXTValidationFeatures.VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT);
				// enabledValidationFeatures.put(3,
				// EXTValidationFeatures.VK_VALIDATION_FEATURE_ENABLE_GPU_ASSISTED_RESERVE_BINDING_SLOT_EXT);
				// validationFeatures.set(EXTValidationFeatures.VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT, 0,
				// enabledValidationFeatures, null);
				var debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.mallocStack(stack);
				VulkanDebug.populateCreateInfo(debugCreateInfo);
				// debugCreateInfo.pNext(validationFeatures.address());
				pNext = debugCreateInfo.address();
			}

			var pExtensionNames = MemoryUtil.memAllocPointer(this.extensions.size());
			for (int i = 0; i < this.extensions.size(); ++i) pExtensionNames.put(i, MemoryUtil.memUTF8(this.extensions.get(i).name));
			var pLayerNames = MemoryUtil.memAllocPointer(this.layers.size());
			for (int i = 0; i < this.layers.size(); ++i) pLayerNames.put(i, MemoryUtil.memUTF8(this.layers.get(i).name));

			createInfo.set(VK12.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO, pNext, 0, appInfo, pLayerNames, pExtensionNames);

			if (VK12.vkCreateInstance(createInfo, null, pInstance) == VK12.VK_SUCCESS)
				this.handle = new VkInstance(pInstance.get(0), createInfo);
			else
				this.handle = null;

			for (int i = 0; i < pExtensionNames.capacity(); ++i) MemoryUtil.memFree(pExtensionNames.getByteBuffer(i, 1));
			MemoryUtil.memFree(pExtensionNames);
			for (int i = 0; i < pLayerNames.capacity(); ++i) MemoryUtil.memFree(pLayerNames.getByteBuffer(i, 1));
			MemoryUtil.memFree(pLayerNames);
		}
	}

	@Override
	protected boolean destroyAbstract() {
		VK12.vkDestroyInstance(this.handle, null);
		return true;
	}

	@Override
	protected void removeAbstract() {}

	public List<VulkanExtension> getExtensions() {
		return this.extensions;
	}

	public List<VulkanLayer> getLayers() {
		return this.layers;
	}

	public void forceUseExtension(String name, int minVersion) {
		this.extensions.add(new VulkanExtension(name, minVersion));
	}

	public void forceUseLayer(String name, int minVersion) {
		this.layers.add(new VulkanLayer(name, minVersion));
	}

	public boolean useExtension(String name, int minVersion) {
		if (hasExtension(name, minVersion)) {
			this.extensions.add(new VulkanExtension(name, minVersion));
			return true;
		}
		return false;
	}

	public boolean useLayer(String name, int minVersion) {
		if (hasLayer(name, minVersion)) {
			this.layers.add(new VulkanLayer(name, minVersion));
			return true;
		}
		return false;
	}

	public boolean usesExtension(String name, int minVersion) {
		for (var extension : this.extensions) if (extension.name.equals(name) && extension.version > minVersion) return true;
		return false;
	}

	public boolean usesLayer(String name, int minVersion) {
		for (var layer : this.layers) if (layer.name.equals(name) && layer.version > minVersion) return true;
		return false;
	}

	public static boolean hasExtension(String name, int minVersion) {
		queryAvailableExtensions();
		for (var extension : AvailableExtensions) if (extension.name.equals(name) && extension.version > minVersion) return true;
		return false;
	}

	public static boolean hasLayer(String name, int minVersion) {
		queryAvailableLayers();
		for (var layer : AvailableLayers) if (layer.name.equals(name) && layer.version > minVersion) return true;
		return false;
	}

	public static boolean validateExtensions(List<VulkanExtension> extensions) {
		for (var extension : extensions) if (!hasExtension(extension.name, extension.version)) return false;
		return true;
	}

	public static boolean validateLayers(List<VulkanLayer> layers) {
		for (var layer : layers) if (!hasLayer(layer.name, layer.version)) return false;
		return true;
	}

	public static ArrayList<VulkanExtension> getAvailableExtensions() {
		return AvailableExtensions;
	}

	public static ArrayList<VulkanLayer> getAvailableLayers() {
		return AvailableLayers;
	}

	private static void queryAvailableExtensions() {
		if (AvailableExtensions != null) return;

		try (var stack = MemoryStack.stackPush()) {
			var extensionCount = stack.mallocInt(1);
			VK12.vkEnumerateInstanceExtensionProperties("", extensionCount, null);
			var extensions = VkExtensionProperties.malloc(extensionCount.get(0));
			VK12.vkEnumerateInstanceExtensionProperties("", extensionCount, extensions);

			AvailableExtensions = new ArrayList<>(extensions.capacity());
			for (var extension : extensions) AvailableExtensions.add(new VulkanExtension(extension.extensionNameString(), extension.specVersion()));

			extensions.free();
		}
	}

	private static void queryAvailableLayers() {
		if (AvailableLayers != null) return;

		try (var stack = MemoryStack.stackPush()) {
			var layerCount = stack.mallocInt(1);
			VK12.vkEnumerateInstanceLayerProperties(layerCount, null);
			var layers = VkLayerProperties.malloc(layerCount.get(0));
			VK12.vkEnumerateInstanceLayerProperties(layerCount, layers);

			AvailableLayers = new ArrayList<>(layers.capacity());
			for (var layer : layers) AvailableLayers.add(new VulkanLayer(layer.layerNameString(), layer.specVersion()));

			layers.free();
		}
	}
}
