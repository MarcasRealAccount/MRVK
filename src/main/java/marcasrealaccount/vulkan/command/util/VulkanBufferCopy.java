package marcasrealaccount.vulkan.command.util;

public class VulkanBufferCopy {
	public long srcOffset, dstOffset;
	public long size;

	public VulkanBufferCopy(long srcOffset, long dstOffset, long size) {
		this.srcOffset = srcOffset;
		this.dstOffset = dstOffset;
		this.size      = size;
	}
}
