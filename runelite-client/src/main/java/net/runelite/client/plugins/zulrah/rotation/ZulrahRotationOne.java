package net.runelite.client.plugins.zulrah.rotation;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

import net.runelite.client.plugins.zulrah.phase.SafeLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahLocation;
import net.runelite.client.plugins.zulrah.phase.ZulrahType;

public class ZulrahRotationOne extends ZulrahRotation
{
    public ZulrahRotationOne()
    {
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.NORTH, ZulrahType.MELEE, SafeLocation.TOP_EAST);
        add(ZulrahLocation.NORTH, ZulrahType.MAGIC, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.SOUTH, ZulrahType.RANGE, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.NORTH, ZulrahType.MELEE, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.WEST, ZulrahType.MAGIC, SafeLocation.PILLAR_WEST_INSIDE);
        add(ZulrahLocation.SOUTH, ZulrahType.RANGE, SafeLocation.PILLAR_EAST_OUTSIDE);
        add(ZulrahLocation.SOUTH, ZulrahType.MAGIC, SafeLocation.SOUTH_WEST);
        add(ZulrahLocation.WEST, ZulrahType.RANGE, true, SafeLocation.TOP_WEST);
        add(ZulrahLocation.NORTH, ZulrahType.MELEE, SafeLocation.TOP_WEST);
        add(ZulrahLocation.NORTH, ZulrahType.RANGE, SafeLocation.TOP_WEST);
    }

    @Override
    public String toString()
    {
        return "Rotation 1";
    }
}