/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.CameraTransformViewBobbingListener.CameraTransformViewBobbingEvent;
import net.wurstclient.events.HitResultRayTraceListener.HitResultRayTraceEvent;
import net.wurstclient.events.RenderListener.RenderEvent;
import net.wurstclient.hacks.FullbrightHack;
import net.wurstclient.mixinterface.IGameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
	implements AutoCloseable, SynchronousResourceReloader, IGameRenderer
{
	private boolean cancelNextBobView;
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		ordinal = 0),
		method = {
			"renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"})
	private void onRenderWorldViewBobbing(float tickDelta, long limitTime,
		MatrixStack matrices, CallbackInfo ci)
	{
		CameraTransformViewBobbingEvent event =
			new CameraTransformViewBobbingEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			cancelNextBobView = true;
	}
	
	@Inject(at = @At("HEAD"),
		method = "bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		cancellable = true)
	private void onBobView(MatrixStack matrices, float tickDelta,
		CallbackInfo ci)
	{
		if(!cancelNextBobView)
			return;
		
		ci.cancel();
		cancelNextBobView = false;
	}
	
	@Inject(at = @At("HEAD"),
		method = "renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V")
	private void renderHand(MatrixStack matrices, Camera camera,
		float tickDelta, CallbackInfo ci)
	{
		cancelNextBobView = false;
	}
	
	@Inject(
		at = {@At(value = "FIELD",
			target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
			opcode = Opcodes.GETFIELD,
			ordinal = 0)},
		method = {
			"renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"})
	private void onRenderWorld(float partialTicks, long finishTimeNano,
		MatrixStack matrixStack, CallbackInfo ci)
	{
		RenderEvent event = new RenderEvent(matrixStack, partialTicks);
		EventManager.fire(event);
	}
	
	@Inject(at = @At(value = "RETURN", ordinal = 1),
		method = {"getFov(Lnet/minecraft/client/render/Camera;FZ)D"},
		cancellable = true)
	private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
		CallbackInfoReturnable<Double> cir)
	{
		cir.setReturnValue(WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(cir.getReturnValueD()));
	}
	
	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;getCameraPosVec(F)Lnet/minecraft/util/math/Vec3d;",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0)}, method = {"updateTargetedEntity(F)V"})
	private void onHitResultRayTrace(float float_1, CallbackInfo ci)
	{
		HitResultRayTraceEvent event = new HitResultRayTraceEvent(float_1);
		EventManager.fire(event);
	}
	
	@Redirect(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F",
			ordinal = 0),
		method = {
			"renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"})
	private float wurstNauseaLerp(float delta, float first, float second)
	{
		if(!WurstClient.INSTANCE.getHax().antiWobbleHack.isEnabled())
			return MathHelper.lerp(delta, first, second);
		
		return 0;
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"},
		cancellable = true)
	private static void onGetNightVisionStrength(LivingEntity livingEntity,
		float f, CallbackInfoReturnable<Float> cir)
	{
		FullbrightHack fullbright =
			WurstClient.INSTANCE.getHax().fullbrightHack;
		
		if(fullbright.isNightVisionActive())
			cir.setReturnValue(fullbright.getNightVisionStrength());
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"bobViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V"},
		cancellable = true)
	private void onBobViewWhenHurt(MatrixStack matrixStack, float f,
		CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noHurtcamHack.isEnabled())
			ci.cancel();
	}
	
	@Shadow
	private void bobView(MatrixStack matrixStack, float partalTicks)
	{
		
	}
	
	@Override
	public void loadWurstShader(Identifier id)
	{
		loadPostProcessor(id);
	}
	
	@Shadow
	private void loadPostProcessor(Identifier id)
	{
		
	}
}
