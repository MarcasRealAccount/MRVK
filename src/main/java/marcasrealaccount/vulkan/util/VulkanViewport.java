package marcasrealaccount.vulkan.util;

public class VulkanViewport {
	public float x, y, width, height, minDepth, maxDepth;

	public VulkanViewport(float x, float y, float width, float height) {
		this(x, y, width, height, 0.0f, 1.0f);
	}

	public VulkanViewport(float x, float y, float width, float height, float minDepth, float maxDepth) {
		this.x        = x;
		this.y        = y;
		this.width    = width;
		this.height   = height;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}
}
