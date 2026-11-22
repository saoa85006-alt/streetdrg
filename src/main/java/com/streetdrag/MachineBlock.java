package com.streetdrag;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Заводская машина. ПКМ предметом — загрузить сырьё, Shift+ПКМ пустой рукой — забрать результат,
 * ПКМ пустой рукой — показать рецепты. Воронки/хоппер-линии работают сверху/сбоку (вход) и снизу (выход).
 */
public class MachineBlock extends BaseEntityBlock {
	public final MachineType type;

	public MachineBlock(Properties properties, MachineType type) {
		super(properties);
		this.type = type;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(p -> new MachineBlock(p, MachineType.FOUNDRY));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(blockEntityType, ModBlockEntities.MACHINE, MachineBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
			if (!type.accepts(stack)) {
				// пустая рука / левый предмет -> идём в useWithoutItem (рецепты, забрать результат)
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			int moved = be.insertStack(stack);
			if (moved > 0) {
				stack.shrink(moved);
				return InteractionResult.SUCCESS;
			}
			player.displayClientMessage(Component.literal("Слоты забиты или этот предмет уже лежит в другом слоте.")
				.withStyle(ChatFormatting.RED), true);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity be)) {
			return InteractionResult.PASS;
		}
		if (player.isShiftKeyDown()) {
			ItemStack out = be.takeOutput();
			if (!out.isEmpty()) {
				player.getInventory().placeItemBackInInventory(out);
			} else {
				player.displayClientMessage(Component.literal("Выход пуст.").withStyle(ChatFormatting.GRAY), true);
			}
			return InteractionResult.SUCCESS;
		}
		for (Component line : be.describe(type)) {
			player.displayClientMessage(line, false);
		}
		return InteractionResult.SUCCESS;
	}
}
