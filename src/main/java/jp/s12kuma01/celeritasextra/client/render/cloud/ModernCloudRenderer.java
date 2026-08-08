package jp.s12kuma01.celeritasextra.client.render.cloud;

import jp.s12kuma01.celeritasextra.CeleritasExtraMod;
import jp.s12kuma01.celeritasextra.client.CloudPassState;
import jp.s12kuma01.celeritasextra.client.gui.CeleritasExtraGameOptions.RenderSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeModContainer;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Backport of Minecraft 1.21.6's cloud layout to the 1.12.2 Forge cloud render contract.
 *
 * <p>The updated pattern is supplied at runtime by the optional AssetMover integration. Geometry
 * uses modern 12-block cells, a circular horizon-scale volume, camera-facing exposed side faces,
 * and an alpha fade at the outer edge. The renderer is deliberately opportunistic: absent assets,
 * shaders, unsupported graphics paths, or an excessive mesh all return {@code false}, allowing the
 * caller to continue through Forge's original {@code CloudRenderer} without losing clouds.</p>
 */
public final class ModernCloudRenderer {

    private static final VertexFormat FORMAT = DefaultVertexFormats.POSITION_TEX_COLOR;

    private static final float BASE_CELL_SIZE = 12.0F;
    private static final float CLOUD_HEIGHT = 4.0F;
    private static final float CLOUD_ALPHA = 0.8F;

    /** Avoid multi-second rebuilds for extreme distance/small-scale combinations. */
    private static final long MAX_CANDIDATE_CELLS = 600_000L;
    /** POSITION_TEX_COLOR is 24 bytes, making this a roughly 48 MiB CPU/GPU mesh ceiling. */
    private static final long MAX_VERTEX_COUNT = 2_000_000L;

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private ModernCloudTexture textureData;
    private int textureGeneration;
    private boolean textureLoadFailed;
    private boolean renderFailureLogged;

    private VertexBuffer vertexBuffer;
    private DynamicTexture colorTexture;
    private GeometryKey geometryKey;
    private LayoutKey rejectedLayout;
    private LayoutKey reportedLayout;
    private int vertexCount;

    /**
     * Attempts to render the 1.21.6-style clouds.
     *
     * @return {@code true} when this renderer handled the cloud pass; {@code false} to use Forge's
     * original renderer instead
     */
    public boolean render(int cloudTicks, float partialTicks, RenderSettings settings) {
        try {
            if (!canUseModernRenderer() || !ensureTextureData()) {
                return false;
            }

            Entity entity = minecraft.getRenderViewEntity();
            if (entity == null) {
                return false;
            }

            int cloudMode = minecraft.gameSettings.shouldRenderClouds();
            float cellSize = BASE_CELL_SIZE * settings.cloudScale
                    / RenderSettings.CLOUD_SCALE_VANILLA;

            double totalOffset = cloudTicks + partialTicks;
            double cameraX = interpolate(entity.prevPosX, entity.posX, partialTicks)
                    + totalOffset * 0.03D;
            double cameraY = interpolate(entity.lastTickPosY, entity.posY, partialTicks);
            double cameraZ = interpolate(entity.prevPosZ, entity.posZ, partialTicks);
            if (cloudMode == 2) {
                cameraZ += 0.33D * cellSize;
            }

            double cloudBaseY = minecraft.world.provider.getCloudHeight() + 0.33D;
            ViewMode viewMode = ViewMode.fromCamera(cloudBaseY - cameraY);
            int centerX = MathHelper.floor(cameraX / cellSize);
            int centerZ = MathHelper.floor(cameraZ / cellSize);
            int distanceChunks = CloudPassState.effectiveCloudDistanceChunks(
                    settings, minecraft.gameSettings.renderDistanceChunks);
            float fogEnd = CloudPassState.cloudHorizontalRangeBlocks(distanceChunks);
            int radius = ModernCloudGeometry.radiusForRange(fogEnd, cellSize);

            LayoutKey layout = new LayoutKey(radius, Float.floatToIntBits(cellSize),
                    Float.floatToIntBits(fogEnd), cloudMode, viewMode, textureGeneration);
            reportLayoutChange(layout, settings.cloudDistance,
                    minecraft.gameSettings.renderDistanceChunks, distanceChunks);
            if (!isLayoutWithinBudget(layout)) {
                return false;
            }

            GeometryKey key = new GeometryKey(centerX, centerZ, layout);
            if (!ensureGeometry(key, cellSize)) {
                return false;
            }

            if (vertexCount == 0) {
                return true;
            }

            renderGeometry(cameraX, cloudBaseY - cameraY, cameraZ, centerX, centerZ, cellSize,
                    cloudMode == 2, viewMode, partialTicks);
            return true;
        } catch (RuntimeException | LinkageError throwable) {
            disposeGeometry();
            if (!renderFailureLogged) {
                CeleritasExtraMod.LOGGER.error(
                        "Modern Clouds failed; falling back to Forge's cloud renderer", throwable);
                renderFailureLogged = true;
            }
            return false;
        }
    }

