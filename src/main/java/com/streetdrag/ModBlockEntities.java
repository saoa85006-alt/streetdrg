package com.streetdrag;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<MachineBlockEntity> MACHINE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		StreetDrag.id("machine"),
		FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, ModBlocks.MACHINES).build()
	);

	public static void init() {
	}

	private ModBlockEntities() {
	}
}
