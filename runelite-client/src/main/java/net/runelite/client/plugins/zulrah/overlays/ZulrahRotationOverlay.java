package net.runelite.client.plugins.zulrah.overlays;

import com.google.inject.Inject;
import net.runelite.client.plugins.zulrah.ZulrahInstance;
import net.runelite.client.plugins.zulrah.ZulrahPlugin;
import net.runelite.client.plugins.zulrah.rotation.ZulrahRotation;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Dimension;
import java.awt.Graphics2D;

public class ZulrahRotationOverlay extends Overlay
{
    private final PanelComponent panelComponent = new PanelComponent();
    private ZulrahPlugin plugin;

    @Inject
    ZulrahRotationOverlay(ZulrahPlugin plugin)
    {
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        ZulrahInstance instance = plugin.getInstance();
        if (instance == null)
        {
            return null;
        }
        ZulrahRotation rotation = instance.getRotation();
        if (rotation == null)
        {
            return null;
        }
        panelComponent.getChildren().add(TitleComponent.builder().text("Zulrah " + rotation.toString()).build());
        return panelComponent.render(graphics);
    }
}