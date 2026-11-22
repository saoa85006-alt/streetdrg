package com.streetdrag;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModTab {
	public static void init() {
		CreativeModeTab tab = FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.streetdrag"))
			.icon(() -> new ItemStack(ModItems.CAR_KIT))
			.displayItems((params, out) -> {
				out.accept(GuideBook.create());
				for (MachineBlock m : ModBlocks.MACHINES) {
					out.accept(m);
				}
				out.accept(ModBlocks.ASPHALT);
				out.accept(ModBlocks.RACE_START);
				out.accept(ModBlocks.RACE_FINISH);
				out.accept(ModItems.STEEL_PLATE);
				out.accept(ModItems.RUBBER);
				out.accept(ModItems.ECU_CHIP);
				out.accept(ModItems.ENGINE);
				out.accept(ModItems.TURBO);
				out.accept(ModItems.TIRE);
				out.accept(ModItems.NITRO);
				out.accept(ModItems.CAR_KIT);
				out.accept(ModItems.RACE_COIN);
			})
			.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, StreetDrag.id("main")), tab);
	}

	private ModTab() {
	}
}
