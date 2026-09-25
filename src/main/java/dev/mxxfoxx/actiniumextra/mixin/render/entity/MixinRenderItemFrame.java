package dev.mxxfoxx.actiniumextra.mixin.render.entity;

import dev.mxxfoxx.actiniumextra.client.ActiniumExtraClientMod;
import dev.mxxfoxx.actiniumextra.client.ItemFrameLodState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.entity.RenderItemFrame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controls item frame rendering, name tags, and level-of-detail culling via {@link RenderItemFrame}.
 * Ported from MixinItemFrameEntityRenderer in Embeddium Extra 1.20.1.
 * <p>
 * Three independent behaviors, each gated by a render setting:
 * - {@code itemFrames}: cancels {@code doRender} to hide the frame entirely.
 * - {@code itemFrameNameTag}: suppresses the frame's hover name tag.
 * - {@code itemFrameLodDistance}: a backport of MoreCulling's "Frame LOD" that, beyond the
 * configured distance, sets {@link ItemFrameLodState#active} so the held item's quads are
 * face-reduced (via {@link MixinRenderItem} / {@link MixinForgeHooksClient}) and framed maps are
 * culled outright. The flag is always cleared on return so it never leaks into unrelated item
 * rendering.
 */
@Mixin(RenderItemFrame.class)
public class MixinRenderItemFrame {

    @Inject(
            method = "doRender(Lnet/minecraft/entity/item/EntityItemFrame;DDDFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void doRender(EntityItemFrame entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!ActiniumExtraClientMod.options().renderSettings.itemFrames) {
            ci.cancel();
        }
    }

    /**
     * Controls the custom name rendered for the item displayed in an item frame.
     */
    @Inject(
            method = "renderName(Lnet/minecraft/entity/item/EntityItemFrame;DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actiniumExtra$renderName(EntityItemFrame entity, double x, double y, double z,
                                           CallbackInfo ci) {
        if (!ActiniumExtraClientMod.options().renderSettings.itemFrameNameTag) {
            ci.cancel();
        }
    }

    /**
     * Item frame LOD (backport of MoreCulling's "Frame LOD"): mark this frame's content render as
     * beyond the LOD distance so the RenderItem / ForgeHooksClient quad filter kicks in and the map
     * cull below applies. Distance is measured from the view entity (renderItem has no camera-relative
     * coords), matching vanilla renderName's own distance idiom.
     */
    @Inject(method = "renderItem(Lnet/minecraft/entity/item/EntityItemFrame;)V", at = @At("HEAD"))
    private void actiniumExtra$beginItemFrameLod(EntityItemFrame entity, CallbackInfo ci) {
        int dist = ActiniumExtraClientMod.options().renderSettings.itemFrameLodDistance;
        if (dist <= 0) {
            ItemFrameLodState.active = false;
            return;
        }
        Entity view = Minecraft.getMinecraft().getRenderManager().renderViewEntity;
        if (view == null) {
            ItemFrameLodState.active = false;
            return;
        }
        ItemFrameLodState.active = entity.getDistanceSq(view) > (double) dist * (double) dist;
    }

    /**
     * Always clear the LOD flag when leaving renderItem, so it never leaks into unrelated item
     * rendering (inventory, hand, GUI, dropped items).
     */
    @Inject(method = "renderItem(Lnet/minecraft/entity/item/EntityItemFrame;)V", at = @At("RETURN"))
    private void actiniumExtra$endItemFrameLod(EntityItemFrame entity, CallbackInfo ci) {
        ItemFrameLodState.active = false;
    }

    /**
     * Cull the framed map entirely beyond the LOD distance (maps are a single flat texture, so there
     * is nothing to face-reduce — skipping the render is the map equivalent of LOD).
     */
    @Redirect(
            method = "renderItem(Lnet/minecraft/entity/item/EntityItemFrame;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/MapItemRenderer;renderMap(Lnet/minecraft/world/storage/MapData;Z)V")
    )
    private void actiniumExtra$cullFramedMap(MapItemRenderer renderer, MapData data, boolean noOverlay) {
        if (ItemFrameLodState.active) {
            return;
        }
        renderer.renderMap(data, noOverlay);
    }
}