    /** Invalidates cloud texture analysis and geometry after a texture resource reload. */
    public void onTextureReload() {
        disposeGeometry();
        textureData = null;
        textureLoadFailed = false;
        renderFailureLogged = false;
        rejectedLayout = null;
        reportedLayout = null;
        textureGeneration++;
    }

    /** Releases GL resources when Forge disposes its companion cloud renderer. */
    public void destroy() {
        disposeGeometry();
        if (colorTexture != null) {
            colorTexture.deleteGlTexture();
            colorTexture = null;
        }
        textureData = null;
        textureLoadFailed = false;
        renderFailureLogged = false;
        rejectedLayout = null;
        reportedLayout = null;
    }

    private boolean canUseModernRenderer() {
        return ModernCloudAssets.isAvailable()
                && OpenGlHelper.useVbo()
                && !ShaderModBridge.areShadersEnabled()
                && ForgeModContainer.forgeCloudsEnabled
                && minecraft.gameSettings.shouldRenderClouds() != 0
                && minecraft.world != null
                && minecraft.world.provider != null
                && minecraft.world.provider.isSurfaceWorld();
    }

    private boolean ensureTextureData() {
        if (textureData != null) {
            return true;
        }
        if (textureLoadFailed) {
            return false;
        }

        try (IResource resource = minecraft.getResourceManager()
                .getResource(ModernCloudAssets.CLOUD_TEXTURE)) {
            BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
            if (image == null) {
                throw new IllegalStateException("Cloud texture decoder returned no image");
            }
            textureData = ModernCloudTexture.from(image);
            return true;
        } catch (Exception | LinkageError throwable) {
            textureLoadFailed = true;
            CeleritasExtraMod.LOGGER.error(
                    "Could not analyze AssetMover's Minecraft 1.21.6 cloud texture; "
                            + "Modern Clouds will use Forge's renderer",
                    throwable);
            return false;
        }
    }

    private boolean isLayoutWithinBudget(LayoutKey layout) {
        if (layout.equals(rejectedLayout)) {
            return false;
        }

        long candidateCells = ModernCloudGeometry.countCellsInRadius(
                layout.radius(), MAX_CANDIDATE_CELLS);
        if (candidateCells <= MAX_CANDIDATE_CELLS) {
            return true;
        }

        rejectLayout(layout, "candidate cell count exceeds " + MAX_CANDIDATE_CELLS);
        return false;
    }

