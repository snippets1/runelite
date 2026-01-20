package net.runelite.client.plugins.yamacolorblindhelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("yama-colorblind-helper")
public interface YamaColorblindConfig extends Config
{
    /* ================== Core ================== */
    @ConfigSection(
            name = "Core",
            description = "Cosmetic highlights of Yama's visible attack effects",
            position = 0
    )
    String coreSection = "coreSection";

    @ConfigItem(
            keyName = "enableOnNpc",
            name = "Flare on Yama",
            description = "Draw a flare on Yama's SW true tile when his arm spotanim appears",
            position = 1,
            section = coreSection
    )
    default boolean enableOnNpc() { return true; }

    @ConfigItem(
            keyName = "npcFullArea",
            name = "Full-size flare on Yama",
            description = "Outline Yama's full occupied tile area (instead of only the true tile)",
            position = 2,
            section = coreSection
    )
    default boolean npcFullArea() { return true; }

    @ConfigItem(
            keyName = "flashTicks",
            name = "Flare duration (ticks)",
            description = "How long the Yama flare remains after the spotanim appears",
            position = 3,
            section = coreSection
    )
    default int flashTicks() { return 4; }

    @ConfigItem(
            keyName = "pulse",
            name = "Pulse effect",
            description = "Subtle tick-pulse in flare opacity",
            position = 4,
            section = coreSection
    )
    default boolean pulse() { return true; }

    /* ================== Palette ================== */
    @ConfigSection(
            name = "Palette",
            description = "Choose a preset for color vision / contrast, or use custom colors",
            position = 10
    )
    String paletteSection = "paletteSection";

    enum PalettePreset { NORMAL, DEUTERANOPIA, PROTANOPIA, TRITANOPIA, HIGH_CONTRAST }
    enum ColorMode { PRESET, CUSTOM }

    @ConfigItem(
            keyName = "colorMode",
            name = "Color mode",
            description = "Preset = use palette; Custom = use pickers below",
            position = 11,
            section = paletteSection
    )
    default ColorMode colorMode() { return ColorMode.PRESET; }

    @ConfigItem(
            keyName = "palettePreset",
            name = "Palette preset",
            description = "Color preset for visibility/accessibility",
            position = 12,
            section = paletteSection
    )
    default PalettePreset palettePreset() { return PalettePreset.NORMAL; }

    // Custom pickers (used when Color mode = CUSTOM)
    @Alpha @ConfigItem(
            keyName = "magicColor",
            name = "Magic color (custom)",
            description = "Used when Color mode is CUSTOM",
            position = 13,
            section = paletteSection
    )
    default Color magicColor() { return new Color(255, 80, 80, 110); } // vivid red

    @Alpha @ConfigItem(
            keyName = "rangeColor",
            name = "Ranged color (custom)",
            description = "Used when Color mode is CUSTOM",
            position = 14,
            section = paletteSection
    )
    default Color rangeColor() { return new Color(170, 100, 255, 110); } // vivid purple

    @Alpha @ConfigItem(
            keyName = "outlineColor",
            name = "Yama flare outline (custom)",
            description = "Used when Color mode is CUSTOM",
            position = 15,
            section = paletteSection
    )
    default Color outlineColor() { return new Color(255, 255, 255, 220); }

    /* ================== Rockfall Tiles ================== */
    @ConfigSection(
            name = "Rockfall Tiles",
            description = "Highlight tiles that the game already marks via rockfall ground GFX (no prediction).",
            position = 20
    )
    String rockSection = "rockSection";

    @ConfigItem(
            keyName = "enableRockTiles",
            name = "Enable rockfall tiles",
            description = "Draw a highlight on ground-GFX tiles (cosmetic only)",
            position = 21,
            section = rockSection
    )
    default boolean enableRockTiles() { return true; }

    @ConfigItem(
            keyName = "rockGfxId",
            name = "Rock ground GFX id",
            description = "Ground graphics id to treat as rockfall (default 3262)",
            position = 22,
            section = rockSection
    )
    default int rockGfxId() { return 3262; }

    @ConfigItem(
            keyName = "rockTileTicks",
            name = "Tile duration (ticks)",
            description = "How long a rockfall tile remains highlighted",
            position = 23,
            section = rockSection
    )
    default int rockTileTicks() { return 4; }

    @Alpha @ConfigItem(
            keyName = "rockFill",
            name = "Rock tile fill (custom)",
            description = "Used when Color mode is CUSTOM",
            position = 24,
            section = rockSection
    )
    default Color rockFill() { return new Color(255, 170, 0, 80); } // amber

    @Alpha @ConfigItem(
            keyName = "rockOutline",
            name = "Rock tile outline (custom)",
            description = "Used when Color mode is CUSTOM",
            position = 25,
            section = rockSection
    )
    default Color rockOutline() { return new Color(255, 255, 255, 230); }
}