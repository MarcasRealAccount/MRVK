package marcasrealaccount.vulkan.pipeline;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCopyDescriptorSet;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.image.VulkanImageView;
import marcasrealaccount.vulkan.memory.VulkanBuffer;

public class VulkanDescriptorSet extends VulkanHandle<Long> {
	public final VulkanDescriptorPool descriptorPool;

	public VulkanDescriptorSet(VulkanDescriptorPool descriptorPool, long handle) {
		super(0L, false);
		this.descriptorPool = descriptorPool;

		this.descriptorPool.addChild(this);

		this.handle = handle;
	}

	@Override
	protected void createAbstract() {}

	@Override
	protected void destroyAbstract() {}

	@Override
	protected void removeAbstract() {
		this.descriptorPool.removeChild(this);
	}

	public static void updateDescriptorSets(VulkanDevice device, WriteDescriptorSet[] writes, CopyDescriptorSet[] copies) {
		var pWrites = VkWriteDescriptorSet.malloc(writes != null ? writes.length : 0);
		var pCopies = VkCopyDescriptorSet.malloc(copies != null ? copies.length : 0);

		for (int i = 0; i < pWrites.capacity(); ++i) {
			var write  = writes[i];
			var pWrite = pWrites.get(i);

			VkDescriptorImageInfo.Buffer pImageInfos = null;
			if (write.imageInfo != null) {
				pImageInfos = VkDescriptorImageInfo.malloc(1);
				var pImageInfo = pImageInfos.get(0);
				pImageInfo.set(write.imageInfo.sampler.getHandle(), write.imageInfo.imageView.getHandle(), write.imageInfo.imageLayout);
			}

			VkDescriptorBufferInfo.Buffer pBufferInfos = null;
			if (write.bufferInfo != null) {
				pBufferInfos = VkDescriptorBufferInfo.malloc(1);
				var pBufferInfo = pBufferInfos.get(0);
				pBufferInfo.set(write.bufferInfo.buffer.getHandle(), write.bufferInfo.offset, write.bufferInfo.range);
			}

			LongBuffer pTexelBufferView = null;
			if (write.texelBufferView != null) {
				pTexelBufferView = MemoryUtil.memAllocLong(1);
				pTexelBufferView.put(0, write.texelBufferView.getHandle());
			}

			pWrite.set(VK12.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET, 0, write.dstSet.getHandle(), write.dstBinding, write.dstArrayElement,
					write.descriptorCount, write.descriptorType, pImageInfos, pBufferInfos, pTexelBufferView);
		}

		for (int i = 0; i < pCopies.capacity(); ++i) {
			var copy  = copies[i];
			var pCopy = pCopies.get(i);

			pCopy.set(VK12.VK_STRUCTURE_TYPE_COPY_DESCRIPTOR_SET, 0, copy.srcSet.getHandle(), copy.srcBinding, copy.srcArrayElement,
					copy.dstSet.getHandle(), copy.dstBinding, copy.dstArrayElement, copy.descriptorCount);
		}

		VK12.vkUpdateDescriptorSets(device.getHandle(), pWrites, pCopies);

		for (int i = 0; i < pWrites.capacity(); ++i) {
			var pWrite = pWrites.get(i);

			if (pWrite.pImageInfo() != null) pWrite.pImageInfo().free();
			if (pWrite.pBufferInfo() != null) pWrite.pBufferInfo().free();
			if (pWrite.pTexelBufferView() != null) MemoryUtil.memFree(pWrite.pTexelBufferView());
		}

		pWrites.free();
		pCopies.free();
	}

	public static class WriteDescriptorSet {
		public VulkanDescriptorSet dstSet;
		public int                 dstBinding, dstArrayElement;
		public int                 descriptorType, descriptorCount;
		public ImageInfo           imageInfo;
		public BufferInfo          bufferInfo;
		public VulkanHandle<Long>  texelBufferView;                // TODO: IMplement Texel Buffer View

		public WriteDescriptorSet(VulkanDescriptorSet dstSet, int dstBinding, int dstArrayElement, int descriptorType, int descriptorCount,
				ImageInfo imageInfo, BufferInfo bufferInfo, VulkanHandle<Long> texelBufferView) {
			this.dstSet          = dstSet;
			this.dstBinding      = dstBinding;
			this.dstArrayElement = dstArrayElement;
			this.descriptorType  = descriptorType;
			this.descriptorCount = descriptorCount;
			this.imageInfo       = imageInfo;
			this.bufferInfo      = bufferInfo;
			this.texelBufferView = texelBufferView;
		}

		public static class ImageInfo {
			public VulkanHandle<Long> sampler;    // TODO: Implement Image Sampler
			public VulkanImageView    imageView;
			public int                imageLayout;

			public ImageInfo(VulkanHandle<Long> sampler, VulkanImageView imageView, int imageLayout) {
				this.sampler     = sampler;
				this.imageView   = imageView;
				this.imageLayout = imageLayout;
			}
		}

		public static class BufferInfo {
			public VulkanBuffer buffer;
			public long         offset, range;

			public BufferInfo(VulkanBuffer buffer, long offset, long range) {
				this.buffer = buffer;
				this.offset = offset;
				this.range  = range;
			}
		}
	}

	public static class CopyDescriptorSet {
		public VulkanDescriptorSet srcSet;
		public int                 srcBinding, srcArrayElement;
		public VulkanDescriptorSet dstSet;
		public int                 dstBinding, dstArrayElement;
		public int                 descriptorCount;

		public CopyDescriptorSet(VulkanDescriptorSet srcSet, int srcBinding, int srcArrayElement, VulkanDescriptorSet dstSet, int dstBinding,
				int dstArrayElement, int descriptorCount) {
			this.srcSet          = srcSet;
			this.srcBinding      = srcBinding;
			this.srcArrayElement = srcArrayElement;
			this.dstSet          = dstSet;
			this.dstBinding      = dstBinding;
			this.dstArrayElement = dstArrayElement;
			this.descriptorCount = descriptorCount;
		}
	}
}
