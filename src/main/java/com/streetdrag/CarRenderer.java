package com.streetdrag;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Рисует машину как 3D-модель предмета streetdrag:car_body_<цвет>
 * (assets/streetdrag/models/item/car_body_<цвет>.json). Внешний вид правится в JSON, код трогать не надо.
 */
public class CarRenderer extends EntityRenderer<CarEntity, CarRenderState> {
	private static final ItemStack[] MODEL_STACKS = createModelStacks();

	private final ItemModelResolver itemModelResolver;

	public CarRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
		this.shadowRadius = 0.9F;
	}

	private static ItemStack[] createModelStacks() {
		ItemStack[] stacks = new ItemStack[CarEntity.COLORS.length];
		for (int i = 0; i < stacks.length; i++) {
			ItemStack stack = new ItemStack(Items.STICK);
			stack.set(DataComponents.ITEM_MODEL, StreetDrag.id("car_body_" + CarEntity.COLORS[i]));
			stacks[i] = stack;
		}
		return stacks;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(CarEntity entity, CarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		state.color = Math.floorMod(entity.getColor(), MODEL_STACKS.length);
		this.itemModelResolver.updateForTopItem(state.item, MODEL_STACKS[state.color], ItemDisplayContext.FIXED,
			entity.level(), null, entity.getId());
	}

	@Override
	public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.scale(2.0F, 2.0F, 2.0F);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
