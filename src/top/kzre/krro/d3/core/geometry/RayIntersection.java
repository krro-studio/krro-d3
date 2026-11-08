package top.kzre.krro.d3.core.geometry;

/**
 * <p>较完整的相交信息值对象，一般用于承载窄查询或者解析法的相交结果</p>
 *
 * {@link RayHit} 更加简略的相交信息的值对象
 *
 * @see Primitive
 */
public final class RayIntersection {
    private final float t;
    private final boolean hit;
    private final Point point;
    private final Normal normal;

    public RayIntersection(boolean hit, float t,  Point point, Normal normal) {
        this.t = t;
        this.hit = hit;
        this.point = point;
        this.normal = normal;
    }

    private static final RayIntersection MISS = new RayIntersection(false, 0, null, null);
    public static RayIntersection miss() {
        return MISS;
    }

    public static RayIntersection hit(float t, Point point, Normal normal) {
        return new RayIntersection(true, t, point, normal);
    }

    public float getT() {
        return t;
    }

    public boolean isHit() {
        return hit;
    }

    public Point getPoint() {
        return point;
    }

    public Normal getNormal() {
        return normal;
    }
}
