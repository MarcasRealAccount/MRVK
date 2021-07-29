package marcasrealaccount.vulkan.mixins.net.minecraft.util.math;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.math.MathConstants;
import net.minecraft.util.math.Matrix4f;

@Mixin(Matrix4f.class)
public abstract class MixinMatrix4f {
	@Shadow
	protected float a00;
	@Shadow
	protected float a01;
	@Shadow
	protected float a02;
	@Shadow
	protected float a03;
	@Shadow
	protected float a10;
	@Shadow
	protected float a11;
	@Shadow
	protected float a12;
	@Shadow
	protected float a13;
	@Shadow
	protected float a20;
	@Shadow
	protected float a21;
	@Shadow
	protected float a22;
	@Shadow
	protected float a23;
	@Shadow
	protected float a30;
	@Shadow
	protected float a31;
	@Shadow
	protected float a32;
	@Shadow
	protected float a33;

	private void viewboxMatrixVulkan(double fov, float aspectRatio, float cameraDepth, float viewDistance) {
		float focalLength = (float) (1.0D / Math.tan(fov * MathConstants.RADIANS_PER_DEGREE / 2.0D));

		float x = focalLength / aspectRatio;
		float y = -focalLength;
		float A = cameraDepth / (viewDistance - cameraDepth);
		float B = viewDistance * A;

		this.a00 = x;
		this.a11 = y;
		this.a22 = A;
		this.a23 = B;
		this.a32 = -1.0f;
	}

	@Inject(at = @At("TAIL"), cancellable = true, method = "viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;")
	private static void viewboxMatrixVulkan(double fov, float aspectRatio, float cameraDepth, float viewDistance,
			CallbackInfoReturnable<Matrix4f> info) {
		Matrix4f mat = new Matrix4f();
		((MixinMatrix4f) (Object) mat).viewboxMatrixVulkan(fov, aspectRatio, cameraDepth, viewDistance);
		info.setReturnValue(mat);
	}
}
