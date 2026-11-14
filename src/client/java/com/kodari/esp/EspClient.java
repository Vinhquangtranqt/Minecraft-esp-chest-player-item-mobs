package com.kodari.esp;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class EspClient implements ClientModInitializer {
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.esp.toggle",
            InputConstants.KEY_R,
            KeyMapping.Category.MISC
    );

    static boolean enabled = true;
    static boolean chestsEnabled = true;
    static boolean mobsEnabled = true;
    static boolean itemsEnabled = true;
    static boolean playersEnabled = true;
    static DisplayMode displayMode = DisplayMode.BOX;

    @Override
    public void onInitializeClient() {
        EspConfig.load();
        EspRenderer.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                enabled = !enabled;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommands.literal("esp")
                        .then(ClientCommands.argument("feature", StringArgumentType.word())
                                .executes(context -> executeFeature(
                                        StringArgumentType.getString(context, "feature"),
                                        context.getSource()
                                )))));
    }

    private static int executeFeature(String feature, FabricClientCommandSource source) {
        switch (feature.toLowerCase(Locale.ROOT)) {
            case "chest" -> {
                chestsEnabled = !chestsEnabled;
                sendToggle(source, "chest", chestsEnabled);
            }
            case "mob", "entity" -> {
                mobsEnabled = !mobsEnabled;
                sendToggle(source, "mob", mobsEnabled);
            }
            case "item" -> {
                itemsEnabled = !itemsEnabled;
                sendToggle(source, "item", itemsEnabled);
            }
            case "player" -> {
                playersEnabled = !playersEnabled;
                sendToggle(source, "player", playersEnabled);
            }
            case "all" -> {
                enabled = !enabled;
                source.sendFeedback(Component.translatable(enabled ? "esp.feedback.enabled" : "esp.feedback.disabled"));
            }
            case "box" -> setMode(source, DisplayMode.BOX, "esp.mode.box");
            case "highlight" -> setMode(source, DisplayMode.HIGHLIGHT, "esp.mode.highlight");
            case "tracer" -> setMode(source, DisplayMode.TRACER, "esp.mode.tracer");
            default -> source.sendFeedback(Component.translatable("esp.feedback.unknown", feature));
        }
        return 1;
    }

    private static void sendToggle(FabricClientCommandSource source, String feature, boolean value) {
        source.sendFeedback(Component.translatable(
                "esp.feedback.toggle",
                Component.translatable("esp.feature." + feature),
                Component.translatable(value ? "esp.state.enabled" : "esp.state.disabled")
        ));
    }

    private static void setMode(FabricClientCommandSource source, DisplayMode mode, String translationKey) {
        displayMode = mode;
        source.sendFeedback(Component.translatable("esp.feedback.mode", Component.translatable(translationKey)));
    }

    enum DisplayMode {
        BOX,
        HIGHLIGHT,
        TRACER
    }
}