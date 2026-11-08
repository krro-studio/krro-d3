package top.kzre.krro.d3.core.geometry;

/**
 * 射线-AABB slab 裁剪。package-private 工具，供各 Primitive 复用。
 * 支持零厚度 slab（如 Disk 的 Y 轴）与 ±INFINITY 无限边界。
 */
final class AabbClipper {

    private AabbClipper() {}

    /** 返回 true 表示射线错过 AABB [minX,maxX]×[minY,maxY]×[minZ,maxZ]。 */
    static boolean misses(Ray ray,
                          double minX, double maxX,
                          double minY, double maxY,
                          double minZ, double maxZ) {
        double tmin = 0.0;
        double tmax = Double.POSITIVE_INFINITY;

        // X
        if (ray.dx == 0.0) {
            if (ray.ox < minX || ray.ox > maxX) return true;
        } else {
            double inv = 1.0 / ray.dx;
            double t1 = (minX - ray.ox) * inv;
            double t2 = (maxX - ray.ox) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return true;
        }

        // Y
        if (ray.dy == 0.0) {
            if (ray.oy < minY || ray.oy > maxY) return true;
        } else {
            double inv = 1.0 / ray.dy;
            double t1 = (minY - ray.oy) * inv;
            double t2 = (maxY - ray.oy) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return true;
        }

        // Z
        if (ray.dz == 0.0) {
            if (ray.oz < minZ || ray.oz > maxZ) return true;
        } else {
            double inv = 1.0 / ray.dz;
            double t1 = (minZ - ray.oz) * inv;
            double t2 = (maxZ - ray.oz) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tmin) tmin = t1;
            if (t2 < tmax) tmax = t2;
            if (tmin > tmax) return true;
        }

        return false;
    }
}