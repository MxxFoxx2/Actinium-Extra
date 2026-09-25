package jp.s12kuma01.actiniumextra.client;

import jp.s12kuma01.actiniumextra.ActiniumExtraMod;
import jp.s12kuma01.actiniumextra.client.gui.ActiniumExtraGameOptions;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;

/**
 * Client-side entry point that owns Actinium Extra's configuration.
 * <p>
 * Lazily loads and caches the {@link ActiniumExtraGameOptions} backing the mod's
 * options from {@code config/actinium-extra.cfg}, creating the {@code config} directory
 * on first use. Invoked from {@link jp.s12kuma01.actiniumextra.ActiniumExtraMod} during
 * initialization, on the client side only.
 */
public class ActiniumExtraClientMod {

    private static ActiniumExtraGameOptions CONFIG;
    private static File configDirectory = new File("config");

    /**
     * Supplies Forge's canonical configuration directory before the options are loaded.
     */
    private static void setConfigDirectory(File directory) {
        if (directory != null && CONFIG == null) {
            configDirectory = directory;
        }
    }

    /**
     * Returns the mod's client options, loading and caching them on first access.
     *
     * @return the shared {@link ActiniumExtraGameOptions} instance
     */
    public static ActiniumExtraGameOptions options() {
        if (CONFIG == null) {
            CONFIG = loadConfig();
        }
        return CONFIG;
    }

    /**
     * Loads the options from {@code config/actinium-extra.cfg}, creating the
     * {@code config} directory first if it does not yet exist.
     *
     * @return the freshly loaded {@link ActiniumExtraGameOptions}
     */
    private static ActiniumExtraGameOptions loadConfig() {
        if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
            ActiniumExtraMod.LOGGER.warn("Could not create config directory: {}", configDirectory);
        }
        File configFile = new File(configDirectory, "actinium-extra.cfg");
        return ActiniumExtraGameOptions.load(configFile);
    }

    /**
     * Initializes the client by eagerly loading the config; called during mod init.
     *
     * @param directory Forge's canonical configuration directory
     */
    public static void onClientInit(File directory) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            setConfigDirectory(directory);
            ActiniumExtraMod.LOGGER.info("Initializing Actinium Extra client...");
            options();
        }
    }
}
