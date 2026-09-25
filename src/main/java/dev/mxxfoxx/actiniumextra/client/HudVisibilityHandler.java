package dev.mxxfoxx.actiniumextra.client;

import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions.ExtraSettings;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Hides the HUD elements the user switched off: the boss health bar, the scoreboard sidebar and the
 * potion effect icons.
 * <p>
 * This is deliberately Forge-level rather than mixin-level. Forge's own in-game HUD posts a
 * cancellable {@code RenderGameOverlayEvent.Pre} per element, so cancelling the element's event is
 * both enough and polite: another mod that also owns that element still sees its own handler run,
 * and nothing here depends on how the world is rendered. That matters for an add-on that runs on two
 * different render pipelines - these toggles behave identically on Celeritas and Actinium, and on
 * vanilla, because none of them touches the renderer at all.
 * <p>
 * The sidebar is the one exception: Forge 1.12.2 gives it no element type and instead guards it with
 * the {@code GuiIngameForge.renderObjective} flag, so that flag is refreshed from the config on every
 * frame's {@code ALL} event.
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public final class HudVisibilityHandler {
    private HudVisibilityHandler() {
    }

    /**
     * Cancels the overlay elements whose toggles are off.
     *
     * @param event the Forge pre-render event for one HUD element
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        ExtraSettings settings = ActiniumExtraClientMod.options().extraSettings;

        switch (event.getType()) {
            case BOSSHEALTH -> {
                if (!settings.showBossHealth) {
                    event.setCanceled(true);
                }
            }
            case POTION_ICONS -> {
                if (!settings.showPotionIcons) {
                    event.setCanceled(true);
                }
            }
            case ALL -> GuiIngameForge.renderObjective = settings.showScoreboard;
            default -> {
                // Every other element is left to vanilla and to whatever else the pack does with it.
            }
        }
    }
}
