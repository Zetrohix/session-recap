package com.wherewasi;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhereWasI implements ClientModInitializer {
	public static final String MOD_ID = "wherewasi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SessionTracker.onJoin(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SessionTracker.onDisconnect());
		ClientTickEvents.END_CLIENT_TICK.register(SessionTracker::onTick);
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> SessionTracker.onBlockMined(state.getBlock()));
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof DeathScreen && client.player != null) {
				SessionTracker.onDeath(client.player);
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommands.literal("wherewasi")
				.executes(ctx -> showRecap(ctx.getSource()))
				.then(ClientCommands.literal("note")
					.executes(ctx -> showNote(ctx.getSource()))
					.then(ClientCommands.argument("text", StringArgumentType.greedyString())
						.executes(ctx -> setNote(ctx.getSource(), StringArgumentType.getString(ctx, "text")))))
				.then(ClientCommands.literal("clearnote")
					.executes(ctx -> setNote(ctx.getSource(), null)))));
	}

	private static int showRecap(FabricClientCommandSource source) {
		source.sendFeedback(Recap.build(SessionTracker.previous(), source.getPlayer()));
		return 1;
	}

	private static int showNote(FabricClientCommandSource source) {
		String note = SessionTracker.note();
		source.sendFeedback(note == null
			? Component.literal("No note set. Use /wherewasi note <text>").withStyle(ChatFormatting.GRAY)
			: Component.literal("Note: ").withStyle(ChatFormatting.YELLOW).append(Component.literal(note).withStyle(ChatFormatting.WHITE)));
		return 1;
	}

	private static int setNote(FabricClientCommandSource source, String note) {
		if (!SessionTracker.inWorld()) {
			return 0;
		}
		SessionTracker.setNote(note);
		source.sendFeedback(Component.literal(note == null ? "Note cleared." : "Saved. I'll remind you next time you join.")
			.withStyle(ChatFormatting.GREEN));
		return 1;
	}
}
