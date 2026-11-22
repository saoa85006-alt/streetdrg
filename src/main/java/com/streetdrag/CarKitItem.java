package com.streetdrag;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** ПКМ по земле — поставить собранную машину (с сохранённым тюнингом и цветом). */
public class CarKitItem extends Item {
	public CarKitItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = context.getItemInHand();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		CarEntity car = new CarEntity(ModEntities.CAR, server);
		car.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		car.setYRot(context.getRotation());
		car.applyKit(stack);
		server.addFreshEntity(car);
		stack.consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}
}
