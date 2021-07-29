package marcasrealaccount.vulkan.util;

public class VulkanExtension {
	public final String name;
	public final int    version;

	public VulkanExtension(String name, int version) {
		this.name    = name;
		this.version = version;
	}
}
