package com.streetdrag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Магазин за гоночные монеты: /streetshop и /streetshop buy <id>. */
public final class Shop {
	public record Offer(String id, String name, Supplier<ItemStack> stack, int price) {
	}

	public static final Map<String, Offer> OFFERS = new LinkedHashMap<>();

	static {
		add("nitro", "Нитро x1", () -> new ItemStack(ModItems.NITRO), 8);
		add("tire", "Шина x1", () -> new ItemStack(ModItems.TIRE), 12);
		add("ecu", "Блок управления x1", () -> new ItemStack(ModItems.ECU_CHIP), 15);
		add("steel", "Стальная пластина x8", () -> new ItemStack(ModItems.STEEL_PLATE, 8), 10);
		add("turbo", "Турбина x1", () -> new ItemStack(ModItems.TURBO), 45);
		add("engine", "Двигатель x1", () -> new ItemStack(ModItems.ENGINE), 70);
		add("carkit", "Набор для авто x1", () -> new ItemStack(ModItems.CAR_KIT), 180);
		add("asphalt", "Асфальт x16", () -> new ItemStack(ModBlocks.ASPHALT, 16), 6);
		add("start", "Линия старта x1", () -> new ItemStack(ModBlocks.RACE_START), 4);
		add("finish", "Линия финиша x1", () -> new ItemStack(ModBlocks.RACE_FINISH), 4);
	}

	private static void add(String id, String name, Supplier<ItemStack> stack, int price) {
		OFFERS.put(id, new Offer(id, name, stack, price));
	}

	public static int countCoins(Player player) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(ModItems.RACE_COIN)) {
				total += s.getCount();
			}
		}
		return total;
	}

	public static void takeCoins(Player player, int amount) {
		int left = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.is(ModItems.RACE_COIN)) {
				int take = Math.min(left, s.getCount());
				s.shrink(take);
				left -= take;
				if (s.isEmpty()) {
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
		}
	}

	private Shop() {
	}
}
