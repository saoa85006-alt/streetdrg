package com.streetdrag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Старт/финиш дрэг-трассы. Встал на старт — пошёл таймер, встал на финиш — получил время и монеты. */
public class RaceLineBlock extends Block {
	private record Run(long startTick, BlockPos startPos) {
	}

	private static final Map<UUID, Run> RUNS = new HashMap<>();
	private static final double MIN_DISTANCE = 10.0;

	private final boolean finish;

	public RaceLineBlock(Properties properties, boolean finish) {
		super(properties);
		this.finish = finish;
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(p -> new RaceLineBlock(p, false));
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		if (!(level instanceof ServerLevel)) {
			return;
		}
		Player player = null;
		if (entity instanceof Player p) {
			player = p;
		} else if (entity.getControllingPassenger() instanceof Player p) {
			player = p;
		}
		if (player == null) {
			return;
		}

		long now = level.getGameTime();
		if (!finish) {
			Run old = RUNS.get(player.getUUID());
			// не перезапускать таймер каждый тик, пока стоишь на старте
			if (old == null || old.startPos().equals(pos) || now - old.startTick() > 20 * 120) {
				RUNS.put(player.getUUID(), new Run(now, pos));
				player.displayClientMessage(Component.literal("Старт! Жми на газ.").withStyle(ChatFormatting.GREEN), true);
			}
			return;
		}

		Run run = RUNS.get(player.getUUID());
		if (run == null) {
			return;
		}
		double dist = Math.sqrt(run.startPos().distSqr(pos));
		if (dist < MIN_DISTANCE) {
			return;
		}
		RUNS.remove(player.getUUID());
		double seconds = (now - run.startTick()) / 20.0;
		int coins = Math.max(1, (int) Math.round(30 - seconds));
		player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.RACE_COIN, coins));
		player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
			"Финиш! Время: %.2f c (%d м). Монет: %d", seconds, (int) dist, coins)).withStyle(ChatFormatting.GOLD), false);
	}
}
