package com.kodari.esp;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EspConfigScreen {
    private EspConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("esp.config.title"))
                .save(EspConfig::save)
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("esp.config.category.general"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("esp.config.enabled"))
                                .binding(true, () -> EspClient.enabled, value -> EspClient.enabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("esp.config.chests"))
                                .binding(true, () -> EspClient.chestsEnabled, value -> EspClient.chestsEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("esp.config.mobs"))
                                .binding(true, () -> EspClient.mobsEnabled, value -> EspClient.mobsEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("esp.config.items"))
                                .binding(true, () -> EspClient.itemsEnabled, value -> EspClient.itemsEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("esp.config.players"))
                                .binding(true, () -> EspClient.playersEnabled, value -> EspClient.playersEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<EspClient.DisplayMode>createBuilder()
                                .name(Component.translatable("esp.config.mode"))
                                .binding(EspClient.DisplayMode.BOX, () -> EspClient.displayMode, value -> EspClient.displayMode = value)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(EspClient.DisplayMode.class))
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}