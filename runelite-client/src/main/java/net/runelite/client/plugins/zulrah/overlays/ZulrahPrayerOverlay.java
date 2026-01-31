package net.runelite.client.plugins.zulrah.overlays;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.VarClientInt;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.zulrah.ZulrahPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;

public class ZulrahPrayerOverlay extends Overlay
{
    private static final int PRAYER_TAB_ID = 5;

    // These match the example you posted
    private static final int WIDGET_ID_PRAYER_GROUP = 541;
    private static final int WIDGET_ID_PRAYER_PROTECT_MAGIC = 21;
    private static final int WIDGET_ID_PRAYER_PROTECT_MISSILES = 22;
    private static final int WIDGET_ID_PRAYER_PROTECT_MELEE = 23;

    private static final Color OUTLINE = new Color(255, 255, 0, 220);
    private static final Color FILL = new Color(255, 255, 0, 40);

    private final Client client;
    private final ZulrahPlugin plugin;

    @Inject
    public ZulrahPrayerOverlay(Client client, ZulrahPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != PRAYER_TAB_ID)
        {
            return null;
        }

        Prayer pray = plugin.getRecommendedPrayer();
        if (pray == null)
        {
            return null;
        }

        Widget w = getPrayerWidget(client, pray);
        if (w == null || w.isHidden())
        {
            return null;
        }

        Rectangle bounds = w.getBounds();
        if (bounds == null)
        {
            return null;
        }

        // Highlight (optional if already active)
        if (!client.isPrayerActive(pray))
        {
            graphics.setColor(FILL);
            graphics.fill(bounds);

            graphics.setColor(OUTLINE);
            graphics.setStroke(new BasicStroke(2));
            graphics.draw(bounds);
        }

        // Draw countdown on top so it stays readable
        int ticks = plugin.getJadTicksUntilNextAttack();
        if (ticks >= 0)
        {
            String text = String.valueOf(ticks);

            int x = (int) (bounds.getX() + bounds.getWidth() / 2);
            int y = (int) (bounds.getY() + bounds.getHeight() / 2);

            graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 16f));

            graphics.setColor(Color.BLACK);
            graphics.drawString(text, x - 4 + 1, y + 6 + 1);

            graphics.setColor(ticks <= 1 ? Color.RED : Color.WHITE);
            graphics.drawString(text, x - 4, y + 6);
        }

        return null;
    }

    private static Widget getPrayerWidget(Client client, Prayer prayer)
    {
        int childId;
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                childId = WIDGET_ID_PRAYER_PROTECT_MAGIC;
                break;
            case PROTECT_FROM_MISSILES:
                childId = WIDGET_ID_PRAYER_PROTECT_MISSILES;
                break;
            case PROTECT_FROM_MELEE:
                childId = WIDGET_ID_PRAYER_PROTECT_MELEE;
                break;
            default:
                return null;
        }

        return client.getWidget(WIDGET_ID_PRAYER_GROUP, childId);
    }
}