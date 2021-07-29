package marcasrealaccount.vulkan.shader;

import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRRayTracingPipeline;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineTessellationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkStencilOpState;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;

import marcasrealaccount.vulkan.device.VulkanDevice;
import marcasrealaccount.vulkan.pipeline.VulkanPipeline;
import marcasrealaccount.vulkan.pipeline.VulkanPipelineLayout;
import marcasrealaccount.vulkan.pipeline.VulkanRenderPass;

public class VulkanGraphicsPipeline extends VulkanPipeline {
	public final VulkanDevice         device;
	public final VulkanPipelineLayout pipelineLayout;
	public final VulkanRenderPass     renderPass;

	public final ArrayList<VulkanShaderStage> shaderStages       = new ArrayList<>();
	public final VertexInputState             vertexInputState   = new VertexInputState();
	public final InputAssemblyState           inputAssemblyState = new InputAssemblyState();
	public final TesselationState             tesselationState   = new TesselationState();
	public final ViewportState                viewportState      = new ViewportState();
	public final RasterizationState           rasterizationState = new RasterizationState();
	public final MultisampleState             multisampleState   = new MultisampleState();
	public final DepthStencilState            depthStencilState  = new DepthStencilState();
	public final ColorBlendState              colorBlendState    = new ColorBlendState();
	public final DynamicState                 dynamicState       = new DynamicState();
	public int                                subpass            = 0;

	public VulkanGraphicsPipeline(VulkanDevice device, VulkanPipelineLayout pipelineLayout, VulkanRenderPass renderPass) {
		super(0L);
		this.device         = device;
		this.pipelineLayout = pipelineLayout;
		this.renderPass     = renderPass;

		this.device.addChild(this);
		this.pipelineLayout.addChild(this);
		this.renderPass.addChild(this);
	}

	@Override
	protected void createAbstract() {
		try (var stack = MemoryStack.stackPush()) {
			var       createInfos         = VkGraphicsPipelineCreateInfo.mallocStack(1, stack);
			var       vertexInputState    = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
			var       inputAssemblyState  = VkPipelineInputAssemblyStateCreateInfo.mallocStack(stack);
			var       tesselationState    = VkPipelineTessellationStateCreateInfo.mallocStack(stack);
			var       viewportState       = VkPipelineViewportStateCreateInfo.mallocStack(stack);
			var       rasterizationState  = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
			var       multisampleState    = VkPipelineMultisampleStateCreateInfo.mallocStack(stack);
			var       depthStencilState   = VkPipelineDepthStencilStateCreateInfo.mallocStack(stack);
			var       depthStencilFront   = VkStencilOpState.mallocStack(stack);
			var       depthStencilBack    = VkStencilOpState.mallocStack(stack);
			var       colorBlendState     = VkPipelineColorBlendStateCreateInfo.mallocStack(stack);
			var       colorBlendConstants = stack.mallocFloat(4);
			var       dynamicState        = VkPipelineDynamicStateCreateInfo.mallocStack(stack);
			var       pGraphicsPipelines  = stack.mallocLong(1);
			IntBuffer pSampleMask         = null;

			var stages = VkPipelineShaderStageCreateInfo.malloc(this.shaderStages.size());
			for (int i = 0; i < this.shaderStages.size(); ++i) {
				var shaderStage = this.shaderStages.get(i);
				var stage       = stages.get(i);

				var pEntrypoint = MemoryUtil.memUTF8(shaderStage.entrypoint);

				stage.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO, 0, 0, shaderStage.stage.getValue(),
						shaderStage.module.getHandle(), pEntrypoint, null);
			}

			var pVertexBindings = VkVertexInputBindingDescription.malloc(this.vertexInputState.vertexBindings.size());
			for (int i = 0; i < this.vertexInputState.vertexBindings.size(); ++i) {
				var vertexBinding  = this.vertexInputState.vertexBindings.get(i);
				var pVertexBinding = pVertexBindings.get(i);
				pVertexBinding.set(vertexBinding.binding, vertexBinding.stride, vertexBinding.inputRate);
			}

			var pVertexAttributes = VkVertexInputAttributeDescription.malloc(this.vertexInputState.vertexAttributes.size());
			for (int i = 0; i < this.vertexInputState.vertexAttributes.size(); ++i) {
				var vertexAttribute  = this.vertexInputState.vertexAttributes.get(i);
				var pVertexAttribute = pVertexAttributes.get(i);
				pVertexAttribute.set(vertexAttribute.location, vertexAttribute.binding, vertexAttribute.format, vertexAttribute.offset);
			}

			vertexInputState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO, 0, 0, pVertexBindings, pVertexAttributes);

