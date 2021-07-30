package marcasrealaccount.vulkan;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import com.google.common.collect.Lists;

import marcasrealaccount.vulkan.command.VulkanCommandBuffer;
import marcasrealaccount.vulkan.command.VulkanCommandPool;
import marcasrealaccount.vulkan.command.util.VulkanBufferCopy;
import marcasrealaccount.vulkan.command.util.VulkanBufferImageCopy;
import marcasrealaccount.vulkan.command.util.VulkanImageMemoryBarrier;
import marcasrealaccount.vulkan.debug.VulkanDebug;
import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.device.VulkanPhysicalDevice;
import marcasrealaccount.vulkan.device.VulkanQueue;
import marcasrealaccount.vulkan.image.VulkanFramebuffer;
import marcasrealaccount.vulkan.image.VulkanImage;
import marcasrealaccount.vulkan.image.VulkanImageView;
import marcasrealaccount.vulkan.image.VulkanSampler;
import marcasrealaccount.vulkan.instance.VulkanInstance;
import marcasrealaccount.vulkan.memory.VulkanBuffer;
import marcasrealaccount.vulkan.memory.VulkanMemoryAllocator;
import marcasrealaccount.vulkan.pipeline.VulkanDescriptorPool;
import marcasrealaccount.vulkan.pipeline.VulkanDescriptorPool.PoolSize;
import marcasrealaccount.vulkan.pipeline.VulkanDescriptorSet;
import marcasrealaccount.vulkan.pipeline.VulkanDescriptorSetLayout;
import marcasrealaccount.vulkan.pipeline.VulkanPipelineLayout;
import marcasrealaccount.vulkan.pipeline.VulkanRenderPass;
import marcasrealaccount.vulkan.shader.VulkanGraphicsPipeline;
import marcasrealaccount.vulkan.shader.VulkanGraphicsPipeline.VulkanShaderStage.EShaderStage;
import marcasrealaccount.vulkan.shader.VulkanShaderModule;
import marcasrealaccount.vulkan.surface.VulkanSurface;
import marcasrealaccount.vulkan.surface.VulkanSwapchain;
import marcasrealaccount.vulkan.sync.VulkanFence;
import marcasrealaccount.vulkan.sync.VulkanSemaphore;
import marcasrealaccount.vulkan.util.VulkanClearColorFloat;
import marcasrealaccount.vulkan.util.VulkanClearDepthStencil;
import marcasrealaccount.vulkan.util.VulkanClearValue;
import marcasrealaccount.vulkan.util.VulkanImageSubresourceLayers;
import marcasrealaccount.vulkan.util.VulkanImageSubresourceRange;
import marcasrealaccount.vulkan.util.VulkanOffset3D;
import marcasrealaccount.vulkan.util.VulkanScissor;
import marcasrealaccount.vulkan.util.VulkanSurfaceFormat;
import marcasrealaccount.vulkan.util.VulkanViewport;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

public class Vulkan {
	public static final Vulkan INSTANCE             = new Vulkan();
	private static final int   MAX_FRAMES_IN_FLIGHT = 2;

	private VulkanInstance        instance        = new VulkanInstance();
	private VulkanDebug           debug           = new VulkanDebug(this.instance);
	private VulkanSurface         surface         = new VulkanSurface(this.instance);
	private VulkanPhysicalDevice  physicalDevice  = new VulkanPhysicalDevice(this.instance, this.surface);
	private VulkanDevice          device          = new VulkanDevice(this.physicalDevice);
	private VulkanQueue           graphicsQueue   = new VulkanQueue(this.device);
	private VulkanMemoryAllocator memoryAllocator = new VulkanMemoryAllocator(this.device);

	private Window              window = null;
	private boolean             vsync  = false, minimized = false;
	private VulkanSurfaceFormat format = null;

	private boolean                            recreateSwapchain        = false;
	private VulkanSwapchain                    swapchain                = new VulkanSwapchain(this.memoryAllocator);
	private VulkanRenderPass                   renderPass               = new VulkanRenderPass(this.device);
	private final ArrayList<VulkanImageView>   imageViews               = new ArrayList<>();
	private final ArrayList<VulkanImage>       swapchainDepthImages     = new ArrayList<>();
	private final ArrayList<VulkanImageView>   swapchainDepthImageViews = new ArrayList<>();
	private final ArrayList<VulkanFramebuffer> framebuffers             = new ArrayList<>();
	private final ArrayList<VulkanFence>       imagesInFlight           = new ArrayList<>();
	private int                                currentImage             = 0;

