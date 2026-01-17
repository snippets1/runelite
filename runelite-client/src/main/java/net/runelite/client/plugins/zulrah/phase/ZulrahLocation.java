package net.runelite.client.plugins.zulrah.phase;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public enum ZulrahLocation
{
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public static ZulrahLocation valueOf(WorldPoint start, NPC zulrah)
    {
        WorldPoint current = zulrah.getWorldLocation();
        int dx = start.getX() - current.getX();
        int dy = start.getY() - current.getY();
        if (dx == -10 && dy == 2)
        {
            return ZulrahLocation.EAST;
        }
        else if (dx == 10 && dy == 2)
        {
            return ZulrahLocation.WEST;
        }
        else if (dx == 0 && dy == 11)
        {
            return ZulrahLocation.SOUTH;
        }
        else if (dx == 0 && dy == 0)
        {
            return ZulrahLocation.NORTH;
        }
        else
        {
            log.debug("Unknown Zulrah location dx: {}, dy: {}", dx, dy);
            return null;
        }
    }
}