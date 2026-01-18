package net.runelite.client.plugins.vorkath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SpawnOverlay extends Overlay
{
	private final VorkathPlugin plugin;

	@Inject
	public SpawnOverlay(final VorkathPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getZombifiedSpawn() != null)
		{
			OverlayUtil.renderActorOverlayImage(graphics, plugin.getZombifiedSpawn(), VorkathPlugin.SPAWN, Color.green, 10);
		}

		return null;
	}
}