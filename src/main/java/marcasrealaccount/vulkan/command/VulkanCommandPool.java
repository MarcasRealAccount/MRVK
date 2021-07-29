package marcasrealaccount.vulkan.command;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import marcasrealaccount.vulkan.VulkanHandle;
import marcasrealaccount.vulkan.device.VulkanDevice;

public class VulkanCommandPool extends VulkanHandle<Long> {
	public final VulkanDevice device;

	public int queueFamilyIndex = 0;

	private final HashMap<Integer, ArrayList<VulkanCommandBuffer>> commandBufferLevels = new HashMap<>();

	public VulkanCommandPool(VulkanDevice device) {
		super(0L);
		this.device = device;

		this.device.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var createInfo   = VkCommandPoolCreateInfo.mallocStack(stack);
			var pCommandPool = stack.mallocLong(1);

			createInfo.set(VK12.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO, 0, 0, this.queueFamilyIndex);

			if (VK12.vkCreateCommandPool(this.device.getHandle(), createInfo, null, pCommandPool) == VK12.VK_SUCCESS)
				this.handle = pCommandPool.get(0);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyCommandPool(this.device.getHandle(), this.handle, null);
		for (var commandBuffers : this.commandBufferLevels.values()) for (var commandBuffer : commandBuffers) commandBuffer.remove();
		this.commandBufferLevels.clear();
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
	}

	public void reset() {
		VK12.vkResetCommandPool(this.device.getHandle(), this.handle, 0);
	}

	public ArrayList<VulkanCommandBuffer> allocateBuffers(int level, int count) {
		var buffers = new ArrayList<VulkanCommandBuffer>();

		try (var stack = MemoryStack.stackPush()) {
			var allocateInfo    = VkCommandBufferAllocateInfo.mallocStack(stack);
			var pCommandBuffers = stack.mallocPointer(count);

			allocateInfo.set(VK12.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO, 0, this.handle, level, count);

			if (VK12.vkAllocateCommandBuffers(this.device.getHandle(), allocateInfo, pCommandBuffers) == VK12.VK_SUCCESS) {
				var commandBuffers = this.commandBufferLevels.get(level);
				if (commandBuffers == null) {
					commandBuffers = new ArrayList<>();
					this.commandBufferLevels.put(level, commandBuffers);
				}

				for (int i = 0; i < count; ++i) {
					var buffer = new VulkanCommandBuffer(this, new VkCommandBuffer(pCommandBuffers.get(i), this.device.getHandle()), level);
					commandBuffers.add(buffer);
					buffers.add(buffer);
				}
			}
		}

		return buffers;
	}

	public VulkanCommandBuffer getCommandBuffer(int level, int index) {
		var commandBuffers = this.commandBufferLevels.get(level);
		return commandBuffers != null ? index >= 0 && index < commandBuffers.size() ? commandBuffers.get(index) : null : null;
	}

	public ArrayList<VulkanCommandBuffer> getCommandBuffers() {
		var buffers = new ArrayList<VulkanCommandBuffer>();
		for (var value : this.commandBufferLevels.values()) buffers.addAll(value);
		return buffers;
	}
}
