package com.streetdrag;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Типы заводских машин и их рецепты. Добавлять новые рецепты сюда. */
public enum MachineType {
	FOUNDRY("foundry", List.of(
		recipe(100, ModItems.STEEL_PLATE_S, 1, in(() -> Items.IRON_INGOT, 2), in(() -> Items.COAL, 1))
	)),
	RUBBER_MIXER("rubber_mixer", List.of(
		recipe(80, ModItems.RUBBER_S, 2, in(() -> Items.SLIME_BALL, 1), in(() -> Items.COAL, 1))
	)),
	CIRCUIT_ASSEMBLER("circuit_assembler", List.of(
		recipe(120, ModItems.ECU_S, 1, in(() -> Items.REDSTONE, 2), in(() -> Items.COPPER_INGOT, 1), in(() -> Items.GOLD_NUGGET, 2))
	)),
	TIRE_PRESS("tire_press", List.of(
		recipe(100, ModItems.TIRE_S, 1, in(ModItems.RUBBER_S, 3), in(ModItems.STEEL_PLATE_S, 1))
	)),
	NITRO_BREWER("nitro_brewer", List.of(
		recipe(160, ModItems.NITRO_S, 1, in(() -> Items.BLAZE_POWDER, 2), in(() -> Items.GUNPOWDER, 2), in(() -> Items.GLOWSTONE_DUST, 1))
	)),
	ENGINE_ASSEMBLER("engine_assembler", List.of(
		recipe(300, ModItems.ENGINE_S, 1, in(ModItems.STEEL_PLATE_S, 4), in(() -> Items.PISTON, 2), in(ModItems.ECU_S, 1)),
		recipe(200, ModItems.TURBO_S, 1, in(ModItems.STEEL_PLATE_S, 3), in(ModItems.ECU_S, 1), in(() -> Items.DIAMOND, 1))
	)),
	CAR_ASSEMBLER("car_assembler", List.of(
		recipe(600, ModItems.CAR_KIT_S, 1, in(ModItems.ENGINE_S, 1), in(ModItems.TIRE_S, 4), in(ModItems.STEEL_PLATE_S, 6), in(ModItems.ECU_S, 1))
	));

	public record Ing(Supplier<Item> item, int count) {
	}

	public record Recipe(List<Ing> inputs, Supplier<Item> output, int outCount, int ticks) {
	}

	public final String id;
	public final List<Recipe> recipes;

	MachineType(String id, List<Recipe> recipes) {
		this.id = id;
		this.recipes = recipes;
	}

	public boolean accepts(ItemStack stack) {
		for (Recipe r : recipes) {
			for (Ing i : r.inputs()) {
				if (stack.is(i.item().get())) {
					return true;
				}
			}
		}
		return false;
	}

	private static Ing in(Supplier<Item> item, int count) {
		return new Ing(item, count);
	}

	private static Recipe recipe(int ticks, Supplier<Item> out, int outCount, Ing... inputs) {
		return new Recipe(List.of(inputs), out, outCount, ticks);
	}
}
