package com.streetdrag;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/** Строка над хотбаром: время заезда, монеты, скорость, передача, нитро. Обновляется раз в 2 тика. */
public final class RaceHud {
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 2 != 0) {
			return;
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			CarEntity car = p.getVehicle() instanceof CarEntity c ? c : null;
			RaceLineBlock.Run run = RaceLineBlock.getRun(p.getUUID());
			if (car == null && run == null) {
				continue;
			}

			StringBuilder sb = new StringBuilder();
			if (run != null) {
				double seconds = (p.level().getGameTime() - run.startTick()) / 20.0;
				if (seconds > RaceLineBlock.MAX_SECONDS) {
					RaceLineBlock.clearRun(p.getUUID());
					p.displayClientMessage(Component.literal("Время вышло, заезд сброшен.").withStyle(ChatFormatting.RED), true);
					run = null;
				} else {
					double dist = RaceLineBlock.flatDist(run.startPos(), p.getX(), p.getZ());
					int coins = RaceLineBlock.coinsFor(dist, seconds);
					sb.append(String.format(Locale.ROOT, "Время: %.2f c   Монет сейчас: %d", seconds, coins));
					if (car == null) {
						double speed = seconds > 0.2 ? dist / seconds : 0.0;
						sb.append(String.format(Locale.ROOT, "   Скорость: %.1f бл/с", speed));
					}
				}
			}
			if (car != null) {
				double bps = Math.hypot(car.getX() - car.xo, car.getZ() - car.zo) * 20.0;
				double frac = bps / (car.maxSpeed() * 20.0);
				String gear = bps < 0.5 ? "N" : String.valueOf(Math.min(6, 1 + (int) (frac * 6.0)));
				if (sb.length() > 0) {
					sb.append("   |   ");
				}
				sb.append(String.format(Locale.ROOT, "%d км/ч   Передача: %s", (int) Math.round(bps * 8.0), gear));
				if (p.hasEffect(MobEffects.SPEED)) {
					sb.append("   НИТРО");
				}
			}
			if (sb.length() > 0) {
				p.displayClientMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.YELLOW), true);
			}
		}
	}

	private RaceHud() {
	}
}
