package com.streetdrag;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /streetguide, /streettop, /streetshop. */
public final class StreetCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("streetguide").executes(ctx -> {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			player.getInventory().placeItemBackInInventory(GuideBook.create());
			return 1;
		}));

		dispatcher.register(Commands.literal("streettop").executes(ctx -> {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			List<RaceRecords.Entry> top = RaceRecords.top(10);
			player.displayClientMessage(Component.literal("Топ скорости заездов:").withStyle(ChatFormatting.GOLD), false);
			if (top.isEmpty()) {
				player.displayClientMessage(Component.literal("Пока никто не финишировал.").withStyle(ChatFormatting.GRAY), false);
			}
			int place = 1;
			for (RaceRecords.Entry e : top) {
				player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
					"%d. %s — %.1f бл/с (%d м за %.2f c)", place++, e.name(), e.speed(), e.dist(), e.time())), false);
			}
			return 1;
		}));

		dispatcher.register(Commands.literal("streetshop")
			.executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				player.displayClientMessage(Component.literal("Магазин. Твои монеты: " + Shop.countCoins(player))
					.withStyle(ChatFormatting.GOLD), false);
				for (Shop.Offer o : Shop.OFFERS.values()) {
					player.displayClientMessage(Component.literal(o.id() + " — " + o.name() + " — " + o.price() + " монет"), false);
				}
				player.displayClientMessage(Component.literal("Купить: /streetshop buy <id>").withStyle(ChatFormatting.GRAY), false);
				return 1;
			})
			.then(Commands.literal("buy").then(Commands.argument("item", StringArgumentType.word())
				.suggests((c, b) -> SharedSuggestionProvider.suggest(Shop.OFFERS.keySet(), b))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Shop.Offer offer = Shop.OFFERS.get(StringArgumentType.getString(ctx, "item"));
					if (offer == null) {
						player.displayClientMessage(Component.literal("Нет такого товара. Список: /streetshop")
							.withStyle(ChatFormatting.RED), false);
						return 0;
					}
					if (Shop.countCoins(player) < offer.price()) {
						player.displayClientMessage(Component.literal("Не хватает монет: нужно " + offer.price()
							+ ", у тебя " + Shop.countCoins(player)).withStyle(ChatFormatting.RED), false);
						return 0;
					}
					Shop.takeCoins(player, offer.price());
					player.getInventory().placeItemBackInInventory(offer.stack().get());
					player.displayClientMessage(Component.literal("Куплено: " + offer.name()).withStyle(ChatFormatting.GREEN), false);
					return 1;
				}))));
	}

	private StreetCommands() {
	}
}
