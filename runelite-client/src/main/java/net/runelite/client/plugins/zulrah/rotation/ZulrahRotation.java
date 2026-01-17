package net.runelite.client.plugins.zulrah.rotation;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahPhase;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

import java.util.ArrayList;
import java.util.List;

public class ZulrahRotation
{
    private List<ZulrahPhase> rotation = new ArrayList<>();

    public void add(ZulrahLocation zulrahLocation, ZulrahType type, boolean jad, SafeLocation safeLocation)
    {
        rotation.add(new ZulrahPhase(zulrahLocation, type, jad, safeLocation));
    }

    public void add(ZulrahLocation zulrahLocation, ZulrahType type, SafeLocation safeLocation)
    {
        add(zulrahLocation, type, false, safeLocation);
    }

    public ZulrahPhase getPhase(int stage)
    {
        if (stage >= rotation.size())
        {
            return null;
        }
        return rotation.get(stage);
    }

    public boolean stageEquals(int stage, ZulrahPhase phase)
    {

        return phase != null && phase.equals(getPhase(stage));
    }

    public boolean canReset(int stage)
    {
        return stage >= rotation.size();
    }
}