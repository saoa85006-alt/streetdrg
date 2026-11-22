package com.streetdrag;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class StreetDrag implements ModInitializer {
	public static final String MOD_ID = "streetdrag";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// порядок важен: предметы -> блоки -> блок-энтити -> вкладка
		ModItems.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModTab.init();
	}
}
