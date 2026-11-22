package com.streetdrag;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/** Личные рекорды (лучшая средняя скорость заезда). Хранятся в config/streetdrag_records.json. */
public final class RaceRecords {
	public record Entry(String name, double speed, double time, int dist) {
	}

	private static final Map<UUID, Entry> BEST = new HashMap<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("streetdrag_records.json");
	}

	public static synchronized void load() {
		BEST.clear();
		Path f = file();
		if (!Files.exists(f)) {
			return;
		}
		try (Reader r = Files.newBufferedReader(f)) {
			JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				BEST.put(UUID.fromString(e.getKey()), new Entry(
					o.get("name").getAsString(), o.get("speed").getAsDouble(),
					o.get("time").getAsDouble(), o.get("dist").getAsInt()));
			}
		} catch (Exception ex) {
			// битый файл — начинаем с чистого листа
			BEST.clear();
		}
	}

	private static void save() {
		JsonObject root = new JsonObject();
		for (Map.Entry<UUID, Entry> e : BEST.entrySet()) {
			JsonObject o = new JsonObject();
			o.addProperty("name", e.getValue().name());
			o.addProperty("speed", e.getValue().speed());
			o.addProperty("time", e.getValue().time());
			o.addProperty("dist", e.getValue().dist());
			root.add(e.getKey().toString(), o);
		}
		try {
			Files.writeString(file(), GSON.toJson(root));
		} catch (Exception ex) {
			// не критично: рекорд останется в памяти до перезапуска
		}
	}

	/** Возвращает true, если это новый личный рекорд. */
	public static synchronized boolean submit(ServerPlayer player, double speed, double time, int dist) {
		Entry old = BEST.get(player.getUUID());
		if (old != null && old.speed() >= speed) {
			return false;
		}
		BEST.put(player.getUUID(), new Entry(player.getGameProfile().name(), speed, time, dist));
		save();
		return true;
	}

	public static synchronized List<Entry> top(int n) {
		List<Entry> list = new ArrayList<>(BEST.values());
		list.sort(Comparator.comparingDouble(Entry::speed).reversed());
		return list.size() > n ? list.subList(0, n) : list;
	}

	private RaceRecords() {
	}
}
