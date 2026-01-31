package net.runelite.client.plugins.zulrah;

/*
 * Copyright (c) 2018, Devin French <https://github.com/devinfrench>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.inject.Binder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.zulrah.overlays.*;
import net.runelite.client.plugins.zulrah.phase.ZulrahPhase;
import net.runelite.client.plugins.zulrah.rotation.*;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Zulrah",
        description = "Make Zulrah fight trivial.",
        tags = {"combat", "overlay", "pve", "pvm"}
)
@Slf4j
public class ZulrahPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ZulrahTileOverlay tileOverlay;

    @Inject
    private ZulrahPrayerInfoboxOverlay prayerInfoboxOverlay;

    @Inject
    private ZulrahRotationOverlay rotationOverlay;

    @Inject
    private ZulrahPrayTextOverlay prayTextOverlay;

    @Inject
    private ZulrahPrayerOverlay prayerOverlay;

    private ZulrahRotation[] rotations = new ZulrahRotation[]
            {
                    new ZulrahRotationOne(),
                    new ZulrahRotationTwo(),
                    new ZulrahRotationThree(),
                    new ZulrahRotationFour()
            };

    @Getter private ZulrahInstance instance;

    @Getter private NPC npcZulrah;

    @Getter private Prayer recommendedPrayer;
    private Prayer nextPrayer;

    private static final int ZULRAH_PROJ_MAGIC = 1046;
    private static final int ZULRAH_PROJ_RANGE = 1044;

    private Prayer projectilePrayer;          // last prayer implied by projectile
    private int projectilePrayerTick = -1;
    private Prayer lastJadIncomingPrayer;     // prayer that would block the last projectile fired
    private int lastJadAttackTick = -1;       // tick when the last jad projectile was fired
    private int jadIntervalTicks = 4;         // dynamically learned, default 4
    private int lastProcessedProjStartCycleMagic = -1;
    private int lastProcessedProjStartCycleRange = -1;

    public Prayer getNextRecommendedPrayer() {
        return nextPrayer;
    }

    private Prayer mapProjectileToPrayer(Projectile p) {
        switch (p.getId()) {
            case ZULRAH_PROJ_MAGIC:
                return Prayer.PROTECT_FROM_MAGIC;
            case ZULRAH_PROJ_RANGE:
                return Prayer.PROTECT_FROM_MISSILES;
            default:
                return null;
        }
    }

    public int getJadTicksUntilNextAttack()
    {
        if (instance == null || instance.getPhase() == null || !instance.getPhase().isJad())
        {
            return -1;
        }
        if (lastJadAttackTick < 0)
        {
            return -1;
        }

        int now = client.getTickCount();

        if (now - lastJadAttackTick > jadIntervalTicks * 2)
        {
            return -1;
        }

        int nextAttackTick = lastJadAttackTick + jadIntervalTicks;
        return Math.max(0, nextAttackTick - now);
    }

    public Prayer getJadNextPrayer()
    {
        if (instance == null || instance.getPhase() == null || !instance.getPhase().isJad())
        {
            return null;
        }
        if (lastJadIncomingPrayer == null)
        {
            return null; // haven’t seen the first projectile yet
        }
        return invertedJadPrayer(lastJadIncomingPrayer);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ZulrahTileOverlay.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(tileOverlay);
        overlayManager.add(rotationOverlay);
        overlayManager.add(prayerOverlay);
        overlayManager.add(prayerInfoboxOverlay);
        overlayManager.add(prayTextOverlay);
        npcZulrah = null;
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(tileOverlay);
        overlayManager.remove(rotationOverlay);
        overlayManager.remove(prayerOverlay);
        overlayManager.remove(prayTextOverlay);
        overlayManager.remove(prayerInfoboxOverlay);


        instance = null;
        npcZulrah = null;
        recommendedPrayer = null;
        nextPrayer = null;
        projectilePrayer = null;
        projectilePrayerTick = -1;
    }


    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (isNpcZulrah(npc.getId())) {
            npcZulrah = npc;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if (isNpcZulrah(event.getNpc().getId())) {
            projectilePrayer = null;
            projectilePrayerTick = -1;
            recommendedPrayer = null;
            nextPrayer = null;
            instance = null;
            npcZulrah = null;
        }
    }

    public static boolean isNpcZulrah(int npcId) {
        return npcId == NpcID.ZULRAH ||
                npcId == NpcID.ZULRAH_2043 ||
                npcId == NpcID.ZULRAH_2044;
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        if (npcZulrah == null || instance == null)
        {
            return;
        }

        Projectile p = event.getProjectile();
        Prayer incoming = incomingPrayerFromProjectile(p.getId());
        if (incoming == null)
        {
            return;
        }

        // Dedupe: only process once per projectile instance using startCycle
        int startCycle = p.getStartCycle();
        if (p.getId() == ZULRAH_PROJ_MAGIC)
        {
            if (startCycle == lastProcessedProjStartCycleMagic) return;
            lastProcessedProjStartCycleMagic = startCycle;
        }
        else if (p.getId() == ZULRAH_PROJ_RANGE)
        {
            if (startCycle == lastProcessedProjStartCycleRange) return;
            lastProcessedProjStartCycleRange = startCycle;
        }

        int tick = client.getTickCount();

        // Always store latest projectile prayer (useful outside Jad too)
        projectilePrayer = incoming;
        projectilePrayerTick = tick;

        // If we’re in Jad phase, learn interval + store last jad attack tick
        ZulrahPhase current = instance.getPhase();
        if (current != null && current.isJad())
        {
            if (lastJadAttackTick >= 0)
            {
                int interval = tick - lastJadAttackTick;
                // Filter out nonsense intervals from desync / start/end of phase
                if (interval >= 2 && interval <= 8)
                {
                    jadIntervalTicks = interval; // or average it if you want smoother
                }
            }

            lastJadAttackTick = tick;
            lastJadIncomingPrayer = incoming;
        }
    }


    private Prayer getRecentProjectilePrayer(int maxAgeTicks) {
        if (projectilePrayer == null || projectilePrayerTick < 0) {
            return null;
        }
        int age = client.getTickCount() - projectilePrayerTick;
        return age <= maxAgeTicks ? projectilePrayer : null;
    }

    private Prayer prayerForPhase(ZulrahPhase phase) {
        if (phase == null) {
            return null;
        }
        switch (phase.getType()) {
            case MAGIC:
                return Prayer.PROTECT_FROM_MAGIC;
            case RANGE:
                return Prayer.PROTECT_FROM_MISSILES;
            default:
                return null; // melee: optional
        }
    }

    private Prayer incomingPrayerFromProjectile(int projectileId)
    {
        switch (projectileId)
        {
            case ZULRAH_PROJ_MAGIC: return Prayer.PROTECT_FROM_MAGIC;
            case ZULRAH_PROJ_RANGE: return Prayer.PROTECT_FROM_MISSILES;
            default: return null;
        }
    }

    private Prayer invertedJadPrayer(Prayer incoming)
    {
        if (incoming == Prayer.PROTECT_FROM_MAGIC) return Prayer.PROTECT_FROM_MISSILES;
        if (incoming == Prayer.PROTECT_FROM_MISSILES) return Prayer.PROTECT_FROM_MAGIC;
        return null;
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (npcZulrah == null) {
            if (instance != null) {
                log.debug("Zulrah encounter has ended.");
                instance = null;
            }
            return;
        }

        if (instance == null) {
            instance = new ZulrahInstance(npcZulrah);
            log.debug("Zulrah encounter has started.");
        }

        ZulrahPhase phase = ZulrahPhase.valueOf(instance.getStartWorldPoint(), npcZulrah);
        if (instance.getPhase() == null) {
            instance.setPhase(phase);
        } else if (!instance.getPhase().equals(phase)) {
            log.debug("Zulrah phase has moved from {} -> {}, Stage: {}", instance.getPhase(), phase, instance.getStage() + 1);
            instance.setPhase(phase);
            instance.nextStage();
        }

        ZulrahRotation rotation = instance.getRotation();
        if (rotation == null) {
            int potential = 0;
            for (ZulrahRotation r : rotations) {
                if (r.stageEquals(instance.getStage(), instance.getPhase())) {
                    potential++;
                    rotation = r;
                }
            }
            if (potential == 1) {
                log.debug("Zulrah rotation found: {}", rotation);
                instance.setRotation(rotation);
            }
        } else if (rotation.canReset(instance.getStage())) {
            instance.reset();
        }

        ZulrahPhase current = instance.getPhase();
        ZulrahPhase next = instance.getNextPhase();

        Prayer phaseNextPrayer = prayerForPhase(next);


        Prayer phasePrayer = prayerForPhase(current);
        Prayer projIncoming = getRecentProjectilePrayer(2);

        if (current != null && current.isJad())
        {
            Prayer jadNext = getJadNextPrayer();
            recommendedPrayer = (jadNext != null) ? jadNext : phasePrayer;
        }
        if (current == null || !current.isJad())
        {
            lastJadAttackTick = -1;
            lastJadIncomingPrayer = null;
            jadIntervalTicks = 4;
        }
        else
        {
            recommendedPrayer = (projIncoming != null) ? projIncoming : phasePrayer;
        }

        nextPrayer = prayerForPhase(next);


    }
}