package com.wherewasi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// tracks the current session and persists it per world/server
public final class SessionTracker {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("wherewasi");
	private static final int SAVE_INTERVAL_TICKS = 20 * 60;
	// prevents counting teleports
	private static final double MAX_STEP = 10.0;
	private static final long SHORT_SESSION_MILLIS = 2 * 60 * 1000;

	private static String worldKey;
	private static SessionRecord current;
	private static SessionRecord previous;
	private static boolean recapPending;
	private static long sessionStart;
	private static double lastX, lastZ;
	private static boolean hasLastPos;
	private static int ticksSinceSave;

	private SessionTracker() {
	}

	public static void onJoin(Minecraft client) {
		worldKey = resolveWorldKey(client);
		previous = load(worldKey);
		current = new SessionRecord();
		if (previous != null) {
			current.hasDeath = previous.hasDeath;
			current.deathX = previous.deathX;
			current.deathY = previous.deathY;
			current.deathZ = previous.deathZ;
			current.deathDimension = previous.deathDimension;
			current.deathTime = previous.deathTime;
			current.note = previous.note;
		}
		sessionStart = System.currentTimeMillis();
		hasLastPos = false;
		ticksSinceSave = 0;
		recapPending = true;
	}

	public static void onDisconnect() {
		if (current != null) {
			save();
		}
		worldKey = null;
		current = null;
		previous = null;
		recapPending = false;
	}

	public static void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (current == null || player == null) {
			return;
		}

		// Show the recap once the player actually exists in the world.
		if (recapPending) {
			recapPending = false;
			player.sendSystemMessage(Recap.build(previous, player));
		}

		double px = player.getX(), pz = player.getZ();
		if (hasLastPos) {
			double step = Math.hypot(px - lastX, pz - lastZ);
			if (step < MAX_STEP) {
				current.distanceWalked += step;
			}
		}
		lastX = px;
		lastZ = pz;
		hasLastPos = true;

		current.x = px;
		current.y = player.getY();
		current.z = pz;
		current.dimension = Recap.dimensionId(player.level());
		current.lastSeen = System.currentTimeMillis();
		current.sessionMillis = current.lastSeen - sessionStart;

		// Periodic save so a crash doesn't lose the session.
		if (++ticksSinceSave >= SAVE_INTERVAL_TICKS) {
			ticksSinceSave = 0;
			save();
		}
	}

	public static void onBlockMined(Block block) {
		if (current != null) {
			current.mined.merge(blockId(block), 1, Integer::sum);
		}
	}

	public static void onBlockPlaced(Block block) {
		if (current != null) {
			current.placed.merge(blockId(block), 1, Integer::sum);
		}
	}

	public static void onDeath(LocalPlayer player) {
		if (current == null) {
			return;
		}
		current.hasDeath = true;
		current.deathX = player.getX();
		current.deathY = player.getY();
		current.deathZ = player.getZ();
		current.deathDimension = Recap.dimensionId(player.level());
		current.deathTime = System.currentTimeMillis();
		save();
	}

	public static boolean inWorld() {
		return current != null;
	}

	public static SessionRecord previous() {
		return previous;
	}

	public static String note() {
		return current == null ? null : current.note;
	}

	public static void setNote(String note) {
		if (current != null) {
			current.note = note;
			save();
		}
	}

	private static String blockId(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	private static String resolveWorldKey(Minecraft client) {
		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			return "sp_" + root.getFileName();
		}
		ServerData data = client.getCurrentServer();
		return "mp_" + (data != null ? data.ip : "unknown");
	}

	private static Path file(String key) {
		return DIR.resolve(key.replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
	}

	private static SessionRecord load(String key) {
		Path path = file(key);
		if (!Files.exists(path)) {
			return null;
		}
		try {
			return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), SessionRecord.class);
		} catch (IOException | RuntimeException e) {
			WhereWasI.LOGGER.warn("Couldn't read session record {}", path, e);
			return null;
		}
	}

	private static void save() {
		if (worldKey == null || current == null || !current.hasPosition()) {
			return;
		}
		try {
			Files.createDirectories(DIR);
			Files.writeString(file(worldKey), GSON.toJson(recordToSave()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			WhereWasI.LOGGER.warn("Couldn't save session record", e);
		}
	}

	/**
	 * A quick hop in and out (say, to tweak settings) shouldn't wipe the recap of
	 * the last real session, so short idle sessions keep the previous stats.
	 */
	private static SessionRecord recordToSave() {
		boolean idle = current.mined.isEmpty() && current.placed.isEmpty();
		if (previous == null || !idle || current.sessionMillis >= SHORT_SESSION_MILLIS) {
			return current;
		}
		SessionRecord merged = GSON.fromJson(GSON.toJson(current), SessionRecord.class);
		merged.sessionMillis = previous.sessionMillis;
		merged.distanceWalked = previous.distanceWalked + current.distanceWalked;
		merged.mined = previous.mined;
		merged.placed = previous.placed;
		return merged;
	}
}
