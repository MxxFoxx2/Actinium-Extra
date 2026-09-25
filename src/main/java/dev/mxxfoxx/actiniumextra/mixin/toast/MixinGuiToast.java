package dev.mxxfoxx.actiniumextra.mixin.toast;

import dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod;
import dev.mxxfoxx.actiniumextra.client.gui.ActiniumExtraGameOptions;
import net.minecraft.client.gui.toasts.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses toast pop-ups (advancement / recipe / tutorial / system) per user config.
 * Inspired by Sodium Extra's toast controls.
 */
@Mixin(GuiToast.class)
public class MixinGuiToast {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void actiniumExtra$filterToast(IToast toastIn, CallbackInfo ci) {
        ActiniumExtraGameOptions.ExtraSettings settings = ActiniumExtraClientMod.options().extraSettings;

        if (!settings.toasts
                || (toastIn instanceof AdvancementToast && !settings.toastAdvancement)
                || (toastIn instanceof RecipeToast && !settings.toastRecipe)
                || (toastIn instanceof TutorialToast && !settings.toastTutorial)
                || (toastIn instanceof SystemToast && !settings.toastSystem)) {
            ci.cancel();
        }
    }
}
