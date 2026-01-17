package net.runelite.client.plugins.zulrah.rotation;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahPhase;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

import java.util.ArrayList;
import java.util.List;


import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

public class ZulrahRotationFour extends ZulrahRotation
{
    public ZulrahRotationFour()
    {
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.EAST, ZulrahType.MAGIC, SafeLocation.TOP_EAST);
        add(ZulrahLocation.SOUTH, ZulrahType.RANGE, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.WEST, ZulrahType.MAGIC, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.NORTH, ZulrahType.MELEE, SafeLocation.PILLAR_EAST_OUTSIDE);
        add(ZulrahLocation.EAST, ZulrahType.RANGE, SafeLocation.PILLAR_EAST_OUTSIDE);
        add(ZulrahLocation.SOUTH, ZulrahType.RANGE, SafeLocation.PILLAR_WEST_OUTSIDE);
        add(ZulrahLocation.WEST, ZulrahType.MAGIC, SafeLocation.PILLAR_WEST_OUTSIDE);
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.NORTH, ZulrahType.MAGIC, SafeLocation.TOP_EAST);
        add(ZulrahLocation.EAST, ZulrahType.MAGIC, true, SafeLocation.TOP_EAST);
        add(ZulrahLocation.NORTH, ZulrahType.MAGIC, SafeLocation.TOP_EAST);
    }

    @Override
    public String toString()
    {
        return "Rotation 4";
    }
}