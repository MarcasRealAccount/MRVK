package marcasrealaccount.vulkan.mixins.com.mojang.blaze3d.platform;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlStateManager;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
	@Inject(at = @At("HEAD"), cancellable = true, method = { "_disableScissorTest()V", "_enableScissorTest()V",
			"_scissorBox(IIII)V", "_disableDepthTest()V", "_enableDepthTest()V", "_depthFunc(I)V", "_depthMask(Z)V",
			"_disableBlend()V", "_enableBlend()V", "_blendFunc(II)V", "_blendFuncSeparate(IIII)V", "_blendEquation(I)V",
			"glAttachShader(II)V", "glDeleteShader(I)V", "glShaderSource(ILjava/util/List;)V", "glCompileShader(I)V",
			"_glUseProgram(I)V", "glDeleteProgram(I)V", "glLinkProgram(I)V", "_glUniform1(ILjava/nio/IntBuffer;)V",
			"_glUniform1i(II)V", "_glUniform1(ILjava/nio/FloatBuffer;)V", "_glUniform2(ILjava/nio/IntBuffer;)V",
			"_glUniform2(ILjava/nio/FloatBuffer;)V", "_glUniform3(ILjava/nio/IntBuffer;)V",
			"_glUniform3(ILjava/nio/FloatBuffer;)V", "_glUniform4(ILjava/nio/IntBuffer;)V",
			"_glUniform4(ILjava/nio/FloatBuffer;)V", "_glUniformMatrix2(IZLjava/nio/FloatBuffer;)V",
			"_glUniformMatrix3(IZLjava/nio/FloatBuffer;)V", "_glUniformMatrix4(IZLjava/nio/FloatBuffer;)V",
			"_glBindAttribLocation(IILjava/lang/CharSequence;)V", "_glBindBuffer(II)V", "_glBindVertexArray(I)V",
			"_glBufferData(ILjava/nio/ByteBuffer;I)V", "_glBufferData(IJI)V", "_glUnmapBuffer(I)V",
			"_glDeleteBuffers(I)V", "_glCopyTexSubImage2D(IIIIIIII)V", "_glDeleteVertexArrays(I)V",
			"_glBindFramebuffer(II)V", "_glBlitFrameBuffer(IIIIIIIIII)V", "_glBindRenderbuffer(II)V",
			"_glDeleteRenderbuffers(I)V", "_glDeleteFramebuffers(I)V", "_glRenderbufferStorage(IIII)V",
			"_glFramebufferRenderbuffer(IIII)V", "_glFramebufferTexture2D(IIIII)V", "glActiveTexture(I)V",
			"glBlendFuncSeparate(IIII)V",
			"setupLevelDiffuseLighting(Lnet/minecraft/util/math/Vec3f;Lnet/minecraft/util/math/Vec3f;Lnet/minecraft/util/math/Matrix4f;)V",
			"setupGuiFlatDiffuseLighting(Lnet/minecraft/util/math/Vec3f;Lnet/minecraft/util/math/Vec3f;)V",
			"setupGui3DDiffuseLighting(Lnet/minecraft/util/math/Vec3f;Lnet/minecraft/util/math/Vec3f;)V",
			"_enableCull()V", "_disableCull()V", "_polygonMode(II)V", "_disablePolygonOffset()V", "_polygonOffset(FF)V",
			"_enableColorLogicOp()V", "_disableColorLogicOp()V", "_logicOp(I)V", "_activeTexture(I)V",
			"_enableTexture()V", "_disableTexture()V", "_texParameter(IIF)V", "_texParameter(III)V",
			"_genTextures([I)V", "_deleteTexture(I)V", "_deleteTextures([I)V", "_bindTexture(I)V",
			"_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", "_texSubImage2D(IIIIIIIIJ)V", "_getTexImage(IIIIJ)V",
			"_viewport(IIII)V", "_colorMask(ZZZZ)V", "_stencilFunc(III)V", "_stencilMask(I)V", "_stencilOp(III)V",
			"_clearDepth(Z)V", "_clearColor(FFFF)V", "_clearStencil(I)V", "_clear(IZ)V", "_glDrawPixels(IIIIJ)V",
			"_vertexAttribPointer(IIIZIJ)V", "_vertexAttribIPointer(IIIIJ)V", "_enableVertexAttribAtrray(I)V",
			"_disableVertexAttribArray(I)V", "_drawElements(IIIJ)V", "_pixelStore(II)V",
			"_readPixels(IIIIIILjava/nio/ByteBuffer;)V", "_readPixels(IIIIIIJ)V" })
	private static void disableGLCall(CallbackInfo info) {
		info.cancel();
	}

	@Inject(at = @At("HEAD"), cancellable = true, method = { "glGetProgrami(II)I", "glCreateShader(I)I",
			"glGetShaderi(II)I", "glCreateProgram()I", "_glGetUniformLocation(ILjava/lang/CharSequence;)I",
			"_glGetAttribLocation(ILjava/lang/CharSequence;)I", "_glGenBuffers()I", "_glGenVertexArrays()I",
			"glGenFramebuffers()I", "glGenRenderbuffers()I", "glCheckFramebufferStatus(I)I", "getBoundFramebuffer()I",
			"_getTexLevelParameter(III)I", "_genTexture()I", "_getTextureId(I)I", "_getActiveTexture()I",
			"_getError()I", "_getInteger(I)I" })
	private static void disableGLCallInt(CallbackInfoReturnable<Integer> info) {
		info.setReturnValue(0);
	}

	@Inject(at = @At("HEAD"), cancellable = true, method = { "glGetShaderInfoLog(II)Ljava/lang/String;",
			"glGetprogramInfoLog(II)Ljava/lang/String;", "_getString(I)Ljava/lang/String;" })
	private static void disableGLCallString(CallbackInfoReturnable<String> info) {
		info.setReturnValue(null);
	}

	@Inject(at = @At("HEAD"), cancellable = true, method = "mapBuffer(II)Ljava/nio/ByteBuffer;")
	private static void disableMapBuffer(CallbackInfoReturnable<ByteBuffer> info) {
		info.setReturnValue(null);
	}
}
