package marcasrealaccount.vulkan.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;

public class VulkanClearColorFloat extends VulkanClearValue {
	public float r, g, b, a;

	public VulkanClearColorFloat(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	@Override
	public void put(VkClearValue clearValue) {
		try (var stack = MemoryStack.stackPush()) {
			var pColor = stack.mallocFloat(4);
			pColor.put(0, new float[] { this.r, this.g, this.b, this.a });
			clearValue.color().float32(pColor);
		}
	}
}
