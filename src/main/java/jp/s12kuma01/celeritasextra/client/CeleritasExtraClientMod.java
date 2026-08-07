package jp.s12kuma01.celeritasextra.client;

import jp.s12kuma01.celeritasextra.CeleritasExtraMod;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;

/**
 * Client-side entry point that owns Celeritas Extra's configuration.
 * <p>
 * Lazily loads and caches the {@link CeleritasExtraGameOptions} backing the mod's
 * options from {@code config/celeritas-extra.cfg}, creating the {@code config} directory
 * on first use. Invoked from {@link jp.s12kuma01.celeritasextra.CeleritasExtraMod} during
 * initialization, on the client side only.
 */
public class CeleritasExtraClientMod {

    private static CeleritasExtraGameOptions CONFIG;
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
     * @return the shared {@link CeleritasExtraGameOptions} instance
     */
    public static CeleritasExtraGameOptions options() {
        if (CONFIG == null) {
            CONFIG = loadConfig();
        }
        return CONFIG;
    }

    /**
     * Loads the options from {@code config/celeritas-extra.cfg}, creating the
     * {@code config} directory first if it does not yet exist.
     *
     * @return the freshly loaded {@link CeleritasExtraGameOptions}
     */
    private static CeleritasExtraGameOptions loadConfig() {
        if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
            CeleritasExtraMod.LOGGER.warn("Could not create config directory: {}", configDirectory);
        }
        File configFile = new File(configDirectory, "celeritas-extra.cfg");
        return CeleritasExtraGameOptions.load(configFile);
    }

    /**
     * Initializes the client by eagerly loading the config; called during mod init.
     *
     * @param directory Forge's canonical configuration directory
     */
    public static void onClientInit(File directory) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            setConfigDirectory(directory);
            CeleritasExtraMod.LOGGER.info("Initializing Celeritas Extra client...");
            options();
        }
    }
}
