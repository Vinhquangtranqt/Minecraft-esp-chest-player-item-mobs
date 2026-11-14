package com.kodari.esp;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class EspConfig {
    private static final ConfigClassHandler<EspConfig> HANDLER = ConfigClassHandler.createBuilder(EspConfig.class)
            .id(Identifier.fromNamespaceAndPath("esp", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(Path.of("config", "esp.json"))
                    .build())
            .build();

    @SerialEntry
    public boolean enabled = true;
    @SerialEntry
    public boolean chestsEnabled = true;
    @SerialEntry
    public boolean mobsEnabled = true;
    @SerialEntry
    public boolean itemsEnabled = true;
    @SerialEntry
    public boolean playersEnabled = true;
    @SerialEntry
    public EspClient.DisplayMode displayMode = EspClient.DisplayMode.BOX;

    public EspConfig() {
    }

    static void load() {
        HANDLER.load();
        EspConfig config = HANDLER.instance();
        EspClient.enabled = config.enabled;
        EspClient.chestsEnabled = config.chestsEnabled;
        EspClient.mobsEnabled = config.mobsEnabled;
        EspClient.itemsEnabled = config.itemsEnabled;
        EspClient.playersEnabled = config.playersEnabled;
        EspClient.displayMode = config.displayMode;
    }

    static void save() {
        EspConfig config = HANDLER.instance();
        config.enabled = EspClient.enabled;
        config.chestsEnabled = EspClient.chestsEnabled;
        config.mobsEnabled = EspClient.mobsEnabled;
        config.itemsEnabled = EspClient.itemsEnabled;
        config.playersEnabled = EspClient.playersEnabled;
        config.displayMode = EspClient.displayMode;
        HANDLER.save();
    }
}