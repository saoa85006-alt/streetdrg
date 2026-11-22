package com.streetdrag;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<CarEntity> CAR = register("car",
		EntityType.Builder.<CarEntity>of(CarEntity::new, MobCategory.MISC)
			.sized(1.6F, 0.7F)
			.clientTrackingRange(10)
			.updateInterval(1));

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, StreetDrag.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}

	private ModEntities() {
	}
}
