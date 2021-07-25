package marcasrealaccount.vulkan;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import com.google.common.collect.Lists;

import marcasrealaccount.vulkan.instance.VulkanDevice;
import marcasrealaccount.vulkan.instance.VulkanInstance;
import marcasrealaccount.vulkan.instance.VulkanPhysicalDevice;
import marcasrealaccount.vulkan.instance.VulkanSurface;
import marcasrealaccount.vulkan.instance.VulkanSwapchain;
import marcasrealaccount.vulkan.instance.command.VulkanCommandBuffer;
import marcasrealaccount.vulkan.instance.command.VulkanCommandPool;
import marcasrealaccount.vulkan.instance.debug.VulkanDebug;
import marcasrealaccount.vulkan.instance.image.VulkanFramebuffer;
import marcasrealaccount.vulkan.instance.pipeline.VulkanGraphicsPipeline;
import marcasrealaccount.vulkan.instance.pipeline.VulkanGraphicsPipeline.VulkanShaderStage.EShaderStage;
import marcasrealaccount.vulkan.instance.pipeline.VulkanPipelineLayout;
import marcasrealaccount.vulkan.instance.pipeline.VulkanRenderPass;
import marcasrealaccount.vulkan.instance.shader.VulkanShaderModule;
import marcasrealaccount.vulkan.instance.synchronize.VulkanFence;
import marcasrealaccount.vulkan.instance.synchronize.VulkanSemaphore;
import marcasrealaccount.vulkan.util.VulkanClearColorFloat;
import marcasrealaccount.vulkan.util.VulkanClearValue;
import marcasrealaccount.vulkan.util.VulkanScissor;
import marcasrealaccount.vulkan.util.VulkanViewport;
import net.minecraft.client.util.Window;

public class Vulkan {
	public static final Vulkan INSTANCE = new Vulkan();
	private static final int MAX_FRAMES_IN_FLIGHT = 2;

	private VulkanInstance instance = new VulkanInstance();
	private VulkanDebug debug = new VulkanDebug(this.instance);
	private VulkanSurface surface = new VulkanSurface(this.instance, null);
	private VulkanPhysicalDevice physicalDevice = null;
	private VulkanDevice device = null;

	private VulkanSwapchain swapchain = null;
	private VulkanRenderPass renderPass = null;
	private final ArrayList<VulkanFramebuffer> framebuffers = new ArrayList<>();

	private final ArrayList<VulkanCommandPool> commandPools = new ArrayList<>();
	private final ArrayList<VulkanSemaphore> imageAvailableSemaphores = new ArrayList<>();
	private final ArrayList<VulkanSemaphore> renderFinishedSemaphores = new ArrayList<>();
	private final ArrayList<VulkanFence> inFlightFences = new ArrayList<>();
	private final ArrayList<VulkanFence> imagesInFlight = new ArrayList<>();
	private int currentImage = 0;
	private int currentFrame = 0;

	public void setVSync(boolean vsync) {
		if (this.swapchain != null)
			this.swapchain.setVSync(vsync);
	}

	public void initVulkan(Window window, boolean vsync) {
		if (Reference.USE_VALIDATION_LAYERS)
			if (!this.instance.useLayer("VK_LAYER_KHRONOS_validation", 0))
				VulkanDebug.disable();
			else
				this.instance.useExtension(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME, 0);

		{
			var glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
			boolean failed = false;

			for (int i = 0; i < glfwExtensions.capacity(); ++i) {
				String extensionName = glfwExtensions.getStringUTF8(i);
				if (!this.instance.useExtension(extensionName, 0)) {
					failed = true;
					Reference.LOGGER.error("Missing Instance extension '" + extensionName + "'");
				}
			}

			if (failed)
				throw new RuntimeException("Missing Instance extension(s)");
		}

		if (!this.instance.create())
			throw new RuntimeException("Failed to create Vulkan Instance");

		if (VulkanDebug.isEnabled())
			this.debug.create();

		this.surface.setWindow(window);
		if (!this.surface.create())
			throw new RuntimeException("Failed to create Vulkan Surface");

		this.physicalDevice = VulkanPhysicalDevice.pickBestPhysicalDevice(this.instance, this.surface,
				Vulkan::scorePhysicalDevice);
		if (this.physicalDevice == null)
			throw new RuntimeException("Failed to find a suitable GPU");

		this.device = new VulkanDevice(this.physicalDevice);
		if (!this.device.useExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME, 0))
			throw new RuntimeException("Missing Vulkan KHRSwapchan extension");

