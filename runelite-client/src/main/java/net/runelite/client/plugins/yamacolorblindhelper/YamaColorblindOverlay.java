package net.runelite.client.plugins.yamacolorblindhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class YamaColorblindOverlay extends Overlay
{
    private final Client client;
    private final YamaColorblindHelperPlugin plugin;
    private final YamaColorblindConfig config;

    @Inject
    public YamaColorblindOverlay(Client client, YamaColorblindHelperPlugin plugin, YamaColorblindConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public java.awt.Dimension render(Graphics2D g)
    {
        int tick = client.getTickCount();
        float pulseMul = config.pulse() ? (0.85f + 0.15f * ((tick & 1) == 0 ? 1f : 0f)) : 1f;

        // 1) Yama full-footprint flare (if active)
        if (plugin.isYamaPresent() && config.enableOnNpc() && config.npcFullArea())
        {
            NPC yama = plugin.getYamaNpc();
            if (yama != null)
            {
                int size = 1;
                NPCComposition comp = yama.getTransformedComposition();
                if (comp == null) comp = yama.getComposition();
                if (comp != null) size = Math.max(1, comp.getSize());

                LocalPoint npcLp = yama.getLocalLocation();
                if (npcLp != null)
                {
                    if (plugin.isMagicNpcActive())
                        drawArea(g, npcLp, size, pulse(resolveMagicColor(), pulseMul), resolveOutlineColor());
                    if (plugin.isRangedNpcActive())
                        drawArea(g, npcLp, size, pulse(resolveRangeColor(), pulseMul), resolveOutlineColor());
                }
            }
        }

        // 2) Rockfall tiles (ground GFX → tile highlight)
        if (config.enableRockTiles())
            drawRings(g, plugin.getRockTiles(), pulse(resolveRockFill(), pulseMul), resolveRockOutline());

        return null;
    }

    /* ---------- Color resolution: preset or custom ---------- */

    private Color resolveMagicColor()
    {
        if (config.colorMode() == YamaColorblindConfig.ColorMode.CUSTOM) return config.magicColor();
        switch (config.palettePreset())
        {
            case DEUTERANOPIA: return new Color(0, 127, 255, 110);    // bright blue
            case PROTANOPIA:   return new Color(0, 200, 200, 110);    // cyan
            case TRITANOPIA:   return new Color(255, 85, 170, 110);   // magenta-ish
            case HIGH_CONTRAST:return new Color(255, 255, 255, 130);  // white fill
            case NORMAL:
            default:           return new Color(255, 80, 80, 110);    // vivid red
        }
    }

    private Color resolveRangeColor()
    {
        if (config.colorMode() == YamaColorblindConfig.ColorMode.CUSTOM) return config.rangeColor();
        switch (config.palettePreset())
        {
            case DEUTERANOPIA: return new Color(255, 215, 0, 110);    // gold
            case PROTANOPIA:   return new Color(255, 160, 0, 110);    // orange
            case TRITANOPIA:   return new Color(0, 200, 0, 110);      // green
            case HIGH_CONTRAST:return new Color(0, 0, 0, 130);        // black fill
            case NORMAL:
            default:           return new Color(170, 100, 255, 110);  // vivid purple
        }
    }

    private Color resolveOutlineColor()
    {
        if (config.colorMode() == YamaColorblindConfig.ColorMode.CUSTOM) return config.outlineColor();
        switch (config.palettePreset())
        {
            case HIGH_CONTRAST: return new Color(255, 255, 0, 220); // yellow outline
            default:            return new Color(255, 255, 255, 220);
        }
    }

    private Color resolveRockFill()
    {
        if (config.colorMode() == YamaColorblindConfig.ColorMode.CUSTOM) return config.rockFill();
        switch (config.palettePreset())
        {
            case DEUTERANOPIA: return new Color(255, 255, 255, 80);  // white-ish
            case PROTANOPIA:   return new Color(255, 200, 0, 80);    // amber
            case TRITANOPIA:   return new Color(255, 120, 120, 80);  // coral
            case HIGH_CONTRAST:return new Color(0, 255, 255, 110);   // cyan
            case NORMAL:
            default:           return new Color(255, 170, 0, 80);    // amber
        }
    }

    private Color resolveRockOutline()
    {
        if (config.colorMode() == YamaColorblindConfig.ColorMode.CUSTOM) return config.rockOutline();
        switch (config.palettePreset())
        {
            case HIGH_CONTRAST: return new Color(255, 0, 0, 230);    // red
            default:            return new Color(255, 255, 255, 230);
        }
    }

    /* ---------- Drawing helpers ---------- */

    private Color pulse(Color base, float mul)
    {
        int a = Math.min(255, Math.round(base.getAlpha() * mul));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }

    private void drawRings(Graphics2D g, Map<LocalPoint, Integer> rings, Color fill, Color outline)
    {
        if (rings.isEmpty()) return;
        for (LocalPoint lp : rings.keySet())
        {
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly == null) continue;

            g.setColor(fill);
            g.fillPolygon(poly);

            g.setColor(outline);
            g.setStroke(new BasicStroke(3f));
            g.drawPolygon(poly);
        }
    }

    private void drawArea(Graphics2D g, LocalPoint lp, int size, Color fill, Color outline)
    {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, size);
        if (poly == null) return;

        g.setColor(fill);
        g.fillPolygon(poly);

        g.setColor(outline);
        g.setStroke(new BasicStroke(3.5f));
        g.drawPolygon(poly);
    }
}