	private final ArrayList<VulkanCommandPool> commandPools             = new ArrayList<>(Vulkan.MAX_FRAMES_IN_FLIGHT);
	private final ArrayList<VulkanSemaphore>   imageAvailableSemaphores = new ArrayList<>(Vulkan.MAX_FRAMES_IN_FLIGHT);
	private final ArrayList<VulkanSemaphore>   renderFinishedSemaphores = new ArrayList<>(Vulkan.MAX_FRAMES_IN_FLIGHT);
	private final ArrayList<VulkanFence>       inFlightFences           = new ArrayList<>(Vulkan.MAX_FRAMES_IN_FLIGHT);
	private int                                currentFrame             = 0;

	public void setMinimized(boolean minimized) {
		this.minimized = minimized;
	}

	public void setVSync(boolean vsync) {
		if (this.vsync != vsync) this.recreateSwapchain = true;
		this.vsync = vsync;
	}

	public void recreateSwapchain() {
		this.recreateSwapchain = true;
	}

	public void initVulkan(Window window, boolean vsync) {
		this.window = window;
		this.vsync  = vsync;

		if (Reference.USE_VALIDATION_LAYERS) if (!this.instance.useLayer("VK_LAYER_KHRONOS_validation", 0))
			VulkanDebug.disable();
		else
			this.instance.useExtension(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME, 0);

		this.surface.setWindow(window);
		this.physicalDevice.scorer = Vulkan::scorePhysicalDevice;

		{
			var     glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
			boolean failed         = false;

			for (int i = 0; i < glfwExtensions.capacity(); ++i) {
				String extensionName = glfwExtensions.getStringUTF8(i);
				if (!this.instance.useExtension(extensionName, 0)) {
					failed = true;
					Reference.LOGGER.error("Missing Instance extension '" + extensionName + "'");
				}
			}

			if (failed) throw new RuntimeException("Missing Instance extension(s)");
		}

		if (!this.instance.create()) throw new RuntimeException("Failed to create Vulkan Instance");
		if (VulkanDebug.isEnabled()) this.debug.create();
		if (!this.surface.create()) throw new RuntimeException("Failed to create Vulkan Surface");
		if (!this.physicalDevice.create()) throw new RuntimeException("Failed to find a suitable GPU");

		if (!this.device.useExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME, 0))
			throw new RuntimeException("Missing Vulkan KHRSwapchan extension");

		if (!this.device.create()) throw new RuntimeException("Failed to create Vulkan Device");

		var indices = this.physicalDevice.indices;
		this.graphicsQueue.queueFamilyIndex = indices.graphicsFamily.get();

		if (!this.graphicsQueue.create()) throw new RuntimeException("Failed to create Graphics Queue");

		if (!this.memoryAllocator.create()) throw new RuntimeException("Failed to create Memory Allocator");

		this.commandPools.ensureCapacity(Vulkan.MAX_FRAMES_IN_FLIGHT);
		this.imageAvailableSemaphores.ensureCapacity(Vulkan.MAX_FRAMES_IN_FLIGHT);
		this.renderFinishedSemaphores.ensureCapacity(Vulkan.MAX_FRAMES_IN_FLIGHT);
		this.inFlightFences.ensureCapacity(Vulkan.MAX_FRAMES_IN_FLIGHT);
		for (int i = 0; i < Vulkan.MAX_FRAMES_IN_FLIGHT; ++i) {
			var commandPool = new VulkanCommandPool(this.device);
			if (!commandPool.create()) throw new RuntimeException("Failed to create Vulkan Command Pool");
			commandPool.allocateBuffers(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 1);
			this.commandPools.add(commandPool);

			var ias = new VulkanSemaphore(this.device);
			var rfs = new VulkanSemaphore(this.device);
			var iff = new VulkanFence(this.device);
			iff.signaled = true;
			if (!ias.create()) throw new RuntimeException("Failed to create Vulkan Semaphore");
			if (!rfs.create()) throw new RuntimeException("Failed to create Vulkan Semaphore");
			if (!iff.create()) throw new RuntimeException("Failed to create Vulkan Fence");

			this.imageAvailableSemaphores.add(ias);
			this.renderFinishedSemaphores.add(rfs);
			this.inFlightFences.add(iff);
		}

