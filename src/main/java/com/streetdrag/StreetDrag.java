package com.streetdrag;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class StreetDrag implements ModInitializer {
	public static final String MOD_ID = "streetdrag";
	private static final String GUIDE_TAG = "streetdrag_guide_given";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// порядок важен: предметы -> блоки -> блок-энтити -> сущности -> вкладка
		ModItems.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModEntities.init();
		ModTab.init();

		RaceRecords.load();

		// HUD гонки/машины над хотбаром
		ServerTickEvents.END_SERVER_TICK.register(RaceHud::tick);

		// книга-гайд при первом входе (метка хранится в данных игрока)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!player.getTags().contains(GUIDE_TAG)) {
				player.addTag(GUIDE_TAG);
				player.getInventory().placeItemBackInInventory(GuideBook.create());
			}
		});

		// /streetguide, /streettop, /streetshop
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			StreetCommands.register(dispatcher));
	}
}