    private boolean ensureGeometry(GeometryKey key, float cellSize) {
        if (key.equals(geometryKey)) {
            return true;
        }

        LayoutKey layout = key.layout();
        if (textureData.isEmpty()) {
            disposeGeometry();
            geometryKey = key;
            return true;
        }

        long estimatedVertices = -1L;
        if (needsExactVertexEstimate(layout)) {
            estimatedVertices = estimateVertexCount(key);
        }
        if (estimatedVertices > MAX_VERTEX_COUNT) {
            rejectLayout(layout, "vertex count " + estimatedVertices + " exceeds "
                    + MAX_VERTEX_COUNT);
            return false;
        }

        if (estimatedVertices == 0L) {
            disposeGeometry();
            geometryKey = key;
            return true;
        }

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        boolean drawing = false;

        try {
            buffer.begin(GL11.GL_QUADS, FORMAT);
            drawing = true;
            emitGeometry(buffer, key, cellSize);
            int builtVertexCount = buffer.getVertexCount();
            if (builtVertexCount > MAX_VERTEX_COUNT) {
                throw new IllegalStateException("Cloud mesh exceeded its proven vertex budget: "
                        + builtVertexCount + " > " + MAX_VERTEX_COUNT);
            }
            if (estimatedVertices >= 0L && builtVertexCount != estimatedVertices) {
                throw new IllegalStateException("Cloud mesh estimate was " + estimatedVertices
                        + " vertices, but the builder produced " + builtVertexCount);
            }
            buffer.finishDrawing();
            drawing = false;

            if (builtVertexCount == 0) {
                disposeGeometry();
                geometryKey = key;
                return true;
            }

            if (vertexBuffer == null) {
                vertexBuffer = new VertexBuffer(FORMAT);
            }
            vertexBuffer.bufferData(buffer.getByteBuffer());
            vertexCount = builtVertexCount;
            geometryKey = key;
            rejectedLayout = null;
            return true;
        } finally {
            if (drawing) {
                try {
                    buffer.finishDrawing();
                } catch (RuntimeException ignored) {
                    // Preserve the original failure while leaving the shared builder reusable.
                }
            }
            buffer.reset();
        }
    }

    private static boolean needsExactVertexEstimate(LayoutKey layout) {
        boolean fancy = layout.cloudMode() == 2;
        int horizontalFaces = fancy && layout.viewMode() == ViewMode.INSIDE ? 2 : 1;
        int maxFacesPerCell = horizontalFaces + (fancy ? 4 : 0);
        int maxVerticesPerCell = maxFacesPerCell * 4;
        return !ModernCloudGeometry.allCellsFitVertexBudget(
                layout.radius(), maxVerticesPerCell, MAX_VERTEX_COUNT);
    }

    private long estimateVertexCount(GeometryKey key) {
        LayoutKey layout = key.layout();
        boolean fancy = layout.cloudMode() == 2;
        int horizontalFaces = fancy && layout.viewMode() == ViewMode.INSIDE ? 2 : 1;
        long vertices = 0L;

        for (int dz = -layout.radius(); dz <= layout.radius(); dz++) {
            int textureZ = key.centerZ() + dz;
            int maxX = ModernCloudGeometry.maxXForRow(dz, layout.radius());
            for (int dx = -maxX; dx <= maxX; dx++) {
                int textureX = key.centerX() + dx;
                if (!textureData.isOccupied(textureX, textureZ)) {
                    continue;
                }

                int faces = horizontalFaces;
                if (fancy) {
                    faces += Integer.bitCount(ModernCloudGeometry.visibleSides(
                            textureData.getExposedSides(textureX, textureZ), dx, dz));
                }
                vertices += faces * 4L;
                if (vertices > MAX_VERTEX_COUNT) {
                    return vertices;
                }
            }
        }
        return vertices;
    }

