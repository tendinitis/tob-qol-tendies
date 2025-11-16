package com.tob_qol_tendies;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tob_qol_tendies")
public interface ExampleConfig extends Config
{
    @ConfigItem(
            keyName = "bloatCounterX",
            name = "Bloat Counter X Offset",
            description = "Horizontal position of the Bloat counter"
    )
    default int bloatCounterX() { return 500; }

    @ConfigItem(
            keyName = "bloatCounterY",
            name = "Bloat Counter Y Offset",
            description = "Vertical position of the Bloat counter"
    )
    default int bloatCounterY() { return 200; }

    @ConfigItem(
            keyName = "bloatCounterSize",
            name = "Bloat Counter Font Size",
            description = "Font size for the Bloat counter text"
    )
    default int bloatCounterSize() { return 50; }
}
