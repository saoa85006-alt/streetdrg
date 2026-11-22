package com.streetdrag;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item STEEL_PLATE = simple("steel_plate");
	public static final Item RUBBER = simple("rubber");
	public static final Item ECU_CHIP = simple("ecu_chip");
	public static final Item ENGINE = simple("engine");
	public static final Item TURBO = simple("turbo");
	public static final Item TIRE = simple("tire");
	public static final Item CAR_KIT = register("car_kit", CarKitItem::new);
	public static final Item RACE_COIN = simple("race_coin");
	public static final Item NITRO = register("nitro", NitroItem::new);

	// Supplier-ы для рецептов (предметы регистрируются раньше, чем читаются рецепты)
	public static final Supplier<Item> STEEL_PLATE_S = () -> STEEL_PLATE;
	public static final Supplier<Item> RUBBER_S = () -> RUBBER;
	public static final Supplier<Item> ECU_S = () -> ECU_CHIP;
	public static final Supplier<Item> ENGINE_S = () -> ENGINE;
	public static final Supplier<Item> TURBO_S = () -> TURBO;
	public static final Supplier<Item> TIRE_S = () -> TIRE;
	public static final Supplier<Item> CAR_KIT_S = () -> CAR_KIT;
	public static final Supplier<Item> NITRO_S = () -> NITRO;

	private static Item simple(String name) {
		return register(name, Item::new);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, StreetDrag.id(name));
		Item item = factory.apply(new Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
	}

	private ModItems() {
	}
}