    private void emitGeometry(BufferBuilder buffer, GeometryKey key, float cellSize) {
        LayoutKey layout = key.layout();
        boolean fancy = layout.cloudMode() == 2;
        float fogEnd = Float.intBitsToFloat(layout.fogEndBits());

        for (int dz = -layout.radius(); dz <= layout.radius(); dz++) {
            int textureZ = key.centerZ() + dz;
            float z = dz * cellSize;
            float v = textureData.getV(textureZ);

            int maxX = ModernCloudGeometry.maxXForRow(dz, layout.radius());
            for (int dx = -maxX; dx <= maxX; dx++) {
                int textureX = key.centerX() + dx;
                if (!textureData.isOccupied(textureX, textureZ)) {
                    continue;
                }

                float x = dx * cellSize;
                float u = textureData.getU(textureX);

                if (!fancy) {
                    emitTop(buffer, x, z, cellSize, 0.0F, u, v, 1.0F, fogEnd);
                    continue;
                }

                switch (layout.viewMode()) {
                    case ABOVE -> emitTop(buffer, x, z, cellSize, CLOUD_HEIGHT, u, v,
                            1.0F, fogEnd);
                    case BELOW -> emitBottom(buffer, x, z, cellSize, u, v, 0.7F, fogEnd);
                    case INSIDE -> {
                        emitTop(buffer, x, z, cellSize, CLOUD_HEIGHT, u, v, 1.0F, fogEnd);
                        emitBottom(buffer, x, z, cellSize, u, v, 0.7F, fogEnd);
                    }
                }

                int sides = ModernCloudGeometry.visibleSides(
                        textureData.getExposedSides(textureX, textureZ), dx, dz);
                if ((sides & ModernCloudTexture.WEST) != 0) {
                    emitWest(buffer, x, z, cellSize, u, v, fogEnd);
                }
                if ((sides & ModernCloudTexture.EAST) != 0) {
                    emitEast(buffer, x, z, cellSize, u, v, fogEnd);
                }
                if ((sides & ModernCloudTexture.NORTH) != 0) {
                    emitNorth(buffer, x, z, cellSize, u, v, fogEnd);
                }
                if ((sides & ModernCloudTexture.SOUTH) != 0) {
                    emitSouth(buffer, x, z, cellSize, u, v, fogEnd);
                }
            }
        }
    }

    private void renderGeometry(double cameraX, double relativeCloudY, double cameraZ,
                                int centerX, int centerZ, float cellSize, boolean fancy,
                                ViewMode viewMode, float partialTicks) {
        boolean modelMatrixPushed = false;
        boolean textureMatrixPushed = false;
        boolean pointersEnabled = false;
        boolean vertexBufferBound = false;
        boolean colorTextureUnitEnabled = false;
        boolean fogStateCaptured = false;
        boolean fogWasEnabled = false;

        try {
            fogWasEnabled = GL11.glIsEnabled(GL11.GL_FOG);
            fogStateCaptured = true;
            // Fixed-function fog only blends RGB. The modern horizon fade is carried in vertex
            // alpha, so leave GL fog out of this draw and restore it immediately afterwards.
            GlStateManager.disableFog();

            GlStateManager.pushMatrix();
            modelMatrixPushed = true;
            GlStateManager.translate(centerX * (double) cellSize - cameraX, relativeCloudY,
                    centerZ * (double) cellSize - cameraZ);

            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            textureMatrixPushed = true;
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);

            if (fancy && viewMode != ViewMode.INSIDE) {
                GlStateManager.enableCull();
            } else {
                GlStateManager.disableCull();
            }

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);

            updateCloudColor(partialTicks);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.bindTexture(colorTexture.getGlTextureId());
            GlStateManager.enableTexture2D();
            colorTextureUnitEnabled = true;

            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            minecraft.renderEngine.bindTexture(ModernCloudAssets.CLOUD_TEXTURE);

