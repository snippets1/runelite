package net.runelite.client.plugins.zulrah.overlays;

import com.google.inject.Inject;
import java.awt.*;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Point;
import net.runelite.client.plugins.zulrah.ZulrahInstance;
import net.runelite.client.plugins.zulrah.ZulrahPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.Perspective;

public class ZulrahPrayTextOverlay extends Overlay
{
    private final Client client;
    private final ZulrahPlugin plugin;

    @Inject
    ZulrahPrayTextOverlay(Client client, ZulrahPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        ZulrahInstance inst = plugin.getInstance();
        if (inst == null)
        {
            return null;
        }

        NPC zulrah = plugin.getNpcZulrah();
        if (zulrah == null)
        {
            return null;
        }

        Prayer p = plugin.getRecommendedPrayer();
        if (p == null)
        {
            return null;
        }

        String text = (p == Prayer.PROTECT_FROM_MAGIC) ? "PRAY MAGIC" : "PRAY RANGE";

        // Draw above zulrah
        Point loc = zulrah.getCanvasTextLocation(graphics, text, 40);
        if (loc != null)
        {
            graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 16f));
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, loc.getX() + 1, loc.getY() + 1);
            graphics.setColor(Color.WHITE);
            graphics.drawString(text, loc.getX(), loc.getY());
        }

        return null;
    }
}