package top.kzre.krro.d3.core.geometry;

/**
 * Y 轴对齐的矩形平面（GL 右手系，零厚度面）。
 *
 * <p><b>无限语义显式化</b>：
 * <ul>
 *   <li>{@code infiniteX = true} → X 方向不裁剪，{@code width} 被忽略</li>
 *   <li>{@code infiniteZ = true} → Z 方向不裁剪，{@code height} 被忽略</li>
 *   <li>两轴均无限 → 无限平面</li>
 *   <li>两轴均有限 → 普通矩形平面（{@code width / height} 为总宽，内部取 {@code ×0.5} 为半宽）</li>
 * </ul>
 *
 * <p><b>有限方向合法性</b>：{@code width > 0}（X 有限时）、{@code height > 0}（Z 有限时），
 * 否则视为非法形状 → 一律 miss。无限方向的 {@code width / height} 不做检查。
 */
public class Plane implements Primitive {

    private static final double PARALLEL_EPS = 1e-12;

    public final float cx, cy, cz;
    /** X 方向总宽（仅 {@code infiniteX = false} 时生效） */
    public final float width;
    /** Z 方向总宽（仅 {@code infiniteZ = false} 时生效） */
    public final float height;
    /** X 方向是否无限（不裁剪） */
    public final boolean infiniteX;
    /** Z 方向是否无限（不裁剪） */
    public final boolean infiniteZ;

    // ------------------------------------------------------------------
    // 构造
    // ------------------------------------------------------------------

    /** 有限矩形平面（两轴均有限）。 */
    public Plane(float cx, float cy, float cz, float width, float height) {
        this(cx, cy, cz, width, height, false, false);
    }

    /** 完全控制构造器。 */
    public Plane(float cx, float cy, float cz,
                 float width, float height,
                 boolean infiniteX, boolean infiniteZ) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.width = width; this.height = height;
        this.infiniteX = infiniteX;
        this.infiniteZ = infiniteZ;
    }

    // ------------------------------------------------------------------
    // 工厂（与三种典型形态一一对应）
    // ------------------------------------------------------------------

    /** 无限平面（两轴均不裁剪）。 */
    public static Plane infinite(float cx, float cy, float cz) {
        return new Plane(cx, cy, cz, 0f, 0f, true, true);
    }

    /** 有限矩形平面。 */
    public static Plane rect(float cx, float cy, float cz,
                             float width, float height) {
        return new Plane(cx, cy, cz, width, height, false, false);
    }

    /** 沿 X 方向有限、Z 方向无限的"长条"平面。 */
    public static Plane stripX(float cx, float cy, float cz, float width) {
        return new Plane(cx, cy, cz, width, 0f, false, true);
    }

    /** 沿 Z 方向有限、X 方向无限的"长条"平面。 */
    public static Plane stripZ(float cx, float cy, float cz, float height) {
        return new Plane(cx, cy, cz, 0f, height, true, false);
    }

    // ------------------------------------------------------------------
    // 相交
    // ------------------------------------------------------------------

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded()) return RayIntersection.miss();
        if (!infiniteX && !(width > 0f))   return RayIntersection.miss();
        if (!infiniteZ && !(height > 0f))  return RayIntersection.miss();

        // 无限方向的半宽 = +INF，AabbClipper 天然支持 ±INF 边界
        final double halfW = infiniteX ? Double.POSITIVE_INFINITY : 0.5 * (double) width;
        final double halfH = infiniteZ ? Double.POSITIVE_INFINITY : 0.5 * (double) height;

        if (AabbClipper.misses(ray,
                (double) cx - halfW, (double) cx + halfW,
                (double) cy,          (double) cy,              // 零厚度
                (double) cz - halfH, (double) cz + halfH)) {
            return RayIntersection.miss();
        }

        final double dy = ray.dy;
        if (Math.abs(dy) < PARALLEL_EPS) return RayIntersection.miss();

        final double t = ((double) cy - ray.oy) / dy;
        if (!(t >= 0.0) || !Double.isFinite(t)) return RayIntersection.miss();

        final double px = (double) ray.ox + (double) ray.dx * t;
        final double pz = (double) ray.oz + (double) ray.dz * t;

        if (!infiniteX && Math.abs(px - cx) > 0.5 * (double) width) {
            return RayIntersection.miss();
        }
        if (!infiniteZ && Math.abs(pz - cz) > 0.5 * (double) height) {
            return RayIntersection.miss();
        }

        final float ny = (dy < 0.0) ? 1f : -1f;
        return RayIntersection.hit((float) t,
                new Point((float) px, cy, (float) pz),
                new Normal(0f, ny, 0f));
    }

    /** XZ 投影是否落在矩形 / 无限区域内，忽略 y。 */
    public boolean containsXZ(float x, float z) {
        if (!infiniteX && !(width > 0f))   return false;
        if (!infiniteZ && !(height > 0f))  return false;
        if (!infiniteX && Math.abs(x - cx) > 0.5f * width)  return false;
        if (!infiniteZ && Math.abs(z - cz) > 0.5f * height) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Plane[(" + cx + ", " + cy + ", " + cz + ")"
                + " size=" + (infiniteX ? "∞" : String.valueOf(width)) + "×"
                + (infiniteZ ? "∞" : String.valueOf(height)) + "]";
    }
}