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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Старт/финиш дрэг-трассы. Таймер идёт, пока стоишь на старте, и отсчитывается с момента, как сошёл с линии.
 * Тикающий HUD рисует {@link RaceHud}. Работает пешком и на машине.
 */
public class RaceLineBlock extends Block {
	public record Run(long startTick, BlockPos startPos) {
	}

	private static final Map<UUID, Run> RUNS = new HashMap<>();
	public static final double MIN_DISTANCE = 20.0;
	public static final int MAX_SECONDS = 120;
	private static final int MAX_COINS = 40;

	private final boolean finish;

	public RaceLineBlock(Properties properties, boolean finish) {
		super(properties);
		this.finish = finish;
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(p -> new RaceLineBlock(p, false));
	}

	public static Run getRun(UUID id) {
		return RUNS.get(id);
	}

	public static void clearRun(UUID id) {
		RUNS.remove(id);
	}

	/**
	 * Монеты зависят от средней скорости (блоков/сек) и дистанции: шагом ~3.5 б/с — ноль,
	 * спринт ~5.6 — чуть-чуть, машина с тюнингом — много. Полная награда с 100 блоков.
	 */
	public static int coinsFor(double dist, double seconds) {
		if (seconds < 0.5 || dist < MIN_DISTANCE) {
			return 0;
		}
		double speed = dist / seconds;
		double factor = Math.min(dist, 100.0) / 100.0;
		int coins = (int) Math.round(Math.max(0.0, speed - 3.5) * 2.0 * factor);
		return Math.min(MAX_COINS, coins);
	}

	public static double flatDist(BlockPos a, double x, double z) {
		double dx = x - (a.getX() + 0.5);
		double dz = z - (a.getZ() + 0.5);
		return Math.sqrt(dx * dx + dz * dz);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		if (!(level instanceof ServerLevel)) {
			return;
		}
		Player found = null;
		if (entity instanceof Player p) {
			found = p;
		} else if (entity.getControllingPassenger() instanceof Player p) {
			found = p;
		}
		if (!(found instanceof ServerPlayer player)) {
			return;
		}

		long now = level.getGameTime();
		if (!finish) {
			// пока стоишь на линии старта — таймер на нуле, пойдёт, когда сойдёшь
			RUNS.put(player.getUUID(), new Run(now, pos));
			return;
		}

		Run run = RUNS.get(player.getUUID());
		if (run == null) {
			return;
		}
		double dist = flatDist(run.startPos(), pos.getX() + 0.5, pos.getZ() + 0.5);
		if (dist < MIN_DISTANCE) {
			return;
		}
		RUNS.remove(player.getUUID());
		double seconds = (now - run.startTick()) / 20.0;
		double speed = dist / Math.max(seconds, 0.05);
		int coins = coinsFor(dist, seconds);
		if (coins > 0) {
			player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.RACE_COIN, coins));
		}
		boolean record = RaceRecords.submit(player, speed, seconds, (int) dist);
		player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
			"Финиш! Время: %.2f c | Дистанция: %d м | Скорость: %.1f бл/с | Монет: %d",
			seconds, (int) dist, speed, coins)).withStyle(ChatFormatting.GOLD), false);
		if (record) {
			player.displayClientMessage(Component.literal("Новый личный рекорд скорости!").withStyle(ChatFormatting.GREEN), false);
		}
		player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "Финиш! %.2f c, монет: %d", seconds, coins))
			.withStyle(ChatFormatting.GOLD), true);
	}
}
