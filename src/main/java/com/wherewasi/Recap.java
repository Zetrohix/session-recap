package com.wherewasi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// builds the chat recap shown when rejoining a world
public final class Recap {
	private static final int TOP_BLOCKS = 3;

	private Recap() {
	}

	public static Component build(SessionRecord last, Player player) {
		if (last == null || !last.hasPosition()) {
			return Component.empty()
				.append(header())
				.append(Component.literal("\nI'll remember this session for next time. Leave yourself a reminder with ")
					.withStyle(ChatFormatting.GRAY))
				.append(command("/wherewasi note <text>"));
		}

		MutableComponent msg = Component.empty().append(header());

		msg.append(line("Last here ", ChatFormatting.GRAY)
			.append(Component.literal(ago(last.lastSeen)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" · played " + duration(last.sessionMillis)).withStyle(ChatFormatting.GRAY)));

		msg.append(line("Logged off at ", ChatFormatting.GRAY)
			.append(coords(last.x, last.y, last.z))
			.append(Component.literal(" in " + dimensionName(last.dimension)).withStyle(ChatFormatting.GRAY))
			.append(distanceFrom(player, last.x, last.z, last.dimension)));

		if (last.hasDeath) {
			msg.append(line("Last death ", ChatFormatting.RED)
				.append(coords(last.deathX, last.deathY, last.deathZ))
				.append(Component.literal(" in " + dimensionName(last.deathDimension) + ", " + ago(last.deathTime))
					.withStyle(ChatFormatting.GRAY))
				.append(distanceFrom(player, last.deathX, last.deathZ, last.deathDimension)));
		}

		if (!last.mined.isEmpty()) {
			msg.append(line("Mined ", ChatFormatting.GRAY).append(topBlocks(last.mined)));
		}
		if (!last.placed.isEmpty()) {
			msg.append(line("Placed ", ChatFormatting.GRAY).append(topBlocks(last.placed)));
		}
		if (last.distanceWalked >= 50) {
			msg.append(line("Travelled ", ChatFormatting.GRAY)
				.append(Component.literal(distance(last.distanceWalked)).withStyle(ChatFormatting.WHITE)));
		}

		if (last.note != null && !last.note.isBlank()) {
			msg.append(line("Note: ", ChatFormatting.YELLOW)
				.append(Component.literal(last.note).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)));
		} else {
			msg.append(line("Tip: ", ChatFormatting.DARK_GRAY).append(command("/wherewasi note <text>")));
		}

		return msg;
	}

	public static String dimensionId(Level level) {
		return level.dimension().identifier().toString();
	}

	private static Component header() {
		return Component.literal(" Where Was I? ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	private static MutableComponent line(String label, ChatFormatting color) {
		return Component.literal("\n" + label).withStyle(color);
	}

	private static Component command(String text) {
		return Component.literal(text).withStyle(Style.EMPTY
			.withColor(ChatFormatting.AQUA)
			.withClickEvent(new ClickEvent.SuggestCommand(text.replace("<text>", "")))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to type"))));
	}

	private static Component coords(double x, double y, double z) {
		String text = String.format("%d %d %d", (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		return Component.literal(text).withStyle(Style.EMPTY
			.withColor(ChatFormatting.WHITE)
			.withUnderlined(true)
			.withClickEvent(new ClickEvent.CopyToClipboard(text))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
	}

	private static Component distanceFrom(Player player, double x, double z, String dimension) {
		if (!dimensionId(player.level()).equals(dimension)) {
			return Component.empty();
		}
		double d = Math.hypot(player.getX() - x, player.getZ() - z);
		if (d < 16) {
			return Component.empty();
		}
		return Component.literal(" (" + distance(d) + " away)").withStyle(ChatFormatting.DARK_GRAY);
	}

	private static Component topBlocks(Map<String, Integer> counts) {
		List<String> entries = counts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.map(e -> number(e.getValue()) + " " + blockName(e.getKey()))
			.toList();
		MutableComponent text = Component.literal(String.join(", ", entries.subList(0, Math.min(TOP_BLOCKS, entries.size()))))
			.withStyle(ChatFormatting.WHITE);
		int others = entries.size() - TOP_BLOCKS;
		if (others > 0) {
			text.append(Component.literal(" +" + others + " more").withStyle(Style.EMPTY
				.withColor(ChatFormatting.GRAY)
				.withUnderlined(true)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(String.join("\n", entries))))));
		}
		return text;
	}

	private static String number(long n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	private static String blockName(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
			return id;
		}
		return BuiltInRegistries.BLOCK.getValue(key).getName().getString();
	}

	private static String dimensionName(String id) {
		if (id == null) {
			return "an unknown dimension";
		}
		return switch (id) {
			case "minecraft:overworld" -> "the Overworld";
			case "minecraft:the_nether" -> "the Nether";
			case "minecraft:the_end" -> "the End";
			default -> id;
		};
	}

	static String ago(long epochMillis) {
		long secs = Math.max(0, (System.currentTimeMillis() - epochMillis) / 1000);
		if (secs < 60) return "just now";
		if (secs < 3600) return plural(secs / 60, "minute") + " ago";
		if (secs < 86400) return plural(secs / 3600, "hour") + " ago";
		if (secs < 86400 * 2) return "yesterday";
		if (secs < 86400 * 30) return plural(secs / 86400, "day") + " ago";
		return plural(secs / (86400 * 30), "month") + " ago";
	}

	static String duration(long millis) {
		long mins = millis / 60000;
		if (mins < 1) return "under a minute";
		if (mins < 60) return mins + "m";
		return (mins / 60) + "h " + (mins % 60) + "m";
	}

	private static String distance(double blocks) {
		return blocks >= 1000 ? String.format("%.1f km", blocks / 1000) : Math.round(blocks) + " blocks";
	}

	private static String plural(long n, String unit) {
		return n + " " + unit + (n == 1 ? "" : "s");
	}
}
