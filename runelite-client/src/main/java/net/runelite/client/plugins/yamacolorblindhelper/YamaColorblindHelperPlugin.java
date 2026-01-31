package net.runelite.client.plugins.yamacolorblindhelper;

import com.google.inject.Provides;

import java.util.*;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;

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
    private static final int VFX_YAMA_SHADOW_SPIKE_SPOTANIM_01 = 3259;
    private static final int VFX_YAMA_METEOR_SPOTANIM01 = 3270;
    private static final int YAMA_SPECIAL_ATTACK = 12145;
    public static final int VFX_YAMA_FLAMING_ROCK_PROJECTILE_01 = 3254;
    private String calloutText = null;
    private int calloutExpiryTick = -1;
    private static final Set<Integer> VOID_FLARE_NPC_IDS = Set.of(14179); // (you listed 14179 twice)
    private final Set<Integer> voidFlareIndexes = new HashSet<>();
    private WorldPoint dodgeTile;
    private int dodgeTileExpiryTick = -1;

    // Dedupe per projectile instance
    private int lastFlamingRockStartCycle = -1;

    WorldPoint getDodgeTile() { return dodgeTile; }

    int getDodgeTicksRemaining()
    {
        if (dodgeTileExpiryTick < 0) return -1;
        int rem = dodgeTileExpiryTick - client.getTickCount();
        return rem > 0 ? rem : -1;
    }
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

    @Subscribe
    public void onAnimationChanged(AnimationChanged e)
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

        if (npc.getAnimation() == YAMA_SPECIAL_ATTACK)
        {
            setCallout("WALK ON CIRCLE NOW", null);
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        Projectile p = event.getProjectile();
        if (p == null || p.getId() != VFX_YAMA_FLAMING_ROCK_PROJECTILE_01)
        {
            return;
        }

        // Optional: only care when Yama is present (prevents false positives elsewhere)
        if (!isYamaPresent())
        {
            return;
        }

        // Dedupe: don’t re-handle the same projectile instance
        int startCycle = p.getStartCycle();
        if (startCycle == lastFlamingRockStartCycle)
        {
            return;
        }
        lastFlamingRockStartCycle = startCycle;

        WorldPoint src = p.getSourcePoint();
        WorldPoint dst = p.getTargetPoint();
        if (src == null || dst == null)
        {
            return;
        }

        int dx = dst.getX() - src.getX();
        int dy = dst.getY() - src.getY();

        // Classify line direction
        LineType type = classify(dx, dy);

        WorldPoint player = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
        if (player == null)
        {
            return;
        }

        WorldPoint chosen = chooseDodgeTile(player, type);
        if (chosen != null)
        {
            dodgeTile = chosen;
            dodgeTileExpiryTick = client.getTickCount() + 2; // fast attack: show briefly (tweak as needed)
        }
    }

    private enum LineType { VERTICAL, HORIZONTAL, DIAGONAL_SLOPE_POS, DIAGONAL_SLOPE_NEG, UNKNOWN }

    private LineType classify(int dx, int dy)
    {
        if (dx == 0 && dy != 0) return LineType.VERTICAL;         // north-south
        if (dy == 0 && dx != 0) return LineType.HORIZONTAL;       // east-west

        if (dx != 0 && dy != 0 && Math.abs(dx) == Math.abs(dy))
        {
            // slope +1 vs -1
            return (dx * dy > 0) ? LineType.DIAGONAL_SLOPE_POS : LineType.DIAGONAL_SLOPE_NEG;
        }

        return LineType.UNKNOWN;
    }

    private WorldPoint chooseDodgeTile(WorldPoint player, LineType type)
    {
        // Candidates based on your rules:
        // Vertical (N-S): step left/right (W/E)
        // Horizontal (E-W): step up/down (N/S)
        // Diagonal: step one tile diagonally on the perpendicular diagonal
        WorldPoint[] candidates;

        switch (type)
        {
            case VERTICAL:
                candidates = new WorldPoint[] {
                        player.dx(1),  // right (east)
                        player.dx(-1)  // left (west)
                };
                break;

            case HORIZONTAL:
                candidates = new WorldPoint[] {
                        player.dy(1),  // up (north)
                        player.dy(-1)  // down (south)
                };
                break;

            case DIAGONAL_SLOPE_POS:
                // projectile line like NE<->SW, move on the opposite diagonal NW/SE
                candidates = new WorldPoint[] {
                        player.dx(1).dy(-1),   // SE
                        player.dx(-1).dy(1)    // NW
                };
                break;

            case DIAGONAL_SLOPE_NEG:
                // projectile line like NW<->SE, move on the opposite diagonal NE/SW
                candidates = new WorldPoint[] {
                        player.dx(1).dy(1),    // NE
                        player.dx(-1).dy(-1)   // SW
                };
                break;

            default:
                // Fallback: try a simple sidestep
                candidates = new WorldPoint[] { player.dx(1), player.dx(-1), player.dy(1), player.dy(-1) };
                break;
        }

        // Prefer a walkable tile
        for (WorldPoint wp : candidates)
        {
            if (isWalkable(wp))
            {
                return wp;
            }
        }
        // If none are walkable, still return first candidate so you at least show *something*
        return candidates.length > 0 ? candidates[0] : null;
    }

    private boolean isWalkable(WorldPoint wp)
    {
        if (wp == null) return false;

        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp == null) return false;

        CollisionData[] maps = client.getCollisionMaps();
        int plane = client.getPlane();
        if (maps == null || plane < 0 || plane >= maps.length) return true;

        CollisionData data = maps[plane];
        if (data == null) return true;

        int[][] flags = data.getFlags();
        if (flags == null) return true;

        // Convert local point to scene tile indices
        int sceneX = lp.getSceneX();
        int sceneY = lp.getSceneY();
        if (sceneX < 0 || sceneY < 0 || sceneX >= flags.length || sceneY >= flags[0].length) return false;

        int f = flags[sceneX][sceneY];

        // Basic blocked check
        return (f & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
    }

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private YamaColorblindOverlay overlay;
    @Inject private YamaColorblindConfig config;

    private final Set<Integer> yamaIndexes = new HashSet<>();

    // NPC full-area “active” timers (style-specific)
    private int magicNpcExpiryTick  = -1;
    private int rangedNpcExpiryTick = -1;
    private int lastCalloutTick = -1;
    private int lastCalloutGfx = -1;

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

        int now = client.getTickCount();
        int expiry = now + Math.max(1, config.flashTicks());

        // We’ll pick ONE callout per event, with priority.
        // Highest priority first.
        boolean sawMeteor = false;
        boolean sawShadow = false;
        boolean sawMagic  = false;
        boolean sawRange  = false;

        for (ActorSpotAnim s : npc.getSpotAnims())
        {
            int gfx = s.getId();

            // Keep your existing flare timers
            if (gfx == NPC_GFX_MAGIC)  { magicNpcExpiryTick  = expiry; sawMagic = true; }
            if (gfx == NPC_GFX_RANGED) { rangedNpcExpiryTick = expiry; sawRange = true; }

            if (gfx == VFX_YAMA_METEOR_SPOTANIM01)          { sawMeteor = true; }
            else if (gfx == VFX_YAMA_SHADOW_SPIKE_SPOTANIM_01) { sawShadow = true; }
        }

        // Choose the best callout (priority order)
        int chosenGfx = -1;
        String msg = null;
        Prayer pray = null;

        if (sawMeteor)
        {
            chosenGfx = VFX_YAMA_METEOR_SPOTANIM01;
            msg = "METEOR STRIKE, WALK ON RED, PRAY MAGIC";
            pray = Prayer.PROTECT_FROM_MAGIC;
        }
        else if (sawShadow)
        {
            chosenGfx = VFX_YAMA_SHADOW_SPIKE_SPOTANIM_01;
            msg = "SHADOW STOMP, WALK ON BLUE, PRAY RANGE";
            pray = Prayer.PROTECT_FROM_MISSILES;
        }
        else if (sawRange)
        {
            chosenGfx = NPC_GFX_RANGED;
            msg = "RANGED ATTACK";
            pray = Prayer.PROTECT_FROM_MISSILES;
        }
        else if (sawMagic)
        {
            chosenGfx = NPC_GFX_MAGIC;
            msg = "MAGIC ATTACK";
            pray = Prayer.PROTECT_FROM_MAGIC;
        }

        if (msg != null)
        {
            if (now == lastCalloutTick && chosenGfx == lastCalloutGfx)
            {
                return;
            }
            lastCalloutTick = now;
            lastCalloutGfx = chosenGfx;

            setCallout(msg, pray);
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

        if (dodgeTileExpiryTick > 0 && now >= dodgeTileExpiryTick)
        {
            dodgeTile = null;
            dodgeTileExpiryTick = -1;
        }
    }

    /* ---------------- Accessors for overlay ---------------- */

    boolean isMagicNpcActive()  { return magicNpcExpiryTick  > client.getTickCount(); }
    boolean isRangedNpcActive() { return rangedNpcExpiryTick > client.getTickCount(); }

    Map<LocalPoint, Integer> getRockTiles() { return rockTiles; }

    NPC getYamaNpc() { return getAnyYama(); }
    boolean isYamaPresent() { return !yamaIndexes.isEmpty(); }
}