		createSwapchain();
	}

	private void createSwapchain() {
		for (var imageView : this.imageViews) {
			imageView.destroy();
			imageView.remove();
		}
		this.imageViews.clear();

		for (var depthImageView : this.swapchainDepthImageViews) {
			depthImageView.destroy();
			depthImageView.remove();
		}
		this.swapchainDepthImageViews.clear();

		for (var depthImage : this.swapchainDepthImages) {
			depthImage.destroy();
			depthImage.remove();
		}
		this.swapchainDepthImages.clear();

		for (var framebuffer : this.framebuffers) {
			framebuffer.destroy();
			framebuffer.remove();
		}
		this.framebuffers.clear();

		this.physicalDevice.update();

		var oldFormat               = this.format;
		var swapchainSupportDetails = this.physicalDevice.swapchainSupportDetails;
		this.format = swapchainSupportDetails.getSwapchainFormat();
		var extent = swapchainSupportDetails.getSwapchainExtent(this.window);

		if ((oldFormat == null && this.format != null) || (!oldFormat.equals(this.format))) {
			{
				this.renderPass.attachments.clear();
				this.renderPass.subpasses.clear();
				this.renderPass.dependencies.clear();

				var colorAttachment = new VulkanRenderPass.Attachment();
				colorAttachment.format = this.format.format;
				this.renderPass.attachments.add(colorAttachment);

				var depthAttachment = new VulkanRenderPass.Attachment();
				depthAttachment.format      = VK12.VK_FORMAT_D32_SFLOAT;
				depthAttachment.storeOp     = VK12.VK_ATTACHMENT_STORE_OP_DONT_CARE;
				depthAttachment.finalLayout = VK12.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
				this.renderPass.attachments.add(depthAttachment);

				var subpass            = new VulkanRenderPass.Subpass();
				var colorAttachmentRef = new VulkanRenderPass.Subpass.AttachmentRef();
				colorAttachmentRef.attachment = 0;
				colorAttachmentRef.layout     = VK12.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
				subpass.colorAttachmentRefs.add(colorAttachmentRef);
				subpass.useDepthStencilAttachment         = true;
				subpass.depthStencilAttachment.attachment = 1;
				subpass.depthStencilAttachment.layout     = VK12.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
				this.renderPass.subpasses.add(subpass);

				var dependency = new VulkanRenderPass.Dependency();
				dependency.srcSubpass    = VK12.VK_SUBPASS_EXTERNAL;
				dependency.dstSubpass    = 0;
				dependency.srcStageMask  = VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK12.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
				dependency.srcAccessMask = 0;
				dependency.dstStageMask  = VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK12.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
				dependency.dstAccessMask = VK12.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK12.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
				this.renderPass.dependencies.add(dependency);
			}
			if (!this.renderPass.create()) throw new RuntimeException("Failed to create Vulkan RenderPass");
		}

		this.swapchain.imageCount   = Math.min(swapchainSupportDetails.capabilities.minImageCount + 1,
				swapchainSupportDetails.capabilities.maxImageCount);
		this.swapchain.preTransform = swapchainSupportDetails.capabilities.currentTransform;
		this.swapchain.format       = this.format.format;
		this.swapchain.colorSpace   = this.format.colorSpace;
		this.swapchain.presentMode  = swapchainSupportDetails.getSwapchainPresentMode(this.vsync);
		this.swapchain.width        = extent.width;
		this.swapchain.height       = extent.height;
		var indices = this.physicalDevice.indices;
		this.swapchain.indices.clear();
		this.swapchain.indices.add(indices.graphicsFamily.get());
		if (!this.swapchain.create()) throw new RuntimeException("Failed to create Vulkan Swapchain");

		this.imageViews.ensureCapacity(this.swapchain.getNumImages());
		this.swapchainDepthImages.ensureCapacity(this.swapchain.getNumImages());
		this.swapchainDepthImageViews.ensureCapacity(this.swapchain.getNumImages());
		this.framebuffers.ensureCapacity(this.swapchain.getNumImages());
		for (int i = 0; i < this.swapchain.getNumImages(); ++i) {
			var imageView = new VulkanImageView(this.device, this.swapchain.getImage(i));
			imageView.format = this.format.format;
			if (!imageView.create()) throw new RuntimeException("Failed to create Vulkan ImageView");
			this.imageViews.add(imageView);

			var depthBufferImage = new VulkanImage(this.memoryAllocator);
			depthBufferImage.format = VK12.VK_FORMAT_D32_SFLOAT;
			depthBufferImage.usage  = VK12.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;

			depthBufferImage.extent.width  = this.swapchain.width;
			depthBufferImage.extent.height = this.swapchain.height;
			if (!depthBufferImage.create()) throw new RuntimeException("Failed to create Vulkan Image");
			this.swapchainDepthImages.add(depthBufferImage);

			var depthBufferImageView = new VulkanImageView(this.device, depthBufferImage);
			depthBufferImageView.format = VK12.VK_FORMAT_D32_SFLOAT;

			depthBufferImageView.subresourceRange.aspectMask = VK12.VK_IMAGE_ASPECT_DEPTH_BIT;
			if (!depthBufferImageView.create()) throw new RuntimeException("Failed to create Vulkan ImageView");
			this.swapchainDepthImageViews.add(depthBufferImageView);

			var framebuffer = new VulkanFramebuffer(this.device, this.renderPass);
			framebuffer.attachments.add(imageView);
			framebuffer.attachments.add(depthBufferImageView);
			framebuffer.width  = this.swapchain.width;
			framebuffer.height = this.swapchain.height;
			if (!framebuffer.create()) throw new RuntimeException("Failed to create Vulkan Framebuffers");
			this.framebuffers.add(framebuffer);
		}

		this.imagesInFlight.clear();
		this.imagesInFlight.ensureCapacity(this.swapchain.getNumImages());
		for (int i = 0; i < this.swapchain.getNumImages(); ++i) this.imagesInFlight.add(null);

		{
			var currentCommandPool = getCommandPool(this.currentFrame);
			currentCommandPool.reset();
			var currentCommandBuffer = currentCommandPool.getCommandBuffer(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 0);
			if (currentCommandBuffer.begin()) {
				var imageMemoryBarriers = new VulkanImageMemoryBarrier[this.swapchain.imageCount];
				for (int i = 0; i < this.swapchain.imageCount; ++i) imageMemoryBarriers[i] = new VulkanImageMemoryBarrier(0,
						VK12.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK12.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
						VK12.VK_QUEUE_FAMILY_IGNORED, VK12.VK_QUEUE_FAMILY_IGNORED, VK12.VK_IMAGE_LAYOUT_UNDEFINED,
						VK12.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, this.swapchainDepthImages.get(i),
						new VulkanImageSubresourceRange(VK12.VK_IMAGE_ASPECT_DEPTH_BIT, 0, 1, 0, 1));

				currentCommandBuffer.cmdPipelineBarrier(VK12.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK12.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT, 0,
						null, null, imageMemoryBarriers);

				currentCommandBuffer.end();

				this.graphicsQueue.submitCommandBuffers(new VulkanCommandBuffer[] { currentCommandBuffer }, null, null, null, null);
				this.graphicsQueue.waitIdle();
			}
		}

		this.recreateSwapchain = false;
	}

	public void destroy() {
		VK12.vkDeviceWaitIdle(this.device.getHandle());

		this.instance.destroy();
	}

	public void beginFrame() {
		if (this.minimized) {
			this.commandPools.get(this.currentFrame).reset();
			return;
		}

		if (this.recreateSwapchain) createSwapchain();

		try (var stack = MemoryStack.stackPush()) {
			var imageIndex = stack.mallocInt(1);

			VK12.vkWaitForFences(this.device.getHandle(), this.inFlightFences.get(this.currentFrame).getHandle(), true, -1);
			VK12.vkResetFences(this.device.getHandle(), this.inFlightFences.get(this.currentFrame).getHandle());

			var result = KHRSwapchain.vkAcquireNextImageKHR(this.device.getHandle(), this.swapchain.getHandle(), -1,
					this.imageAvailableSemaphores.get(this.currentFrame).getHandle(), 0, imageIndex);

			if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
				createSwapchain();
				beginFrame();
				return;
			} else if (result != VK12.VK_SUCCESS && result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
				throw new RuntimeException("Failed to acquire next Vulkan Swapchain image");
			}

			this.currentImage = imageIndex.get(0);

			if (this.imagesInFlight.get(this.currentImage) != null)
				VK12.vkWaitForFences(this.device.getHandle(), this.imagesInFlight.get(this.currentImage).getHandle(), true, -1);
			this.imagesInFlight.set(this.currentImage, this.inFlightFences.get(this.currentFrame));

			this.commandPools.get(this.currentFrame).reset();
			this.inFlightFences.get(this.currentFrame).reset();
		}
	}

	public void endFrame() {
		if (this.minimized) return;

		var waitSemaphores   = new VulkanSemaphore[] { this.imageAvailableSemaphores.get(this.currentFrame) };
		var signalSemaphores = new VulkanSemaphore[] { this.renderFinishedSemaphores.get(this.currentFrame) };

		this.graphicsQueue.submitCommandBuffers(this.commandPools.get(this.currentFrame).getCommandBuffers().toArray(new VulkanCommandBuffer[0]),
				waitSemaphores, signalSemaphores, new int[] { VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT },
				this.inFlightFences.get(this.currentFrame));

		var result = this.graphicsQueue.present(new VulkanSwapchain[] { this.swapchain }, new int[] { this.currentImage }, signalSemaphores)[0];
		if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || result == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
			createSwapchain();
			return;
		} else if (result != VK12.VK_SUCCESS) {
			throw new RuntimeException("Failed to present to Vulkan Swapchain");
		}

		this.graphicsQueue.waitIdle();

		this.currentFrame = (this.currentFrame + 1) % Vulkan.MAX_FRAMES_IN_FLIGHT;
	}

	public void testVulkan(Window window) {
		var vertexShaderModule   = new VulkanShaderModule(this.device);
		var fragmentShaderModule = new VulkanShaderModule(this.device);

		{
			var vertexShader = """
					#version 450

					layout(location = 0) in vec4 inPosition;
					layout(location = 1) in vec2 inUV;

					layout(location = 0) out vec2 outUV;

					layout(set = 0, binding = 0) uniform UniformBufferObject {
						mat4 model;
						mat4 projView;
					} ubo;

					void main() {
						vec4 worldPosition = /*ubo.model **/ inPosition;
					    gl_Position = /*ubo.projView **/ worldPosition;
					    outUV = inUV;
					}
									""";

			var fragmentShader = """
					#version 450

					layout(location = 0) in vec2 inUV;

					layout(location = 0) out vec4 outColor;

					layout(set = 0, binding = 1) uniform sampler2D textureSampler;

					void main() {
					    outColor = texture(textureSampler, inUV);
					}
									""";

			long shadercCompiler          = Shaderc.shaderc_compiler_initialize();
			long shadercAdditionalOptions = Shaderc.shaderc_compile_options_initialize();

			long vertexShaderResult = Shaderc.shaderc_compile_into_spv(shadercCompiler, vertexShader, Shaderc.shaderc_glsl_vertex_shader,
					"shader.vert", "main", shadercAdditionalOptions);
			if (Shaderc.shaderc_result_get_compilation_status(vertexShaderResult) != Shaderc.shaderc_compilation_status_success) {
				Reference.LOGGER.error(Shaderc.shaderc_result_get_error_message(vertexShaderResult));
				throw new RuntimeException("Failed to compile vertex shader");
			}

			long fragmentShaderResult = Shaderc.shaderc_compile_into_spv(shadercCompiler, fragmentShader, Shaderc.shaderc_glsl_fragment_shader,
					"shader.frag", "main", shadercAdditionalOptions);
			if (Shaderc.shaderc_result_get_compilation_status(fragmentShaderResult) != Shaderc.shaderc_compilation_status_success) {
				Reference.LOGGER.error(Shaderc.shaderc_result_get_error_message(fragmentShaderResult));
				throw new RuntimeException("Failed to compile fragment shader");
			}

			var pVertexShaderCode = Shaderc.shaderc_result_get_bytes(vertexShaderResult);
			var vertexShaderCode  = new byte[pVertexShaderCode.capacity()];
			pVertexShaderCode.get(vertexShaderCode);
			vertexShaderModule.code = vertexShaderCode;

			var pFragmentShaderCode = Shaderc.shaderc_result_get_bytes(fragmentShaderResult);

			var fragmentShaderCode = new byte[pFragmentShaderCode.capacity()];
			pFragmentShaderCode.get(fragmentShaderCode);
			fragmentShaderModule.code = fragmentShaderCode;

			Shaderc.shaderc_result_release(vertexShaderResult);
			Shaderc.shaderc_result_release(fragmentShaderResult);

			Shaderc.shaderc_compile_options_release(shadercAdditionalOptions);
			Shaderc.shaderc_compiler_release(shadercCompiler);
		}

		if (!vertexShaderModule.create()) throw new RuntimeException("Failed to create Vulkan ShaderModule");
		if (!fragmentShaderModule.create()) throw new RuntimeException("Failed to create Vulkan ShaderModule");

		var descriptorSetLayout    = new VulkanDescriptorSetLayout(this.device);
		var graphicsPipelineLayout = new VulkanPipelineLayout(this.device);
		var graphicsPipeline       = new VulkanGraphicsPipeline(this.device, graphicsPipelineLayout, this.renderPass);
		var descriptorPool         = new VulkanDescriptorPool(this.device);

		descriptorSetLayout.bindings
				.add(new VulkanDescriptorSetLayout.Binding(0, VK12.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1, VK12.VK_SHADER_STAGE_VERTEX_BIT));
		descriptorSetLayout.bindings
				.add(new VulkanDescriptorSetLayout.Binding(1, VK12.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, VK12.VK_SHADER_STAGE_FRAGMENT_BIT));

		graphicsPipelineLayout.setLayouts.add(descriptorSetLayout);

		graphicsPipeline.vertexInputState.vertexBindings
				.add(new VulkanGraphicsPipeline.VertexInputState.VertexBinding(0, 24, VK12.VK_VERTEX_INPUT_RATE_VERTEX));
		graphicsPipeline.vertexInputState.vertexAttributes
				.add(new VulkanGraphicsPipeline.VertexInputState.VertexAttribute(0, 0, VK12.VK_FORMAT_R32G32B32A32_SFLOAT, 0));
		graphicsPipeline.vertexInputState.vertexAttributes
				.add(new VulkanGraphicsPipeline.VertexInputState.VertexAttribute(1, 0, VK12.VK_FORMAT_R32G32_SFLOAT, 16));

		graphicsPipeline.shaderStages.add(new VulkanGraphicsPipeline.VulkanShaderStage(EShaderStage.VERTEX, vertexShaderModule, "main"));
		graphicsPipeline.shaderStages.add(new VulkanGraphicsPipeline.VulkanShaderStage(EShaderStage.FRAGMENT, fragmentShaderModule, "main"));
		graphicsPipeline.viewportState.viewports.add(new VulkanGraphicsPipeline.ViewportState.Viewport(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f));
		graphicsPipeline.viewportState.scissors.add(new VulkanGraphicsPipeline.ViewportState.Scissor(0, 0, 0, 0));
		graphicsPipeline.colorBlendState.attachments.add(new VulkanGraphicsPipeline.ColorBlendState.Attachment());
		graphicsPipeline.dynamicState.states
				.addAll(Lists.newArrayList(VK12.VK_DYNAMIC_STATE_VIEWPORT, VK12.VK_DYNAMIC_STATE_SCISSOR, VK12.VK_DYNAMIC_STATE_LINE_WIDTH));

		descriptorPool.maxSets = Vulkan.MAX_FRAMES_IN_FLIGHT;
		descriptorPool.poolSizes.add(new PoolSize(VK12.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, Vulkan.MAX_FRAMES_IN_FLIGHT));
		descriptorPool.poolSizes.add(new PoolSize(VK12.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, Vulkan.MAX_FRAMES_IN_FLIGHT));

		if (!descriptorSetLayout.create()) throw new RuntimeException("Failed to create Vulkan DescriptorSetLayout");
		if (!graphicsPipelineLayout.create()) throw new RuntimeException("Failed to create Vulkan PipelineLayout");
		if (!graphicsPipeline.create()) throw new RuntimeException("Failed to create Vulkan GraphicsPipeline");
		if (!descriptorPool.create()) throw new RuntimeException("Failed to create Vulkan DescriptorPool");

		var meshBuffer = new VulkanBuffer(this.memoryAllocator);
		meshBuffer.size   = 240;
		meshBuffer.usage |= VK12.VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK12.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
		if (!meshBuffer.create()) throw new RuntimeException("Failed to create Vulkan Buffer");

		var textureImage = new VulkanImage(this.memoryAllocator);
		textureImage.extent.width   = 2;
		textureImage.extent.height  = 2;
		textureImage.usage         |= VK12.VK_IMAGE_USAGE_SAMPLED_BIT;
		if (!textureImage.create()) throw new RuntimeException("Failed to create Vulkan Image");

		var textureImageView = new VulkanImageView(this.device, textureImage);
		if (!textureImageView.create()) throw new RuntimeException("Failed to create Vulkan ImageView");

		var textureImageSampler = new VulkanSampler(this.device);
		if (!textureImageSampler.create()) throw new RuntimeException("Failed to create Vulkan Sampler");

		{
			var stagingBuffer = new VulkanBuffer(this.memoryAllocator);
			stagingBuffer.size        = meshBuffer.size + (2 * 2 * 4);
			stagingBuffer.usage       = VK12.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
			stagingBuffer.memoryUsage = Vma.VMA_MEMORY_USAGE_CPU_ONLY;
			if (!stagingBuffer.create()) throw new RuntimeException("Failed to create Vulkan Buffer");

			var mappedMemory = stagingBuffer.mapMemory();

			float[] vertices = new float[] {
					-0.5f, -0.5f, 0.5f, 1.0f, 1.0f, 1.0f,
					0.5f, -0.5f, 0.5f, 1.0f, 0.0f, 1.0f,
					0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.0f,
					-0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 0.0f,
					
					-0.3f, -0.3f, 0.0f, 1.0f, 1.0f, 1.0f,
					0.3f, -0.3f, 0.0f, 1.0f, 0.0f, 1.0f,
					0.3f, 0.3f, 0.0f, 1.0f, 0.0f, 0.0f,
					-0.3f, 0.3f, 0.0f, 1.0f, 1.0f, 0.0f };
			int[]   indices  = new int[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4 };
			byte[]  pixels   = new byte[] { -1, 0, 0, -1, 0, -1, 0, -1, 0, -1, 0, -1, -1, 0, 0, -1 };
			mappedMemory.asFloatBuffer().put(0, vertices);
			mappedMemory.asIntBuffer().put(48, indices);
			mappedMemory.put((int) Math.min(meshBuffer.size, Integer.MAX_VALUE), pixels);

			stagingBuffer.unmapMemory();

			var currentCommandPool = getCommandPool(this.currentFrame);
			currentCommandPool.reset();
			var currentCommandBuffer = currentCommandPool.getCommandBuffer(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 0);
			if (currentCommandBuffer.begin()) {
				currentCommandBuffer.cmdCopyBuffer(stagingBuffer, meshBuffer, new VulkanBufferCopy[] { new VulkanBufferCopy(0, 0, meshBuffer.size) });

				int srcStageMask  = VK12.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				int dstStageMask  = VK12.VK_PIPELINE_STAGE_TRANSFER_BIT;
				int srcAccessMask = 0;
				int dstAccessMask = VK12.VK_ACCESS_TRANSFER_WRITE_BIT;

				currentCommandBuffer.cmdPipelineBarrier(srcStageMask, dstStageMask, 0, null, null,
						new VulkanImageMemoryBarrier[] { new VulkanImageMemoryBarrier(srcAccessMask, dstAccessMask, VK12.VK_QUEUE_FAMILY_IGNORED,
								VK12.VK_QUEUE_FAMILY_IGNORED, VK12.VK_IMAGE_LAYOUT_UNDEFINED, VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, textureImage,
								new VulkanImageSubresourceRange(VK12.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)) });

				currentCommandBuffer.cmdCopyBufferToIamge(stagingBuffer, textureImage, VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
						new VulkanBufferImageCopy[] { new VulkanBufferImageCopy(meshBuffer.size, 0, 0,
								new VulkanImageSubresourceLayers(VK12.VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1), new VulkanOffset3D(0, 0, 0),
								textureImage.extent) });

				srcStageMask  = VK12.VK_PIPELINE_STAGE_TRANSFER_BIT;
				dstStageMask  = VK12.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
				srcAccessMask = VK12.VK_ACCESS_TRANSFER_WRITE_BIT;
				dstAccessMask = VK12.VK_ACCESS_SHADER_READ_BIT;

				currentCommandBuffer
						.cmdPipelineBarrier(srcStageMask, dstStageMask, 0, null, null,
								new VulkanImageMemoryBarrier[] { new VulkanImageMemoryBarrier(srcAccessMask, dstAccessMask,
										VK12.VK_QUEUE_FAMILY_IGNORED, VK12.VK_QUEUE_FAMILY_IGNORED, VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
										VK12.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, textureImage,
										new VulkanImageSubresourceRange(VK12.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)) });

				currentCommandBuffer.end();

				this.graphicsQueue.submitCommandBuffers(new VulkanCommandBuffer[] { currentCommandBuffer }, null, null, null, null);
				this.graphicsQueue.waitIdle();
			}

			stagingBuffer.destroy();
		}

		var uniformBuffer = new VulkanBuffer(this.memoryAllocator);
		uniformBuffer.size         = 128 * Vulkan.MAX_FRAMES_IN_FLIGHT;
		uniformBuffer.usage       |= VK12.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
		uniformBuffer.memoryUsage  = Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
		if (!uniformBuffer.create()) throw new RuntimeException("Failed to create Vulkan Buffer");

		var descriptorSetLayouts = new VulkanDescriptorSetLayout[Vulkan.MAX_FRAMES_IN_FLIGHT];
		for (int i = 0; i < Vulkan.MAX_FRAMES_IN_FLIGHT; ++i) descriptorSetLayouts[i] = descriptorSetLayout;
		var descriptorSets = descriptorPool.allocateSets(descriptorSetLayouts);

		var writeDescriptorSets = new VulkanDescriptorSet.WriteDescriptorSet[Vulkan.MAX_FRAMES_IN_FLIGHT * 2];
		for (int i = 0; i < Vulkan.MAX_FRAMES_IN_FLIGHT; ++i)
			writeDescriptorSets[i] = new VulkanDescriptorSet.WriteDescriptorSet(descriptorSets.get(i), 0, 0, VK12.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
					1, null, new VulkanDescriptorSet.WriteDescriptorSet.BufferInfo(uniformBuffer, 128 * i, 128), null);
		for (int i = Vulkan.MAX_FRAMES_IN_FLIGHT; i < Vulkan.MAX_FRAMES_IN_FLIGHT * 2; ++i)
			writeDescriptorSets[i] = new VulkanDescriptorSet.WriteDescriptorSet(descriptorSets.get(i - Vulkan.MAX_FRAMES_IN_FLIGHT), 1, 0,
					VK12.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, new VulkanDescriptorSet.WriteDescriptorSet.ImageInfo(textureImageSampler,
							textureImageView, VK12.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
					null, null);

		VulkanDescriptorSet.updateDescriptorSets(this.device, writeDescriptorSets, null);

		while (!window.shouldClose()) {
			Matrix4f modelMatrix = new Matrix4f(new Quaternion(new Vec3f(0.0f, 0.0f, 1.0f), System.nanoTime() / 1e9f * 180, true));
			Matrix4f viewMatrix  = new Matrix4f();
			try (var stack = MemoryStack.stackPush()) {
				var buf = stack.mallocFloat(16);
				buf.put(0, new float[] { 0.5773502692f, -0.5773502692f, -0.3333333333333f, 2.0f, 0.5773502692f, 0.5773502692f, -0.333333333333f, 2.0f,
						0.5773502692f, 0.0f, 0.666666666666666f, 2.0f, 0.0f, 0.0f, 0.0f, 1.0f });
				viewMatrix.read(buf, false);
			}
			Matrix4f projMatrix     = Matrix4f.viewboxMatrix(70.0f, (float) this.swapchain.width / this.swapchain.height, 0.01f, 1000.0f);
			Matrix4f projViewMatrix = projMatrix.copy();
			projViewMatrix.multiply(viewMatrix);

			var buf = MemoryUtil.memAllocFloat(48);
			{
				var buff = buf.slice(0, 16);
				modelMatrix.write(buff, false);
			}
			{
				var buff = buf.slice(16, 16);
				projViewMatrix.write(buff, false);
			}

			var mappedUniformMemory = uniformBuffer.mapMemory();
			mappedUniformMemory.put(128 * this.currentFrame, MemoryUtil.memByteBuffer(buf), 0, 128);
			uniformBuffer.unmapMemory();

			MemoryUtil.memFree(buf);

			GLFW.glfwPollEvents();
			beginFrame();

			var currentCommandPool   = getCommandPool(this.currentFrame);
			var currentCommandBuffer = currentCommandPool.getCommandBuffer(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 0);
			if (currentCommandBuffer.begin()) {
				currentCommandBuffer.cmdBeginRenderPass(this.renderPass, this.framebuffers.get(this.currentImage), 0, 0, this.swapchain.width,
						this.swapchain.height,
						new VulkanClearValue[] { new VulkanClearColorFloat(0.1f, 0.1f, 0.1f, 1.0f), new VulkanClearDepthStencil(1.0f, 0) });

				currentCommandBuffer.cmdSetViewports(0,
						new VulkanViewport[] { new VulkanViewport(0.0f, 0.0f, this.swapchain.width, this.swapchain.height) });
				currentCommandBuffer.cmdSetScissors(0, new VulkanScissor[] { new VulkanScissor(0, 0, this.swapchain.width, this.swapchain.height) });
				currentCommandBuffer.cmdSetLineWidth(1.0f);
				currentCommandBuffer.cmdBindPipeline(graphicsPipeline);
				currentCommandBuffer.cmdBindVertexBuffers(0, new VulkanBuffer[] { meshBuffer }, new long[] { 0 });
				currentCommandBuffer.cmdBindIndexBuffer(meshBuffer, 192, VK12.VK_INDEX_TYPE_UINT32);
				currentCommandBuffer.cmdBindDescriptorSets(VK12.VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineLayout, 0,
						new VulkanDescriptorSet[] { descriptorSets.get(this.currentFrame) }, null);
				currentCommandBuffer.cmdDrawIndexed(12, 1, 0, 0, 0);

				currentCommandBuffer.cmdEndRenderPass();
				currentCommandBuffer.end();
			}

			endFrame();
		}

		destroy();
		System.exit(0);
	}

	public VulkanInstance getInstance() {
		return this.instance;
	}

	public VulkanDebug getDebug() {
		return this.debug;
	}

	public VulkanSurface getSurface() {
		return this.surface;
	}

	public VulkanPhysicalDevice getPhysicalDevice() {
		return this.physicalDevice;
	}

	public VulkanDevice getDevice() {
		return this.device;
	}

	public VulkanSwapchain getSwapchain() {
		return this.swapchain;
	}

	public VulkanRenderPass getRenderPass() {
		return this.renderPass;
	}

	public VulkanFramebuffer getFramebuffer(int index) {
		return index >= 0 && index < this.framebuffers.size() ? this.framebuffers.get(index) : null;
	}

	public VulkanCommandPool getCommandPool(int index) {
		return index >= 0 && index < this.commandPools.size() ? this.commandPools.get(index) : null;
	}

	public int getNumFrames() {
		return MAX_FRAMES_IN_FLIGHT;
	}

	public VulkanSemaphore getImageAvailableSemaphore(int index) {
		return index >= 0 && index < this.imageAvailableSemaphores.size() ? this.imageAvailableSemaphores.get(index) : null;
	}

	public VulkanSemaphore getRenderFinishedSemaphore(int index) {
		return index >= 0 && index < this.renderFinishedSemaphores.size() ? this.renderFinishedSemaphores.get(index) : null;
	}

	private static long scorePhysicalDevice(VkPhysicalDevice physicalDevice) {
		long score = 0;

		try (var stack = MemoryStack.stackPush()) {
			var deviceProperties = VkPhysicalDeviceProperties.mallocStack(stack);
			var deviceFeatures   = VkPhysicalDeviceFeatures.mallocStack(stack);

			VK12.vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
			VK12.vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

			switch (deviceProperties.deviceType()) {
			case VK12.VK_PHYSICAL_DEVICE_TYPE_CPU:
				score += 1;
				break;
			case VK12.VK_PHYSICAL_DEVICE_TYPE_OTHER:
				score += 2;
				break;
			case VK12.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
				score += 3;
				break;
			case VK12.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
				score += 4;
				break;
			case VK12.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
				score += 5;
				break;
			}
		}

		return score;
	}
}
