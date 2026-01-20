package net.runelite.client.plugins.yamacolorblindhelper;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Yama Colorblind Helper",
        description = "Accessibility-focused, cosmetic highlights for Yama's visible effects (no prediction, no advice).",
        tags = {"yama","accessibility","colorblind","overlay"}
)
public class YamaColorblindHelperPlugin extends Plugin
{
    private static final Set<Integer> YAMA_NPC_IDS = Set.of(14176);

    // Yama arm/actor spotanims (visible on NPC)
    private static final int NPC_GFX_MAGIC  = 3246;
    private static final int NPC_GFX_RANGED = 3243;

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private YamaColorblindOverlay overlay;
    @Inject private YamaColorblindConfig config;

    private final Set<Integer> yamaIndexes = new HashSet<>();

    // NPC full-area “active” timers (style-specific)
    private int magicNpcExpiryTick  = -1;
    private int rangedNpcExpiryTick = -1;

    // Rockfall tiles (instance-safe, scene-local)
    private final Map<LocalPoint, Integer> rockTiles = new HashMap<>();

    @Provides
    YamaColorblindConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(YamaColorblindConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        yamaIndexes.clear();
        rockTiles.clear();
        magicNpcExpiryTick = rangedNpcExpiryTick = -1;
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        yamaIndexes.clear();
        rockTiles.clear();
        magicNpcExpiryTick = rangedNpcExpiryTick = -1;
    }

    /* ---------------- Presence tracking ---------------- */

    private static boolean isYama(NPC npc)
    {
        return npc != null && YAMA_NPC_IDS.contains(npc.getId());
    }

    private NPC getAnyYama()
    {
        for (NPC n : client.getNpcs())
        {
            if (n != null && yamaIndexes.contains(n.getIndex()))
                return n;
        }
        return null;
    }

    @Subscribe public void onNpcSpawned(NpcSpawned e)   { if (isYama(e.getNpc())) yamaIndexes.add(e.getNpc().getIndex()); }
    @Subscribe public void onNpcDespawned(NpcDespawned e){ yamaIndexes.remove(e.getNpc().getIndex()); }

    /* ---------------- Visible spotanims → cosmetic flare on Yama ---------------- */

    @Subscribe
    public void onGraphicChanged(GraphicChanged e)
    {
        Actor actor = e.getActor();

        if (actor instanceof NPC)
        {
            NPC npc = (NPC) actor;
            if (!yamaIndexes.contains(npc.getIndex())) return;

            for (ActorSpotAnim s : npc.getSpotAnims())
            {
                int gfx = s.getId();
                int now = client.getTickCount();
                int expiry = now + Math.max(1, config.flashTicks());

                if (gfx == NPC_GFX_MAGIC)  magicNpcExpiryTick  = expiry;
                if (gfx == NPC_GFX_RANGED) rangedNpcExpiryTick = expiry;
            }
        }
        // (Intentionally ignoring player spotanims per design)
    }

    /* ---------------- Rockfall tiles from ground GFX ---------------- */

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e)
    {
        if (!config.enableRockTiles()) return;

        GraphicsObject go = e.getGraphicsObject();
        if (go.getId() != config.rockGfxId()) return;

        LocalPoint lp = go.getLocation(); // scene-local; instance-safe to draw
        if (lp == null) return;

        int expiry = client.getTickCount() + Math.max(1, config.rockTileTicks());
        rockTiles.put(lp, expiry);
    }

    @Subscribe
    public void onGameTick(GameTick t)
    {
        int now = client.getTickCount();
        rockTiles.entrySet().removeIf(e -> e.getValue() <= now);

        if (magicNpcExpiryTick  > 0 && now > magicNpcExpiryTick)  magicNpcExpiryTick  = -1;
        if (rangedNpcExpiryTick > 0 && now > rangedNpcExpiryTick) rangedNpcExpiryTick = -1;
    }

    /* ---------------- Accessors for overlay ---------------- */

    boolean isMagicNpcActive()  { return magicNpcExpiryTick  > client.getTickCount(); }
    boolean isRangedNpcActive() { return rangedNpcExpiryTick > client.getTickCount(); }

    Map<LocalPoint, Integer> getRockTiles() { return rockTiles; }

    NPC getYamaNpc() { return getAnyYama(); }
    boolean isYamaPresent() { return !yamaIndexes.isEmpty(); }
}