package com.streetdrag;

import java.util.EnumMap;
import java.util.Map;
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
	private static final Map<MachineType, MachineBlock> MACHINE_MAP = new EnumMap<>(MachineType.class);

	static {
		for (MachineType type : MachineType.values()) {
			MACHINE_MAP.put(type, register(type.id, p -> new MachineBlock(p, type),
				BlockBehaviour.Properties.of().strength(3.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
		}
	}

	/** Все заводские блоки (порядок как в MachineType). */
	public static final MachineBlock[] MACHINES = MACHINE_MAP.values().toArray(new MachineBlock[0]);

	public static final Block ASPHALT = register("asphalt", Block::new,
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
	public static final RaceLineBlock RACE_START = register("race_start", p -> new RaceLineBlock(p, false),
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
	public static final RaceLineBlock RACE_FINISH = register("race_finish", p -> new RaceLineBlock(p, true),
		BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());

	public static MachineBlock machine(MachineType type) {
		return MACHINE_MAP.get(type);
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
