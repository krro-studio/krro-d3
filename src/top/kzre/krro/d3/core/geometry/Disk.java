package top.kzre.krro.d3.core.geometry;

/**
 * Y 轴对齐的圆盘 / 环形扇区（GL 右手系，零厚度面）。
 */
public final class Disk implements Primitive {

    private static final double TWO_PI       = Math.PI * 2.0;
    private static final double PARALLEL_EPS = 1e-12;
    private static final double ANGLE_EPS    = 1e-9;

    public final float cx, cy, cz;
    public final float innerRadius;
    public final float radius;
    public final float startAngle;
    public final float endAngle;

    public Disk(float cx, float cy, float cz,
                float radius, float startAngle, float endAngle) {
        this(cx, cy, cz, 0f, radius, startAngle, endAngle);
    }

    public Disk(float cx, float cy, float cz,
                float innerRadius, float radius,
                float startAngle, float endAngle) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.innerRadius = innerRadius;
        this.radius = radius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
    }

    public static Disk full(float cx, float cy, float cz, float radius) {
        return new Disk(cx, cy, cz, 0f, radius, 0f, (float) TWO_PI);
    }

    public static Disk sector(float cx, float cy, float cz,
                              float radius, float startAngle, float sweep) {
        return new Disk(cx, cy, cz, 0f, radius, startAngle, startAngle + sweep);
    }

    public static Disk ring(float cx, float cy, float cz,
                            float innerRadius, float radius) {
        return new Disk(cx, cy, cz, innerRadius, radius, 0f, (float) TWO_PI);
    }

    public static Disk ringSector(float cx, float cy, float cz,
                                  float innerRadius, float radius,
                                  float startAngle, float sweep) {
        return new Disk(cx, cy, cz, innerRadius, radius,
                startAngle, startAngle + sweep);
    }

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded() || !shapeValid()) return RayIntersection.miss();

        if (AabbClipper.misses(ray,
                (double) cx - radius, (double) cx + radius,
                (double) cy,          (double) cy,             // 零厚度
                (double) cz - radius, (double) cz + radius)) {
            return RayIntersection.miss();
        }

        double dy = ray.dy;
        if (Math.abs(dy) < PARALLEL_EPS) return RayIntersection.miss();

        double t = ((double) cy - ray.oy) / dy;
        if (!(t >= 0.0) || !Double.isFinite(t)) return RayIntersection.miss();

        double px = (double) ray.ox + (double) ray.dx * t;
        double pz = (double) ray.oz + (double) ray.dz * t;
        double rx = px - cx;
        double rz = pz - cz;
        double r2 = rx * rx + rz * rz;

        double inner2 = (double) innerRadius * innerRadius;
        double outer2 = (double) radius * radius;
        if (r2 > outer2 || r2 < inner2) return RayIntersection.miss();
        if (!containsAngle(rx, rz)) return RayIntersection.miss();

        float ny = (dy < 0.0) ? 1f : -1f;
        return RayIntersection.hit((float) t,
                new Point((float) px, cy, (float) pz),
                new Normal(0f, ny, 0f));
    }

    public boolean containsXZ(float x, float z) {
        if (!shapeValid()) return false;
        double rx = x - cx;
        double rz = z - cz;
        double r2 = rx * rx + rz * rz;
        if (r2 > (double) radius * radius) return false;
        if (r2 < (double) innerRadius * innerRadius) return false;
        return containsAngle(rx, rz);
    }

    private boolean shapeValid() {
        return radius > 0f && innerRadius >= 0f && innerRadius < radius;
    }

    private boolean containsAngle(double rx, double rz) {
        double sw = (double) endAngle - startAngle;
        if (sw >= TWO_PI - ANGLE_EPS) return true;
        if (!(sw > 0.0)) return false;
        double theta = Math.atan2(-rz, rx);
        double rel = (theta - startAngle) % TWO_PI;
        if (rel < 0.0) rel += TWO_PI;
        return rel <= sw + ANGLE_EPS;
    }

    @Override
    public String toString() {
        return "Disk[(" + cx + ", " + cy + ", " + cz + ")"
                + " r=[" + innerRadius + ", " + radius + "]"
                + " angle=[" + startAngle + ", " + endAngle + "]]";
    }
}