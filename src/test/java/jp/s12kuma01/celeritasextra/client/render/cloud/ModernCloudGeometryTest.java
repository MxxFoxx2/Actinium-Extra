package jp.s12kuma01.celeritasextra.client.render.cloud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernCloudGeometryTest {

    @Test
    void radiusUsesACircleInsteadOfASquare() {
        assertTrue(ModernCloudGeometry.isInsideRadius(3, 4, 5));
        assertFalse(ModernCloudGeometry.isInsideRadius(4, 4, 5));
        assertEquals(81L, ModernCloudGeometry.countCellsInRadius(5, Long.MAX_VALUE));
    }

    @Test
    void rowBoundsCoverExactlyTheSameCircleCells() {
        for (int radius = 0; radius <= 64; radius++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                int maxX = ModernCloudGeometry.maxXForRow(z, radius);
                for (int x = -radius - 1; x <= radius + 1; x++) {
                    assertEquals(ModernCloudGeometry.isInsideRadius(x, z, radius),
                            maxX >= 0 && x >= -maxX && x <= maxX,
                            "row bound mismatch at radius=" + radius + ", x=" + x + ", z=" + z);
                }
            }
        }
    }

    @Test
    void skipsExactVertexScanOnlyWhenTheWorstCaseFits() {
        assertTrue(ModernCloudGeometry.allCellsFitVertexBudget(168, 20, 2_000_000L));
        assertFalse(ModernCloudGeometry.allCellsFitVertexBudget(168, 24, 2_000_000L));
        assertFalse(ModernCloudGeometry.allCellsFitVertexBudget(342, 20, 2_000_000L));
    }

    @Test
    void cloudDistanceChangesTheMeshRadius() {
        assertEquals(32, ModernCloudGeometry.radiusForRange(384.0F, 12.0F));
        assertEquals(168, ModernCloudGeometry.radiusForRange(2_016.0F, 12.0F));
        assertEquals(342, ModernCloudGeometry.radiusForRange(4_096.0F, 12.0F));
    }

    @Test
    void distantCellsKeepOnlyCameraFacingSides() {
        assertEquals(ModernCloudTexture.WEST | ModernCloudTexture.NORTH,
                ModernCloudGeometry.visibleSides(ModernCloudTexture.ALL_SIDES, 4, 7));
        assertEquals(ModernCloudTexture.EAST | ModernCloudTexture.SOUTH,
                ModernCloudGeometry.visibleSides(ModernCloudTexture.ALL_SIDES, -4, -7));
    }

    @Test
    void nearbyCellsKeepEveryExposedSide() {
        assertEquals(ModernCloudTexture.ALL_SIDES,
                ModernCloudGeometry.visibleSides(ModernCloudTexture.ALL_SIDES, 1, -1));
    }

    @Test
    void horizonFadeOnlyUsesTheOuterDistanceBand() {
        assertEquals(1.0F, ModernCloudGeometry.horizonFade(0.0F, 0.0F, 100.0F));
        assertEquals(1.0F, ModernCloudGeometry.horizonFade(90.0F, 0.0F, 100.0F));
        assertEquals(0.5F, ModernCloudGeometry.horizonFade(95.0F, 0.0F, 100.0F));
        assertEquals(0.0F, ModernCloudGeometry.horizonFade(120.0F, 0.0F, 100.0F));

        // Legacy-compatible distance 12 reaches 384 blocks and fades over its final 38.4 blocks.
        assertEquals(1.0F, ModernCloudGeometry.horizonFade(345.6F, 0.0F, 384.0F), 0.0001F);
        assertEquals(0.5F, ModernCloudGeometry.horizonFade(364.8F, 0.0F, 384.0F), 0.0001F);
        assertEquals(0.0F, ModernCloudGeometry.horizonFade(384.0F, 0.0F, 384.0F));

        // Long ranges cap the fade band at 64 blocks instead of making it grow indefinitely.
        assertEquals(1.0F, ModernCloudGeometry.horizonFade(4032.0F, 0.0F, 4096.0F));
        assertEquals(0.5F, ModernCloudGeometry.horizonFade(4064.0F, 0.0F, 4096.0F));
    }

}
