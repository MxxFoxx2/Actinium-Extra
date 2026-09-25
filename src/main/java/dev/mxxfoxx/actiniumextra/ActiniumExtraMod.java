package dev.mxxfoxx.actiniumextra;

import dev.mxxfoxx.actiniumextra.renderer.RendererSupport;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Main mod entry point for Actinium Extra, a client-only companion to the Celeritas and Actinium
 * render pipelines.
 * <p>
 * Actinium Extra layers additional rendering options on top of the renderer and surfaces them
 * inside that renderer's own options GUI. This class drives the Forge lifecycle: during construction
 * it detects which renderer the game run installed and registers the option contributions with it;
 * during initialization it bootstraps the client configuration.
 * <p>
 * The renderer is declared {@code after} rather than {@code required-after} because Forge's
 * dependency language has no "either of these" form and Actinium Extra supports both renderers, so
 * {@link RendererSupport} reports a missing renderer during construction instead.
 * <p>
 * The mod is {@code clientSideOnly} and accepts any remote version, so it can join servers that do
 * not have it installed.
 */
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        clientSideOnly = true, acceptableRemoteVersions = "*",
        dependencies = "required-after:cleanroom@[0.6.10-alpha,);after:celeritas;"
                + "after:actinium;after:assetmover@[2.5,)")
public class ActiniumExtraMod {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @Mod.Instance
    public static ActiniumExtraMod INSTANCE;

    private File configDirectory;

    /**
     * Wires Actinium Extra into the installed renderer's options GUI during mod construction.
     * <p>
     * Both renderers collect and freeze their option-page list the first time the settings screen is
     * built, so the listeners have to be registered before then. Touching the renderer's API only
     * here also makes an incompatible renderer version fail at the actual missing symbol instead of
     * leaving this mod half-initialized.
     *
     * @param event the Forge construction event
     */
    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        RendererSupport.initialize();

        if (Loader.isModLoaded("assetmover")) {
            try {
                dev.mxxfoxx.actiniumextra.compat.assetmover.AssetMoverCompat
                        .registerModernCloudTexture();
                LOGGER.info("Requested the Minecraft 1.21.6 cloud texture through AssetMover");
            } catch (RuntimeException | LinkageError throwable) {
                LOGGER.error("AssetMover integration failed; the modern cloud texture was not requested",
                        throwable);
            }
        } else {
            LOGGER.info("AssetMover is not installed; the modern cloud texture will not be downloaded");
        }
    }

    /**
     * Captures Forge's canonical configuration directory before client initialization.
     *
     * @param event the Forge pre-initialization event
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.configDirectory = event.getModConfigurationDirectory();
        LOGGER.info("Actinium Extra pre-initialization");
    }

    /**
     * Bootstraps the client on the effective client side during Forge initialization.
     * <p>
     * Delegates to {@link dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod#onClientInit(File)}
     * so dedicated-server environments never load client-only classes.
     *
     * @param event the Forge initialization event
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod
                    .onClientInit(this.configDirectory);
        }
        LOGGER.info("Actinium Extra initialized");
    }
}
