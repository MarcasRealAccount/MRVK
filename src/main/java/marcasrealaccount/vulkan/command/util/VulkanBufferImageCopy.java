package marcasrealaccount.vulkan.command.util;

import marcasrealaccount.vulkan.util.VulkanExtent3D;
import marcasrealaccount.vulkan.util.VulkanImageSubresourceLayers;
import marcasrealaccount.vulkan.util.VulkanOffset3D;

public class VulkanBufferImageCopy {
	public long bufferOffset;
	public int  bufferRowLength, bufferImageHeight;

	public VulkanImageSubresourceLayers subresourceLayers;

	public VulkanOffset3D imageOffset;
	public VulkanExtent3D imageExtent;

	public VulkanBufferImageCopy(long bufferOffset, int bufferRowLength, int bufferImageHeight, VulkanImageSubresourceLayers subresourceLayers,
			VulkanOffset3D imageOffset, VulkanExtent3D imageExtent) {
		this.bufferOffset      = bufferOffset;
		this.bufferRowLength   = bufferRowLength;
		this.bufferImageHeight = bufferImageHeight;
		this.subresourceLayers = subresourceLayers;
		this.imageOffset       = imageOffset;
		this.imageExtent       = imageExtent;
	}
}
