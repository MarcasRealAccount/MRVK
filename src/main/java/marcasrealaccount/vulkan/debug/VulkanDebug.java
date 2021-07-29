package marcasrealaccount.vulkan.debug;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;

import marcasrealaccount.vulkan.Reference;
import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.instance.VulkanInstance;

public class VulkanDebug extends VulkanHandle<Long> {
	private static boolean ENABLED = Reference.USE_VALIDATION_LAYERS;

	public final VulkanInstance instance;

	public VulkanDebug(VulkanInstance instance) {
		super(0L);
		this.instance = instance;

		this.instance.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo      = VkDebugUtilsMessengerCreateInfoEXT.mallocStack(stack);
			var pDebugMessenger = stack.mallocLong(1);

			populateCreateInfo(createInfo);

			if (EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(this.instance.getHandle(), createInfo, null, pDebugMessenger) == VK12.VK_SUCCESS)
				this.handle = pDebugMessenger.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(this.instance.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.instance.removeChild(this);
	}

	public static void disable() {
		ENABLED = false;
	}

	public static boolean isEnabled() {
		return ENABLED;
	}

	public static String getTypes(int types) {
		var added = false;
		var str   = "";
		if ((types & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
			added  = true;
			str   += "General";
			types &= ~EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
		}
		if ((types & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
			if (added) str += " | ";
			added  = true;
			str   += "Validation";
			types &= ~EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
		}
		if ((types & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
			if (added) str += " | ";
			added  = true;
			str   += "Performance";
			types &= ~EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
		}
		if (types != 0) str = "(" + str + ") + " + types;
		return str;
	}

	private static int debugCallback(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
		VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

		var message = "VK Validation Layer (" + getTypes(messageTypes) + "): " + callbackData.pMessageString();

		switch (messageSeverity) {
		case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT:
			Reference.LOGGER.trace(message);
			break;
		case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT:
			Reference.LOGGER.info(message);
			break;
		case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
			Reference.LOGGER.warn(message);
			break;
		case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
			Reference.LOGGER.error(message);
			break;
		default:
			Reference.LOGGER.debug(message);
		}

		return VK12.VK_FALSE;
	}

	public static void populateCreateInfo(VkDebugUtilsMessengerCreateInfoEXT createInfo) {
		createInfo.set(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT, 0, 0,
				EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
						| EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT,
				EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
						| EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT,
				VulkanDebug::debugCallback, 0);
	}
}
