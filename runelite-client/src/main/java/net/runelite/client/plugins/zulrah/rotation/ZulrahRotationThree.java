package net.runelite.client.plugins.zulrah.rotation;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

public class ZulrahRotationThree extends ZulrahRotation
{
    public ZulrahRotationThree()
    {
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.EAST, ZulrahType.RANGE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.NORTH, ZulrahType.MELEE, SafeLocation.TOP_WEST);
        add(ZulrahLocation.WEST, ZulrahType.MAGIC, SafeLocation.WEST);
        add(ZulrahLocation.SOUTH, ZulrahType.RANGE, SafeLocation.SOUTH_EAST);
        add(ZulrahLocation.EAST, ZulrahType.MAGIC, SafeLocation.PILLAR_EAST_OUTSIDE);
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.PILLAR_WEST_OUTSIDE);
        add(ZulrahLocation.WEST, ZulrahType.RANGE, SafeLocation.PILLAR_WEST_OUTSIDE);
        add(ZulrahLocation.NORTH, ZulrahType.MAGIC, SafeLocation.TOP_EAST);
        add(ZulrahLocation.EAST, ZulrahType.MAGIC, true, SafeLocation.TOP_EAST);
    }

    @Override
    public String toString()
    {
        return "Rotation 3";
    }
}