		if (!this.device.create())
			throw new RuntimeException("Failed to create Vulkan Device");

		this.swapchain = new VulkanSwapchain(this.device, window, vsync);
		if (!this.swapchain.create())
			throw new RuntimeException("Failed to create Vulkan Swapchain");

		this.renderPass = new VulkanRenderPass(this.device);
		{
			var attachment = new VulkanRenderPass.Attachment();
			attachment.format = this.swapchain.getFormat().format;
			this.renderPass.attachments.add(attachment);

			var subpass = new VulkanRenderPass.Subpass();
			var attachmentRef = new VulkanRenderPass.Subpass.AttachmentRef();
			attachmentRef.attachment = 0;
			attachmentRef.layout = VK12.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
			subpass.colorAttachmentRefs.add(attachmentRef);
			this.renderPass.subpasses.add(subpass);

			var dependency = new VulkanRenderPass.Dependency();
			dependency.srcSubpass = VK12.VK_SUBPASS_EXTERNAL;
			dependency.dstSubpass = 0;
			dependency.srcStageMask = VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
			dependency.srcAccessMask = 0;
			dependency.dstStageMask = VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
			dependency.dstAccessMask = VK12.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
			this.renderPass.dependencies.add(dependency);
		}
		if (!this.renderPass.create())
			throw new RuntimeException("Failed to create Vulkan RenderPass");

		this.framebuffers.ensureCapacity(this.swapchain.getNumImages());
		for (int i = 0; i < this.swapchain.getNumImages(); ++i) {
			var framebuffer = new VulkanFramebuffer(this.device, this.renderPass);
			framebuffer.attachments.add(this.swapchain.getImageView(i));
			framebuffer.width = this.swapchain.getExtent().width;
			framebuffer.height = this.swapchain.getExtent().height;
			if (!framebuffer.create())
				throw new RuntimeException("Failed to create Vulkan Framebuffers");
			this.framebuffers.add(framebuffer);
		}

		this.commandPools.ensureCapacity(MAX_FRAMES_IN_FLIGHT);
		this.imageAvailableSemaphores.ensureCapacity(MAX_FRAMES_IN_FLIGHT);
		this.renderFinishedSemaphores.ensureCapacity(MAX_FRAMES_IN_FLIGHT);
		this.inFlightFences.ensureCapacity(MAX_FRAMES_IN_FLIGHT);
		for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
			VulkanCommandPool commandPool = new VulkanCommandPool(this.device);
			if (!commandPool.create())
				throw new RuntimeException("Failed to create Vulkan Command Pool");
			commandPool.allocateBuffers(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 1);
			this.commandPools.add(commandPool);

			VulkanSemaphore ias = new VulkanSemaphore(this.device);
			VulkanSemaphore rfs = new VulkanSemaphore(this.device);
			VulkanFence iff = new VulkanFence(this.device);
			iff.signaled = true;
			if (!ias.create())
				throw new RuntimeException("Failed to create Vulkan Semaphore");
			if (!rfs.create())
				throw new RuntimeException("Failed to create Vulkan Semaphore");
			if (!iff.create())
				throw new RuntimeException("Failed to create Vulkan Fence");

