package net.runelite.client.plugins.zulrah.overlays;

import com.google.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.zulrah.ZulrahPlugin;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ComponentConstants;

public class ZulrahPrayerInfoboxOverlay extends Overlay
{
    private static final Dimension DIMENSION = new Dimension(30, 30);
    private static final Color COLOR_OFF = new Color(150, 0, 0, 150);

    private final Client client;
    private final ZulrahPlugin plugin;
    private final SpriteManager spriteManager;

    private final PanelComponent panelComponent = new PanelComponent();
    private final InfoBoxComponent infoBoxComponent = new InfoBoxComponent();

    private BufferedImage magicSprite;
    private BufferedImage rangeSprite;
    private BufferedImage meleeSprite;

    @Inject
    public ZulrahPrayerInfoboxOverlay(Client client, ZulrahPlugin plugin, SpriteManager spriteManager)
    {
        this.client = client;
        this.plugin = plugin;
        this.spriteManager = spriteManager;

        infoBoxComponent.setPreferredSize(DIMENSION);
        infoBoxComponent.setColor(Color.WHITE);

        panelComponent.setPreferredSize(DIMENSION);
        panelComponent.setBorder(new Rectangle(0, 0, 0, 0));
        panelComponent.getChildren().add(infoBoxComponent);

        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.BOTTOM_RIGHT);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Only show during the fight
        if (plugin.getInstance() == null)
        {
            return null;
        }

        // Use recommended prayer (during Jad, you should already be setting this to the "next" prayer)
        Prayer prayer = plugin.getRecommendedPrayer();
        if (prayer == null)
        {
            return null;
        }

        infoBoxComponent.setImage(getPrayerSprite(prayer));

        // Show Jad countdown ticks if we have them
        int ticks = plugin.getJadTicksUntilNextAttack();
        infoBoxComponent.setText(ticks >= 0 ? String.valueOf(ticks) : "");

        // Background indicates if the correct prayer is currently active
        infoBoxComponent.setBackgroundColor(
                client.isPrayerActive(prayer) ? ComponentConstants.STANDARD_BACKGROUND_COLOR : COLOR_OFF
        );

        return panelComponent.render(graphics);
    }

    private BufferedImage getPrayerSprite(Prayer prayer)
    {
        switch (prayer)
        {
            case PROTECT_FROM_MAGIC:
                if (magicSprite == null)
                {
                    magicSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0));
                }
                return magicSprite;

            case PROTECT_FROM_MISSILES:
                if (rangeSprite == null)
                {
                    rangeSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0));
                }
                return rangeSprite;

            case PROTECT_FROM_MELEE:
                if (meleeSprite == null)
                {
                    meleeSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MELEE, 0));
                }
                return meleeSprite;

            default:
                return null;
        }
    }

    private static BufferedImage scaleSprite(BufferedImage bufferedImage)
    {
        if (bufferedImage == null)
        {
            return null;
        }

        double width = bufferedImage.getWidth();
        double height = bufferedImage.getHeight();

        double scalex = (DIMENSION.width - 5) / width;
        double scaley = (DIMENSION.height - 5) / height;
        double scale = Math.min(scalex, scaley);

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = scaledImage.createGraphics();
        g.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return scaledImage;
    }
}