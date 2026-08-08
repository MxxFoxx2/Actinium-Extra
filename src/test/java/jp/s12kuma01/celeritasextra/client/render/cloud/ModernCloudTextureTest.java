package jp.s12kuma01.celeritasextra.client.render.cloud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernCloudTextureTest {

    @Test
    void usesModernAlphaThreshold() {
        ModernCloudTexture texture = ModernCloudTexture.fromArgb(2, 1, new int[]{
                0x09000000,
                0x0A000000
        });

        assertFalse(texture.isOccupied(0, 0));
        assertTrue(texture.isOccupied(1, 0));
    }

    @Test
    void isolatedCellExposesEveryHorizontalSide() {
        int[] pixels = new int[9];
        pixels[4] = 0xFF_FFFFFF;
        ModernCloudTexture texture = ModernCloudTexture.fromArgb(3, 3, pixels);

        assertEquals(ModernCloudTexture.ALL_SIDES, texture.getExposedSides(1, 1));
    }

    @Test
    void adjacentCellsHideTheirSharedSide() {
        int[] pixels = new int[12];
        pixels[1 * 4 + 1] = 0xFF_FFFFFF;
        pixels[1 * 4 + 2] = 0xFF_FFFFFF;
        ModernCloudTexture texture = ModernCloudTexture.fromArgb(4, 3, pixels);

        assertEquals(0, texture.getExposedSides(1, 1) & ModernCloudTexture.EAST);
        assertEquals(0, texture.getExposedSides(2, 1) & ModernCloudTexture.WEST);
        assertTrue((texture.getExposedSides(1, 1) & ModernCloudTexture.WEST) != 0);
        assertTrue((texture.getExposedSides(2, 1) & ModernCloudTexture.EAST) != 0);
    }

    @Test
    void textureEdgesWrapWhenComputingNeighbours() {
        int[] pixels = new int[9];
        pixels[1 * 3] = 0xFF_FFFFFF;
        pixels[1 * 3 + 2] = 0xFF_FFFFFF;
        ModernCloudTexture texture = ModernCloudTexture.fromArgb(3, 3, pixels);

        assertEquals(0, texture.getExposedSides(0, 1) & ModernCloudTexture.WEST);
        assertEquals(0, texture.getExposedSides(2, 1) & ModernCloudTexture.EAST);
    }
}