			this.imageAvailableSemaphores.add(ias);
			this.renderFinishedSemaphores.add(rfs);
			this.inFlightFences.add(iff);
		}

		this.imagesInFlight.ensureCapacity(this.swapchain.getNumImages());
		for (int i = 0; i < this.swapchain.getNumImages(); ++i)
			this.imagesInFlight.add(null);
	}

	public void close() {
		VK12.vkDeviceWaitIdle(this.device.getHandle());

		this.instance.close();
	}

	public void beginFrame() {
		try (var stack = MemoryStack.stackPush()) {
			var imageIndex = stack.mallocInt(1);

			VK12.vkWaitForFences(this.device.getHandle(), this.inFlightFences.get(this.currentFrame).getHandle(), true,
					-1);
			VK12.vkResetFences(this.device.getHandle(), this.inFlightFences.get(this.currentFrame).getHandle());

			KHRSwapchain.vkAcquireNextImageKHR(this.device.getHandle(), this.swapchain.getHandle(), -1,
					this.imageAvailableSemaphores.get(this.currentFrame).getHandle(), 0, imageIndex);
			this.currentImage = imageIndex.get(0);

			if (this.imagesInFlight.get(this.currentImage) != null)
				VK12.vkWaitForFences(this.device.getHandle(), this.imagesInFlight.get(this.currentImage).getHandle(),
						true, -1);
			this.imagesInFlight.set(this.currentImage, this.inFlightFences.get(this.currentFrame));

			this.commandPools.get(this.currentFrame).reset();
			this.inFlightFences.get(this.currentFrame).reset();
		}
	}

	public void endFrame() {
		var waitSemaphores = new VulkanSemaphore[] { this.imageAvailableSemaphores.get(this.currentFrame) };
		var signalSemaphores = new VulkanSemaphore[] { this.renderFinishedSemaphores.get(this.currentFrame) };

		this.device.getGraphicsQueue().submitCommandBuffers(
				this.commandPools.get(this.currentFrame).getCommandBuffers().toArray(new VulkanCommandBuffer[0]),
				waitSemaphores, signalSemaphores, new int[] { VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT },
				this.inFlightFences.get(this.currentFrame));

		this.device.getPresentQueue().present(new VulkanSwapchain[] { this.swapchain }, new int[] { this.currentImage },
				signalSemaphores);
		this.device.getPresentQueue().waitIdle();

		this.currentFrame = (this.currentFrame + 1) % Vulkan.MAX_FRAMES_IN_FLIGHT;
	}

	public void testVulkan(Window window) {
		var vertexShader = """
				#version 450

				layout(location = 0) out vec3 fragColor;

				vec2 positions[3] = vec2[](
				    vec2(0.0, -0.5),
				    vec2(0.5, 0.5),
				    vec2(-0.5, 0.5)
				);

				vec3 colors[3] = vec3[](
				    vec3(1.0, 0.0, 0.0),
				    vec3(0.0, 1.0, 0.0),
				    vec3(0.0, 0.0, 1.0)
				);

				void main() {
				    gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
				    fragColor = colors[gl_VertexIndex];
				}
								""";

		var fragmentShader = """
				#version 450

				layout(location = 0) in vec3 fragColor;

				layout(location = 0) out vec4 outColor;

				void main() {
				    outColor = vec4(fragColor, 1.0);
				}
								""";

		long shadercCompiler = Shaderc.shaderc_compiler_initialize();
		long shadercAdditionalOptions = Shaderc.shaderc_compile_options_initialize();

		long vertexShaderResult = Shaderc.shaderc_compile_into_spv(shadercCompiler, vertexShader,
				Shaderc.shaderc_glsl_vertex_shader, "shader.vert", "main", shadercAdditionalOptions);
		if (Shaderc.shaderc_result_get_compilation_status(
				vertexShaderResult) != Shaderc.shaderc_compilation_status_success) {
			Reference.LOGGER.error(Shaderc.shaderc_result_get_error_message(vertexShaderResult));
			throw new RuntimeException("Failed to compile vertex shader");
		}

		long fragmentShaderResult = Shaderc.shaderc_compile_into_spv(shadercCompiler, fragmentShader,
				Shaderc.shaderc_glsl_fragment_shader, "shader.frag", "main", shadercAdditionalOptions);
		if (Shaderc.shaderc_result_get_compilation_status(
				fragmentShaderResult) != Shaderc.shaderc_compilation_status_success) {
			Reference.LOGGER.error(Shaderc.shaderc_result_get_error_message(fragmentShaderResult));
			throw new RuntimeException("Failed to compile fragment shader");
		}

		var vertexShaderModule = new VulkanShaderModule(this.device);
		var fragmentShaderModule = new VulkanShaderModule(this.device);

		var pVertexShaderCode = Shaderc.shaderc_result_get_bytes(vertexShaderResult);
		byte[] vertexShaderCode = new byte[pVertexShaderCode.capacity()];
		pVertexShaderCode.get(vertexShaderCode);
		vertexShaderModule.code = vertexShaderCode;

		var pFragmentShaderCode = Shaderc.shaderc_result_get_bytes(fragmentShaderResult);
		byte[] fragmentShaderCode = new byte[pFragmentShaderCode.capacity()];
		pFragmentShaderCode.get(fragmentShaderCode);
		fragmentShaderModule.code = fragmentShaderCode;

		Shaderc.shaderc_result_release(vertexShaderResult);
		Shaderc.shaderc_result_release(fragmentShaderResult);

		Shaderc.shaderc_compile_options_release(shadercAdditionalOptions);
		Shaderc.shaderc_compiler_release(shadercCompiler);

		if (!vertexShaderModule.create())
			throw new RuntimeException("Failed to create Vulkan ShaderModule");
		if (!fragmentShaderModule.create())
			throw new RuntimeException("Failed to create Vulkan ShaderModule");

		var graphicsPipelineLayout = new VulkanPipelineLayout(this.device);
		var graphicsPipeline = new VulkanGraphicsPipeline(this.device, graphicsPipelineLayout, this.renderPass);
		graphicsPipeline.shaderStages
				.add(new VulkanGraphicsPipeline.VulkanShaderStage(EShaderStage.VERTEX, vertexShaderModule, "main"));
		graphicsPipeline.shaderStages
				.add(new VulkanGraphicsPipeline.VulkanShaderStage(EShaderStage.FRAGMENT, fragmentShaderModule, "main"));
		graphicsPipeline.viewportState.viewports
				.add(new VulkanGraphicsPipeline.ViewportState.Viewport(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f));
		graphicsPipeline.viewportState.scissors.add(new VulkanGraphicsPipeline.ViewportState.Scissor(0, 0, 0, 0));
		graphicsPipeline.colorBlendState.attachments.add(new VulkanGraphicsPipeline.ColorBlendState.Attachment());
		graphicsPipeline.dynamicState.states.addAll(Lists.newArrayList(VK12.VK_DYNAMIC_STATE_VIEWPORT,
				VK12.VK_DYNAMIC_STATE_SCISSOR, VK12.VK_DYNAMIC_STATE_LINE_WIDTH));
		if (!graphicsPipelineLayout.create())
			throw new RuntimeException("Failed to create Vulkan PipelineLayout");
		if (!graphicsPipeline.create())
			throw new RuntimeException("Failed to create Vulkan GraphicsPipeline");

		while (!window.shouldClose()) {
			var currentCommandPool = getCommandPool(this.currentFrame);
			var currentCommandBuffer = currentCommandPool.getCommandBuffer(VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY, 0);
			if (currentCommandBuffer.begin()) {
				currentCommandBuffer.cmdBeginRenderPass(this.renderPass, this.framebuffers.get(this.currentImage), 0, 0,
						this.swapchain.getExtent().width, this.swapchain.getExtent().height,
						new VulkanClearValue[] { new VulkanClearColorFloat(0.1f, 0.1f, 0.1f, 1.0f) });

				currentCommandBuffer.cmdSetViewports(0, new VulkanViewport[] { new VulkanViewport(0.0f, 0.0f,
						this.swapchain.getExtent().width, this.swapchain.getExtent().height) });
				currentCommandBuffer.cmdSetScissors(0, new VulkanScissor[] {
						new VulkanScissor(0, 0, this.swapchain.getExtent().width, this.swapchain.getExtent().height) });
				currentCommandBuffer.cmdSetLineWidth(1.0f);
				currentCommandBuffer.cmdBindPipeline(graphicsPipeline);
				currentCommandBuffer.cmdDraw(3, 1, 0, 0);

				currentCommandBuffer.cmdEndRenderPass();
				currentCommandBuffer.end();
			}

			GLFW.glfwPollEvents();
			endFrame();
			beginFrame();
		}

		close();
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
		return index >= 0 && index < this.imageAvailableSemaphores.size() ? this.imageAvailableSemaphores.get(index)
				: null;
	}

	public VulkanSemaphore getRenderFinishedSemaphore(int index) {
		return index >= 0 && index < this.renderFinishedSemaphores.size() ? this.renderFinishedSemaphores.get(index)
				: null;
	}

	private static long scorePhysicalDevice(VulkanPhysicalDevice physicalDevice) {
		long score = 0;

		try (var stack = MemoryStack.stackPush()) {
			var deviceProperties = VkPhysicalDeviceProperties.mallocStack(stack);
			var deviceFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);

			VK12.vkGetPhysicalDeviceProperties(physicalDevice.getHandle(), deviceProperties);
			VK12.vkGetPhysicalDeviceFeatures(physicalDevice.getHandle(), deviceFeatures);

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