			inputAssemblyState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO, 0, 0,
					this.inputAssemblyState.topology.getValue(), this.inputAssemblyState.restartEnabled);

			tesselationState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO, 0, 0, this.tesselationState.patchControlPoints);

			var pViewports = VkViewport.malloc(this.viewportState.viewports.size());
			for (int i = 0; i < this.viewportState.viewports.size(); ++i) {
				var viewport  = this.viewportState.viewports.get(i);
				var pViewport = pViewports.get(i);
				pViewport.set(viewport.x, viewport.y, viewport.width, viewport.height, viewport.minDepth, viewport.maxDepth);
			}

			var pScissors = VkRect2D.malloc(this.viewportState.scissors.size());
			for (int i = 0; i < this.viewportState.scissors.size(); ++i) {
				var scissor  = this.viewportState.scissors.get(i);
				var pScissor = pScissors.get(i);
				pScissor.offset().set(scissor.x, scissor.y);
				pScissor.extent().set(scissor.width, scissor.height);
			}

			viewportState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO, 0, 0, pViewports.capacity(), pViewports,
					pScissors.capacity(), pScissors);

			rasterizationState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO, 0, 0, this.rasterizationState.depthClampEnable,
					this.rasterizationState.rasterizerDiscardEnable, this.rasterizationState.polygonMode.getValue(),
					this.rasterizationState.cullMode.getValue(), this.rasterizationState.frontFace.getValue(),
					this.rasterizationState.depthBiasEnable, this.rasterizationState.depthBiasConstantFactor, this.rasterizationState.depthBiasClamp,
					this.rasterizationState.depthBiasSlopeFactor, this.rasterizationState.lineWidth);

			if (this.multisampleState.sampleMask != null) {
				pSampleMask = stack.mallocInt((this.multisampleState.rasterizationSamples + 31) / 32);
				pSampleMask.put(0, this.multisampleState.sampleMask, 0, Math.min(pSampleMask.capacity(), this.multisampleState.sampleMask.length));
			}

			multisampleState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO, 0, 0, this.multisampleState.rasterizationSamples,
					this.multisampleState.sampleShadingEnable, this.multisampleState.minSampleShading, pSampleMask,
					this.multisampleState.alphaToCoverageEnable, this.multisampleState.alphaToOneEnable);

			var depthStencilFrontState = this.depthStencilState.front;
			depthStencilFront.set(depthStencilFrontState.failOp, depthStencilFrontState.passOp, depthStencilFrontState.depthFailOp,
					depthStencilFrontState.compareOp, depthStencilFrontState.compareMask, depthStencilFrontState.writeMask,
					depthStencilFrontState.reference);

			var depthStencilBackState = this.depthStencilState.back;
			depthStencilBack.set(depthStencilBackState.failOp, depthStencilBackState.passOp, depthStencilBackState.depthFailOp,
					depthStencilBackState.compareOp, depthStencilBackState.compareMask, depthStencilBackState.writeMask,
					depthStencilBackState.reference);

			depthStencilState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO, 0, 0, this.depthStencilState.depthTestEnable,
					this.depthStencilState.depthWriteEnable, this.depthStencilState.depthCompareOp, this.depthStencilState.depthBoundsTestEnable,
					this.depthStencilState.stencilTestEnable, depthStencilFront, depthStencilBack, this.depthStencilState.minDepthBounds,
					this.depthStencilState.maxDepthBounds);

			var pColorBlendAttachments = VkPipelineColorBlendAttachmentState.malloc(this.colorBlendState.attachments.size());
			for (int i = 0; i < this.colorBlendState.attachments.size(); ++i) {
				var colorBlendAttachment  = this.colorBlendState.attachments.get(i);
				var pColorBlendAttachment = pColorBlendAttachments.get(i);
				pColorBlendAttachment.set(colorBlendAttachment.blendEnable, colorBlendAttachment.srcColorBlendFactor,
						colorBlendAttachment.dstColorBlendFactor, colorBlendAttachment.colorBlendOp, colorBlendAttachment.srcAlphaBlendFactor,
						colorBlendAttachment.dstAlphaBlendFactor, colorBlendAttachment.alphaBlendOp, colorBlendAttachment.colorWriteMask);
			}

			colorBlendConstants.put(0, this.colorBlendState.constants, 0, Math.min(4, this.colorBlendState.constants.length));

			colorBlendState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO, 0, 0, this.colorBlendState.logicOpEnable,
					this.colorBlendState.logicOp, pColorBlendAttachments, colorBlendConstants);

			var pDynamicStates = MemoryUtil.memAllocInt(this.dynamicState.states.size());
			for (int i = 0; i < this.dynamicState.states.size(); ++i) pDynamicStates.put(i, this.dynamicState.states.get(i));

			dynamicState.set(VK12.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO, 0, 0, pDynamicStates);

			var createInfo = createInfos.get(0);
			createInfo.set(VK12.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO, 0, 0, stages, vertexInputState, inputAssemblyState, tesselationState,
					viewportState, rasterizationState, multisampleState, depthStencilState, colorBlendState, dynamicState,
					this.pipelineLayout.getHandle(), this.renderPass.getHandle(), subpass, 0, 0);

			if (VK12.vkCreateGraphicsPipelines(this.device.getHandle(), 0, createInfos, null, pGraphicsPipelines) == VK12.VK_SUCCESS)
				this.handle = pGraphicsPipelines.get(0);

			pVertexBindings.free();
			pVertexAttributes.free();
			stages.free();
			pViewports.free();
			pScissors.free();
			pColorBlendAttachments.free();
			MemoryUtil.memFree(pDynamicStates);
		}
	}

	@Override
	protected void destroyAbstract() {
		VK12.vkDestroyPipeline(this.device.getHandle(), this.handle, null);
	}

	@Override
	protected void removeAbstract() {
		this.device.removeChild(this);
		this.pipelineLayout.removeChild(this);
		this.renderPass.removeChild(this);
	}

	@Override
	public int getBindPoint() {
		return VK12.VK_PIPELINE_BIND_POINT_GRAPHICS;
	}

	public static class VulkanShaderStage {
		public EShaderStage       stage;
		public VulkanShaderModule module;
		public String             entrypoint;

		public VulkanShaderStage(EShaderStage stage, VulkanShaderModule module, String entrypoint) {
			this.stage      = stage;
			this.module     = module;
			this.entrypoint = entrypoint;
		}

		public enum EShaderStage {
			VERTEX(VK12.VK_SHADER_STAGE_VERTEX_BIT), TESSELATION_CONTROL(VK12.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT),
			TESSELATION_EVALUATION(VK12.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT), GEOMETRY(VK12.VK_SHADER_STAGE_GEOMETRY_BIT),
			FRAGMENT(VK12.VK_SHADER_STAGE_FRAGMENT_BIT), COMPUTE(VK12.VK_SHADER_STAGE_COMPUTE_BIT),
			RT_RAYGEN(KHRRayTracingPipeline.VK_SHADER_STAGE_RAYGEN_BIT_KHR), RT_ANY_HIT(KHRRayTracingPipeline.VK_SHADER_STAGE_ANY_HIT_BIT_KHR),
			RT_CLOSEST_HIT(KHRRayTracingPipeline.VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR), RT_MISS(KHRRayTracingPipeline.VK_SHADER_STAGE_MISS_BIT_KHR),
			RT_INTERSECTION(KHRRayTracingPipeline.VK_SHADER_STAGE_INTERSECTION_BIT_KHR),
			RT_CALLABLE(KHRRayTracingPipeline.VK_SHADER_STAGE_CALLABLE_BIT_KHR);

			private int value;

			private EShaderStage(int value) {
				this.value = value;
			}

			public int getValue() {
				return this.value;
			}
		}
	}

	public static class VertexInputState {
		public final ArrayList<VertexBinding>   vertexBindings   = new ArrayList<>();
		public final ArrayList<VertexAttribute> vertexAttributes = new ArrayList<>();

		public static class VertexBinding {
			public int binding;
			public int stride;
			public int inputRate;

			public VertexBinding(int binding, int stride, int inputRate) {
				this.binding   = binding;
				this.stride    = stride;
				this.inputRate = inputRate;
			}
		}

		public static class VertexAttribute {
			public int location;
			public int binding;
			public int format;
			public int offset;

			public VertexAttribute(int location, int binding, int format, int offset) {
				this.location = location;
				this.binding  = binding;
				this.format   = format;
				this.offset   = offset;
			}
		}
	}

	public static class InputAssemblyState {
		public EPipelineTopology topology       = EPipelineTopology.TRIANGLES;
		public boolean           restartEnabled = false;

		public enum EPipelineTopology {
			POINTS(VK12.VK_PRIMITIVE_TOPOLOGY_POINT_LIST), LINES(VK12.VK_PRIMITIVE_TOPOLOGY_LINE_LIST),
			TRIANGLES(VK12.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

			private int value;

			private EPipelineTopology(int value) {
				this.value = value;
			}

			public int getValue() {
				return this.value;
			}
		}
	}

	public static class TesselationState {
		public int patchControlPoints = 1;
	}

	public static class ViewportState {
		public final ArrayList<Viewport> viewports = new ArrayList<>();
		public final ArrayList<Scissor>  scissors  = new ArrayList<>();

		public static class Viewport {
			public float x, y, width, height, minDepth, maxDepth;

			public Viewport(float x, float y, float width, float height, float minDepth, float maxDepth) {
				this.x        = x;
				this.y        = y;
				this.width    = width;
				this.height   = height;
				this.minDepth = minDepth;
				this.maxDepth = maxDepth;
			}
		}

		public static class Scissor {
			public int x, y, width, height;

			public Scissor(int x, int y, int width, int height) {
				this.x      = x;
				this.y      = y;
				this.width  = width;
				this.height = height;
			}
		}
	}

	public static class RasterizationState {
		public boolean      depthClampEnable        = false;
		public boolean      rasterizerDiscardEnable = false;
		public EPolygonMode polygonMode             = EPolygonMode.FILL;
		public ECullMode    cullMode                = ECullMode.BACK;
		public EFrontFace   frontFace               = EFrontFace.CLOCKWISE;
		public boolean      depthBiasEnable         = false;
		public float        depthBiasConstantFactor = 0.0f;
		public float        depthBiasClamp          = 0.0f;
		public float        depthBiasSlopeFactor    = 0.0f;
		public float        lineWidth               = 1.0f;

		public enum EPolygonMode {
			FILL(VK12.VK_POLYGON_MODE_FILL), LINE(VK12.VK_POLYGON_MODE_LINE), POINT(VK12.VK_POLYGON_MODE_POINT);

			private int value;

			private EPolygonMode(int value) {
				this.value = value;
			}

			public int getValue() {
				return this.value;
			}
		}

		public enum ECullMode {
			NONE(VK12.VK_CULL_MODE_NONE), FRONT(VK12.VK_CULL_MODE_FRONT_BIT), BACK(VK12.VK_CULL_MODE_BACK_BIT),
			FRONT_AND_BACK(VK12.VK_CULL_MODE_FRONT_AND_BACK);

			private int value;

			private ECullMode(int value) {
				this.value = value;
			}

			public int getValue() {
				return this.value;
			}
		}

		public enum EFrontFace {
			COUNTER_CLOCKWISE(VK12.VK_FRONT_FACE_COUNTER_CLOCKWISE), CLOCKWISE(VK12.VK_FRONT_FACE_CLOCKWISE);

			private int value;

			private EFrontFace(int value) {
				this.value = value;
			}

			public int getValue() {
				return this.value;
			}
		}
	}

	public static class MultisampleState {
		public int     rasterizationSamples  = 1;
		public boolean sampleShadingEnable   = false;
		public float   minSampleShading      = 1.0f;
		public int[]   sampleMask            = null;
		public boolean alphaToCoverageEnable = false;
		public boolean alphaToOneEnable      = false;
	}

	public static class DepthStencilState {
		public boolean              depthTestEnable       = true;
		public boolean              depthWriteEnable      = true;
		public int                  depthCompareOp        = VK12.VK_COMPARE_OP_LESS;
		public boolean              depthBoundsTestEnable = false;
		public boolean              stencilTestEnable     = true;
		public final StencilOpState front                 = new StencilOpState();
		public final StencilOpState back                  = new StencilOpState();
		public float                minDepthBounds        = 0.0f;
		public float                maxDepthBounds        = 1.0f;

		public static class StencilOpState {
			public int failOp      = VK12.VK_STENCIL_OP_KEEP;
			public int passOp      = VK12.VK_STENCIL_OP_REPLACE;
			public int depthFailOp = VK12.VK_STENCIL_OP_KEEP;
			public int compareOp   = VK12.VK_COMPARE_OP_LESS;
			public int compareMask = -1;
			public int writeMask   = -1;
			public int reference   = -1;
		}
	}

	public static class ColorBlendState {
		public boolean                     logicOpEnable = false;
		public int                         logicOp       = VK12.VK_LOGIC_OP_COPY;
		public final ArrayList<Attachment> attachments   = new ArrayList<>();
		public float[]                     constants     = new float[] { 0.0f, 0.0f, 0.0f, 0.0f };

		public static class Attachment {
			public boolean blendEnable         = false;
			public int     srcColorBlendFactor = VK12.VK_BLEND_FACTOR_ONE;
			public int     dstColorBlendFactor = VK12.VK_BLEND_FACTOR_ZERO;
			public int     colorBlendOp        = VK12.VK_BLEND_OP_ADD;
			public int     srcAlphaBlendFactor = VK12.VK_BLEND_FACTOR_ONE;
			public int     dstAlphaBlendFactor = VK12.VK_BLEND_FACTOR_ZERO;
			public int     alphaBlendOp        = VK12.VK_BLEND_OP_ADD;
			public int     colorWriteMask      = VK12.VK_COLOR_COMPONENT_R_BIT | VK12.VK_COLOR_COMPONENT_G_BIT | VK12.VK_COLOR_COMPONENT_B_BIT
					| VK12.VK_COLOR_COMPONENT_A_BIT;
		}
	}

	public static class DynamicState {
		public final ArrayList<Integer> states = new ArrayList<>();
	}
}
