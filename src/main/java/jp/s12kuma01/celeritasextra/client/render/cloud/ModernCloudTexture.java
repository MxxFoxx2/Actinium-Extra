package jp.s12kuma01.celeritasextra.client.render.cloud;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * CPU-side description of the repeating cloud texture.
 *
 * <p>Modern Minecraft treats every non-transparent texture pixel as one cloud cell. The side mask
 * records which horizontal neighbours are transparent so the renderer can omit faces between two
 * occupied cells.</p>
 */
final class ModernCloudTexture {

    static final int WEST = 1;
    static final int EAST = 1 << 1;
    static final int NORTH = 1 << 2;
    static final int SOUTH = 1 << 3;
    static final int ALL_SIDES = WEST | EAST | NORTH | SOUTH;

    /** Minecraft 1.21.6 treats pixels with alpha below 10 as empty cloud cells. */
    private static final int MIN_VISIBLE_ALPHA = 10;

    private final int width;
    private final int height;
    private final boolean[] occupied;
    private final byte[] exposedSides;
    private final int occupiedCellCount;

    private ModernCloudTexture(int width, int height, boolean[] occupied, byte[] exposedSides,
                               int occupiedCellCount) {
        this.width = width;
        this.height = height;
        this.occupied = occupied;
        this.exposedSides = exposedSides;
        this.occupiedCellCount = occupiedCellCount;
    }

    static ModernCloudTexture from(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        return fromArgb(width, height, argb);
    }

    static ModernCloudTexture fromArgb(int width, int height, int[] argb) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Cloud texture dimensions must be positive");
        }
        Objects.requireNonNull(argb, "argb");

        int cellCount = Math.multiplyExact(width, height);
        if (argb.length != cellCount) {
            throw new IllegalArgumentException("Expected " + cellCount + " pixels, got " + argb.length);
        }

        boolean[] occupied = new boolean[cellCount];
        int occupiedCellCount = 0;

        for (int index = 0; index < cellCount; index++) {
            boolean visible = (argb[index] >>> 24) >= MIN_VISIBLE_ALPHA;
            occupied[index] = visible;
            if (visible) {
                occupiedCellCount++;
            }
        }

        byte[] exposedSides = new byte[cellCount];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int index = index(width, x, z);
                if (!occupied[index]) {
                    continue;
                }

                int sides = 0;
                if (!occupied[index(width, Math.floorMod(x - 1, width), z)]) {
                    sides |= WEST;
                }
                if (!occupied[index(width, Math.floorMod(x + 1, width), z)]) {
                    sides |= EAST;
                }
                if (!occupied[index(width, x, Math.floorMod(z - 1, height))]) {
                    sides |= NORTH;
                }
                if (!occupied[index(width, x, Math.floorMod(z + 1, height))]) {
                    sides |= SOUTH;
                }
                exposedSides[index] = (byte) sides;
            }
        }

        return new ModernCloudTexture(width, height, occupied, exposedSides, occupiedCellCount);
    }

    boolean isEmpty() {
        return occupiedCellCount == 0;
    }

    boolean isOccupied(int x, int z) {
        return occupied[wrappedIndex(x, z)];
    }

    int getExposedSides(int x, int z) {
        return exposedSides[wrappedIndex(x, z)] & 0xFF;
    }

    float getU(int x) {
        return (Math.floorMod(x, width) + 0.5F) / width;
    }

    float getV(int z) {
        return (Math.floorMod(z, height) + 0.5F) / height;
    }

    private int wrappedIndex(int x, int z) {
        return index(width, Math.floorMod(x, width), Math.floorMod(z, height));
    }

    private static int index(int width, int x, int z) {
        return z * width + x;
    }
}
