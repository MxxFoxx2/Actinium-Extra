package jp.s12kuma01.celeritasextra;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;

import java.io.File;

/**
 * Main mod entry point for Celeritas Extra, a client-only companion to Celeritas.
 * <p>
 * Celeritas Extra layers additional rendering options on top of Celeritas and surfaces
 * them inside Celeritas' own options GUI. This class drives the Forge lifecycle: during
 * construction it verifies Celeritas is present and new enough, then registers the
 * option-GUI construction listeners; during initialization it bootstraps the client
 * configuration.
 * <p>
 * The mod is {@code clientSideOnly} and accepts any remote version, so it can join
 * servers that do not have it installed.
 */
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        clientSideOnly = true, acceptableRemoteVersions = "*",
        dependencies = "required-after:cleanroom@[0.6.0-alpha,);required-after:celeritas;"
                + "after:assetmover@[2.5,)")
public class CeleritasExtraMod {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @Mod.Instance
    public static CeleritasExtraMod INSTANCE;

    private File configDirectory;

    /**
     * Wires Celeritas Extra into Celeritas' options GUI during mod construction.
     * <p>
     * Forge's required dependency guarantees Celeritas is loaded first. Calling the API directly
     * also makes an incompatible Celeritas version fail at the actual missing symbol instead of
     * leaving this mod half-initialized.
     *
     * @param event the Forge construction event
     */
    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        if (Loader.isModLoaded("assetmover")) {
            try {
                jp.s12kuma01.celeritasextra.compat.assetmover.AssetMoverCompat
                        .registerModernCloudTexture();
                LOGGER.info("Requested the Minecraft 1.21.6 cloud texture through AssetMover");
            } catch (RuntimeException | LinkageError throwable) {
                LOGGER.error("AssetMover integration failed; Modern Clouds will remain unavailable",
                        throwable);
            }
        } else {
            LOGGER.info("AssetMover is not installed; Modern Clouds will remain unavailable");
        }

        OptionGUIConstructionEvent.BUS.addListener(jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraOptionsListener::onCeleritasOptionsConstruct);
        OptionGroupConstructionEvent.BUS.addListener(jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraOptionsListener::onOptionGroupConstruct);
        LOGGER.info("Successfully registered Celeritas Extra with Celeritas GUI");
    }

    /**
     * Captures Forge's canonical configuration directory before client initialization.
     *
     * @param event the Forge pre-initialization event
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.configDirectory = event.getModConfigurationDirectory();
        LOGGER.info("Celeritas Extra pre-initialization");
    }

    /**
     * Bootstraps the client on the effective client side during Forge initialization.
     * <p>
     * Delegates to {@link jp.s12kuma01.celeritasextra.client.CeleritasExtraClientMod#onClientInit(File)}
     * so dedicated-server environments never load client-only classes.
     *
     * @param event the Forge initialization event
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            jp.s12kuma01.celeritasextra.client.CeleritasExtraClientMod
                    .onClientInit(this.configDirectory);
        }
        LOGGER.info("Celeritas Extra initialized");
    }
}
