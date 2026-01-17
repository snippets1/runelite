package net.runelite.client.plugins.zulrah.phase;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;

@Slf4j
public enum ZulrahType
{
    RANGE,
    MAGIC,
    MELEE;

    private static final int ZULRAH_RANGE = NpcID.ZULRAH;
    private static final int ZULRAH_MELEE = NpcID.ZULRAH_2043;
    private static final int ZULRAH_MAGIC = NpcID.ZULRAH_2044;

    public static ZulrahType valueOf(NPC zulrah)
    {
        int id = zulrah.getId();
        switch (id)
        {
            case ZULRAH_RANGE:
                return ZulrahType.RANGE;
            case ZULRAH_MELEE:
                return ZulrahType.MELEE;
            case ZULRAH_MAGIC:
                return ZulrahType.MAGIC;
        }
        log.debug("Unknown Zulrah Id: {}", id);
        return null;
    }
}
