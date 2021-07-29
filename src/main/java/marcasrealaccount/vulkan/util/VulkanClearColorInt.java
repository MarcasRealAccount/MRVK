package marcasrealaccount.vulkan.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;

public class VulkanClearColorInt extends VulkanClearValue {
	public int     r, g, b, a;
	public boolean unsigned;

	public VulkanClearColorInt(int r, int g, int b, int a) {
		this(r, g, b, a, false);
	}

	public VulkanClearColorInt(int r, int g, int b, int a, boolean unsigned) {
		this.r        = r;
		this.g        = g;
		this.b        = b;
		this.a        = a;
		this.unsigned = unsigned;
	}

	@Override
	public void put(VkClearValue clearValue) {
		try (var stack = MemoryStack.stackPush()) {
			var pColor = stack.mallocInt(4);
			pColor.put(new int[] { this.r, this.g, this.b, this.a });
			if (this.unsigned)
				clearValue.color().uint32(pColor);
			else
				clearValue.color().int32(pColor);
		}
	}
}