            vertexBuffer.bindBuffer();
            vertexBufferBound = true;
            int stride = FORMAT.getSize();
            GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);
            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, stride, 12);
            GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, stride, 20);
            GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
            pointersEnabled = true;

            GlStateManager.colorMask(false, false, false, false);
            vertexBuffer.drawArrays(GL11.GL_QUADS);
            restoreAnaglyphColorMask();
            vertexBuffer.drawArrays(GL11.GL_QUADS);
        } finally {
            GlStateManager.colorMask(true, true, true, true);

            if (vertexBufferBound) {
                vertexBuffer.unbindBuffer();
            }
            if (pointersEnabled) {
                disableVertexFormatPointers();
            }

            if (colorTextureUnitEnabled) {
                GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.disableTexture2D();
            }
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

            if (textureMatrixPushed) {
                GlStateManager.matrixMode(GL11.GL_TEXTURE);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            }

            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            if (modelMatrixPushed) {
                GlStateManager.popMatrix();
            }
            if (fogStateCaptured) {
                if (fogWasEnabled) {
                    GlStateManager.enableFog();
                } else {
                    GlStateManager.disableFog();
                }
            }
        }
    }

    private void updateCloudColor(float partialTicks) {
        Vec3d color = minecraft.world.getCloudColour(partialTicks);
        float red = (float) color.x;
        float green = (float) color.y;
        float blue = (float) color.z;

        if (minecraft.gameSettings.anaglyph) {
            float anaglyphRed = red * 0.3F + green * 0.59F + blue * 0.11F;
            float anaglyphGreen = red * 0.3F + green * 0.7F;
            float anaglyphBlue = red * 0.3F + blue * 0.7F;
            red = anaglyphRed;
            green = anaglyphGreen;
            blue = anaglyphBlue;
        }

        if (colorTexture == null) {
            colorTexture = new DynamicTexture(1, 1);
        }
        colorTexture.getTextureData()[0] = 0xFF000000
                | toColorChannel(red) << 16
                | toColorChannel(green) << 8
                | toColorChannel(blue);
        colorTexture.updateDynamicTexture();
    }

    private void restoreAnaglyphColorMask() {
        if (!minecraft.gameSettings.anaglyph) {
            GlStateManager.colorMask(true, true, true, true);
        } else if (EntityRenderer.anaglyphField == 0) {
            GlStateManager.colorMask(false, true, true, true);
        } else {
            GlStateManager.colorMask(true, false, false, true);
        }
    }

    private static void disableVertexFormatPointers() {
        ByteBuffer buffer = Tessellator.getInstance().getBuffer().getByteBuffer();
        buffer.limit(0);
        for (int index = 0; index < FORMAT.getElementCount(); index++) {
            FORMAT.getElements().get(index).getUsage()
                    .postDraw(FORMAT, index, FORMAT.getSize(), buffer);
        }
        buffer.position(0);
    }

    private void rejectLayout(LayoutKey layout, String reason) {
        rejectedLayout = layout;
        CeleritasExtraMod.LOGGER.warn(
                "Modern Clouds mesh is too large ({}); falling back to Forge's renderer. "
                        + "Reduce Cloud Distance or increase Cloud Scale.",
                reason);
    }

    private void reportLayoutChange(LayoutKey layout, int configuredDistance,
                                    int gameRenderDistance, int effectiveDistance) {
        if (layout.equals(reportedLayout)) {
            return;
        }
        reportedLayout = layout;
        CeleritasExtraMod.LOGGER.debug(
            "Modern Clouds range updated: configured={} chunks, game={} chunks, "
                        + "effective={} chunks, physical={} blocks, radius={} cells",
                configuredDistance, gameRenderDistance, effectiveDistance,
                Math.round(Float.intBitsToFloat(layout.fogEndBits())), layout.radius());
    }

    private void disposeGeometry() {
        if (vertexBuffer != null) {
            vertexBuffer.deleteGlBuffers();
            vertexBuffer = null;
        }
        geometryKey = null;
        vertexCount = 0;
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private static int toColorChannel(float value) {
        return MathHelper.clamp((int) (value * 255.0F), 0, 255);
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z, float u, float v,
                               float brightness, float fogEnd) {
        float alpha = CLOUD_ALPHA * ModernCloudGeometry.horizonFade(x, z, fogEnd);
        buffer.pos(x, y, z).tex(u, v)
                .color(brightness, brightness, brightness, alpha)
                .endVertex();
    }

    private static void emitTop(BufferBuilder buffer, float x, float z, float size, float y,
                                float u, float v, float brightness, float fogEnd) {
        vertex(buffer, x, y, z + size, u, v, brightness, fogEnd);
        vertex(buffer, x + size, y, z + size, u, v, brightness, fogEnd);
        vertex(buffer, x + size, y, z, u, v, brightness, fogEnd);
        vertex(buffer, x, y, z, u, v, brightness, fogEnd);
    }

    private static void emitBottom(BufferBuilder buffer, float x, float z, float size,
                                   float u, float v, float brightness, float fogEnd) {
        vertex(buffer, x + size, 0.0F, z + size, u, v, brightness, fogEnd);
        vertex(buffer, x, 0.0F, z + size, u, v, brightness, fogEnd);
        vertex(buffer, x, 0.0F, z, u, v, brightness, fogEnd);
        vertex(buffer, x + size, 0.0F, z, u, v, brightness, fogEnd);
    }

    private static void emitWest(BufferBuilder buffer, float x, float z, float size, float u,
                                 float v, float fogEnd) {
        vertex(buffer, x, 0.0F, z + size, u, v, 0.9F, fogEnd);
        vertex(buffer, x, CLOUD_HEIGHT, z + size, u, v, 0.9F, fogEnd);
        vertex(buffer, x, CLOUD_HEIGHT, z, u, v, 0.9F, fogEnd);
        vertex(buffer, x, 0.0F, z, u, v, 0.9F, fogEnd);
    }

    private static void emitEast(BufferBuilder buffer, float x, float z, float size, float u,
                                 float v, float fogEnd) {
        vertex(buffer, x + size, CLOUD_HEIGHT, z + size, u, v, 0.9F, fogEnd);
        vertex(buffer, x + size, 0.0F, z + size, u, v, 0.9F, fogEnd);
        vertex(buffer, x + size, 0.0F, z, u, v, 0.9F, fogEnd);
        vertex(buffer, x + size, CLOUD_HEIGHT, z, u, v, 0.9F, fogEnd);
    }

    private static void emitNorth(BufferBuilder buffer, float x, float z, float size, float u,
                                  float v, float fogEnd) {
        vertex(buffer, x + size, CLOUD_HEIGHT, z, u, v, 0.8F, fogEnd);
        vertex(buffer, x + size, 0.0F, z, u, v, 0.8F, fogEnd);
        vertex(buffer, x, 0.0F, z, u, v, 0.8F, fogEnd);
        vertex(buffer, x, CLOUD_HEIGHT, z, u, v, 0.8F, fogEnd);
    }

    private static void emitSouth(BufferBuilder buffer, float x, float z, float size, float u,
                                  float v, float fogEnd) {
        vertex(buffer, x + size, 0.0F, z + size, u, v, 0.8F, fogEnd);
        vertex(buffer, x + size, CLOUD_HEIGHT, z + size, u, v, 0.8F, fogEnd);
        vertex(buffer, x, CLOUD_HEIGHT, z + size, u, v, 0.8F, fogEnd);
        vertex(buffer, x, 0.0F, z + size, u, v, 0.8F, fogEnd);
    }

    private enum ViewMode {
        ABOVE,
        INSIDE,
        BELOW;

        private static ViewMode fromCamera(double relativeCloudY) {
            if (relativeCloudY > 0.0D) {
                return BELOW;
            }
            if (relativeCloudY < -CLOUD_HEIGHT) {
                return ABOVE;
            }
            return INSIDE;
        }
    }

    private record LayoutKey(int radius, int cellSizeBits, int fogEndBits, int cloudMode,
                             ViewMode viewMode, int textureGeneration) {
    }

    private record GeometryKey(int centerX, int centerZ, LayoutKey layout) {
    }
}
