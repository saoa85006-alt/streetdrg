package com.streetdrag;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static final MachineBlock FOUNDRY = machine("foundry", MachineType.FOUNDRY);
	public static final MachineBlock RUBBER_MIXER = machine("rubber_mixer", MachineType.RUBBER_MIXER);
	public static final MachineBlock CIRCUIT_ASSEMBLER = machine("circuit_assembler", MachineType.CIRCUIT_ASSEMBLER);
	public static final MachineBlock TIRE_PRESS = machine("tire_press", MachineType.TIRE_PRESS);
	public static final MachineBlock NITRO_BREWER = machine("nitro_brewer", MachineType.NITRO_BREWER);
	public static final MachineBlock ENGINE_ASSEMBLER = machine("engine_assembler", MachineType.ENGINE_ASSEMBLER);
	public static final MachineBlock CAR_ASSEMBLER = machine("car_assembler", MachineType.CAR_ASSEMBLER);

	public static final Block ASPHALT = register("asphalt", Block::new,
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
	public static final RaceLineBlock RACE_START = register("race_start", p -> new RaceLineBlock(p, false),
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
	public static final RaceLineBlock RACE_FINISH = register("race_finish", p -> new RaceLineBlock(p, true),
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());

	public static final MachineBlock[] MACHINES = {
		FOUNDRY, RUBBER_MIXER, CIRCUIT_ASSEMBLER, TIRE_PRESS, NITRO_BREWER, ENGINE_ASSEMBLER, CAR_ASSEMBLER
	};

	private static MachineBlock machine(String name, MachineType type) {
		return register(name, p -> new MachineBlock(p, type),
			BlockBehaviour.Properties.of().strength(3.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
	}

	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties props) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, StreetDrag.id(name));
		T block = factory.apply(props.setId(key));
		Registry.register(BuiltInRegistries.BLOCK, key, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, StreetDrag.id(name));
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
