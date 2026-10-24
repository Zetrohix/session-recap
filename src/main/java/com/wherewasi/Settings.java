package com.wherewasi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// global settings
public final class Settings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wherewasi").resolve("settings.json");

	private static Data data;

	private Settings() {
	}

	private static class Data {
		boolean autoRecap = true;
	}

	public static boolean autoRecap() {
		return data().autoRecap;
	}

	public static void setAutoRecap(boolean on) {
		data().autoRecap = on;
		save();
	}

	private static Data data() {
		if (data == null) {
			data = load();
		}
		return data;
	}

	private static Data load() {
		if (!Files.exists(FILE)) {
			return new Data();
		}
		try {
			Data loaded = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
			return loaded != null ? loaded : new Data();
		} catch (IOException | RuntimeException e) {
			WhereWasI.LOGGER.warn("Couldn't read settings {}", FILE, e);
			return new Data();
		}
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException e) {
			WhereWasI.LOGGER.warn("Couldn't save settings", e);
		}
	}
}
