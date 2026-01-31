package net.runelite.client.plugins.yamacolorblindhelper;

import com.google.inject.Provides;

import java.util.*;
import javax.inject.Inject;

import net.runelite.api.*;
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
    private static final int VFX_YAMA_SHADOW_SPIKE_SPOTANIM_01 = 3256;
    private static final int VFX_YAMA_METEOR_SPOTANIM01 = 3270;
    private String calloutText = null;
    private int calloutExpiryTick = -1;
    private static final Set<Integer> VOID_FLARE_NPC_IDS = Set.of(14179); // (you listed 14179 twice)
    private final Set<Integer> voidFlareIndexes = new HashSet<>();

    // Optional: show a recommended prayer icon somewhere else later
    private Prayer calloutPrayer = null;

    String getCalloutText() { return calloutText; }
    int getCalloutTicksRemaining()
    {
        if (calloutExpiryTick < 0) return -1;
        int rem = calloutExpiryTick - client.getTickCount();
        return rem > 0 ? rem : -1;
    }
    Prayer getCalloutPrayer() { return calloutPrayer; }

    private void setCallout(String text, Prayer prayer)
    {
        int now = client.getTickCount();
        int expiry = now + Math.max(1, config.flashTicks()); // reuse your tick duration setting
        calloutText = text;
        calloutPrayer = prayer;
        calloutExpiryTick = expiry;
    }

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
        calloutText = null;
        calloutPrayer = null;
        calloutExpiryTick = -1;
        voidFlareIndexes.clear();

    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        yamaIndexes.clear();
        rockTiles.clear();
        magicNpcExpiryTick = rangedNpcExpiryTick = -1;
        calloutText = null;
        calloutPrayer = null;
        calloutExpiryTick = -1;
        voidFlareIndexes.clear();

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



    @Subscribe
    public void onNpcSpawned(NpcSpawned e)
    {
        NPC npc = e.getNpc();

        if (isYama(npc))
        {
            yamaIndexes.add(npc.getIndex());
            return;
        }

        if (npc != null && VOID_FLARE_NPC_IDS.contains(npc.getId()))
        {
            voidFlareIndexes.add(npc.getIndex());
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e)
    {
        NPC npc = e.getNpc();
        if (npc == null) return;

        yamaIndexes.remove(npc.getIndex());
        voidFlareIndexes.remove(npc.getIndex());
    }

    public List<NPC> getVoidFlares()
    {
        List<NPC> out = new ArrayList<>();
        for (NPC n : client.getNpcs())
        {
            if (n != null && voidFlareIndexes.contains(n.getIndex()))
            {
                out.add(n);
            }
        }
        return out;
    }
    /* ---------------- Visible spotanims → cosmetic flare on Yama ---------------- */

    @Subscribe
    public void onGraphicChanged(GraphicChanged e)
    {
        Actor actor = e.getActor();
        if (!(actor instanceof NPC))
        {
            return;
        }

        NPC npc = (NPC) actor;
        if (!yamaIndexes.contains(npc.getIndex()))
        {
            return;
        }

        for (ActorSpotAnim s : npc.getSpotAnims())
        {
            int gfx = s.getId();

            // Keep your existing flare timers
            int now = client.getTickCount();
            int expiry = now + Math.max(1, config.flashTicks());

            if (gfx == NPC_GFX_MAGIC)  magicNpcExpiryTick  = expiry;
            if (gfx == NPC_GFX_RANGED) rangedNpcExpiryTick = expiry;

            // NEW: callouts
            if (gfx == NPC_GFX_RANGED)
            {
                setCallout("PRAY MAGIC, WALK NEAR BLUE", Prayer.PROTECT_FROM_MAGIC);
            }
            else if (gfx == NPC_GFX_MAGIC)
            {
                setCallout("PRAY MAGIC, WALK NEAR RED", Prayer.PROTECT_FROM_MAGIC);
            }
            else if (gfx == VFX_YAMA_SHADOW_SPIKE_SPOTANIM_01)
            {
                setCallout("SHADOW STOMP, PRAY RANGE, WALK NEAR BLUE", Prayer.PROTECT_FROM_MISSILES);
            }
            else if (gfx == VFX_YAMA_METEOR_SPOTANIM01)
            {
                setCallout("FIRE BALL, WALK ON RED, PRAY MAGIC", Prayer.PROTECT_FROM_MAGIC);
            }
        }
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
        if (calloutExpiryTick > 0 && client.getTickCount() >= calloutExpiryTick)
        {
            calloutText = null;
            calloutPrayer = null;
            calloutExpiryTick = -1;
        }
    }

    /* ---------------- Accessors for overlay ---------------- */

    boolean isMagicNpcActive()  { return magicNpcExpiryTick  > client.getTickCount(); }
    boolean isRangedNpcActive() { return rangedNpcExpiryTick > client.getTickCount(); }

    Map<LocalPoint, Integer> getRockTiles() { return rockTiles; }

    NPC getYamaNpc() { return getAnyYama(); }
    boolean isYamaPresent() { return !yamaIndexes.isEmpty(); }
}