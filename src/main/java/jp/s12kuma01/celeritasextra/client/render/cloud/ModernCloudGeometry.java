package jp.s12kuma01.celeritasextra.client.render.cloud;

/** Pure geometry helpers shared by the modern cloud mesh estimator, builder, and tests. */
final class ModernCloudGeometry {

    private static final float FADE_DISTANCE_FRACTION = 0.1F;
    private static final float MIN_FADE_DISTANCE = 4.0F;
    private static final float MAX_FADE_DISTANCE = 64.0F;

    private ModernCloudGeometry() {
    }

    static boolean isInsideRadius(int x, int z, int radius) {
        if (radius < 0) {
            return false;
        }
        long radiusSquared = (long) radius * radius;
        return (long) x * x + (long) z * z <= radiusSquared;
    }

    /** Returns the inclusive horizontal extent of one circle row, or {@code -1} outside it. */
    static int maxXForRow(int z, int radius) {
        if (radius < 0 || z < -radius || z > radius) {
            return -1;
        }

        long radiusSquared = (long) radius * radius;
        long zSquared = (long) z * z;
        return (int) Math.floor(Math.sqrt(radiusSquared - zSquared));
    }

    /** Converts a physical block range into the cell radius used by the circular cloud mesh. */
    static int radiusForRange(float rangeBlocks, float cellSize) {
        if (!(rangeBlocks >= 0.0F) || !Float.isFinite(rangeBlocks)) {
            throw new IllegalArgumentException("Cloud range must be finite and non-negative");
        }
        if (!(cellSize > 0.0F) || !Float.isFinite(cellSize)) {
            throw new IllegalArgumentException("Cloud cell size must be finite and positive");
        }

        double radius = rangeBlocks / cellSize;
        if (radius > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cloud mesh radius exceeds the integer range");
        }
        return (int) Math.ceil(radius);
    }

    /**
     * Counts integer cell centers in a circle, stopping as soon as {@code stopAfter} is exceeded.
     * The early exit lets callers reject pathological layouts without walking the entire square.
     */
    static long countCellsInRadius(int radius, long stopAfter) {
        if (radius < 0) {
            return 0L;
        }

        long count = 0L;
        for (int z = -radius; z <= radius; z++) {
            int maxX = maxXForRow(z, radius);
            count += (long) maxX * 2L + 1L;
            if (count > stopAfter) {
                return count;
            }
        }
        return count;
    }

    /**
     * Returns whether even the worst-case contents of a circle fit a vertex budget. This lets the
     * renderer skip an exact texture scan only when doing so is provably safe.
     */
    static boolean allCellsFitVertexBudget(int radius, int maxVerticesPerCell, long vertexBudget) {
        if (maxVerticesPerCell <= 0) {
            throw new IllegalArgumentException("Vertices per cell must be positive");
        }
        if (vertexBudget < 0L) {
            throw new IllegalArgumentException("Vertex budget cannot be negative");
        }

        long cellBudget = vertexBudget / maxVerticesPerCell;
        return countCellsInRadius(radius, cellBudget) <= cellBudget;
    }

    /**
     * Keeps only exposed side faces that can face the camera. The immediate 3x3 neighborhood keeps
     * every exposed face so entering a cloud cell never reveals missing interior geometry.
     */
    static int visibleSides(int exposedSides, int relativeX, int relativeZ) {
        if (relativeX >= -1 && relativeX <= 1 && relativeZ >= -1 && relativeZ <= 1) {
            return exposedSides;
        }

        int cameraFacing = 0;
        if (relativeX > 0) {
            cameraFacing |= ModernCloudTexture.WEST;
        } else if (relativeX < 0) {
            cameraFacing |= ModernCloudTexture.EAST;
        }
        if (relativeZ > 0) {
            cameraFacing |= ModernCloudTexture.NORTH;
        } else if (relativeZ < 0) {
            cameraFacing |= ModernCloudTexture.SOUTH;
        }
        return exposedSides & cameraFacing;
    }

    /**
     * Fades only the outer edge of the cloud volume. Minecraft 1.21.6 fades clouds across their
     * entire range, but its separate cloud-range option defaults to 128 chunks. Celeritas Extra's
     * default follows the often much shorter game render distance, where a full-range fade makes
     * clouds effectively disappear around half the configured distance. Using the modern terrain
     * fog's final 10% band (clamped to 4-64 blocks) keeps the selected range usable while retaining
     * a smooth boundary. The VBO stores horizontal local coordinates, so vertical distance is
     * intentionally omitted here.
     */
    static float horizonFade(float x, float z, float fogEnd) {
        if (!(fogEnd > 0.0F) || !Float.isFinite(fogEnd)) {
            return 0.0F;
        }

        double distance = Math.sqrt((double) x * x + (double) z * z);
        double fadeDistance = Math.max(MIN_FADE_DISTANCE,
                Math.min(MAX_FADE_DISTANCE, fogEnd * FADE_DISTANCE_FRACTION));
        double fadeStart = Math.max(0.0D, fogEnd - fadeDistance);
        if (distance <= fadeStart) {
            return 1.0F;
        }
        if (distance >= fogEnd) {
            return 0.0F;
        }
        return (float) ((fogEnd - distance) / (fogEnd - fadeStart));
    }
}
