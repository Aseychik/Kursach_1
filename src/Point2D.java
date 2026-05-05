import java.awt.*;

public class Point2D {
    double x, y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point2D() {
        x = 0;
        y = 0;
    }

    public Point2D(Point point) {
        x = point.x;
        y = point.y;
    }

    public Point2D(Point2D point) {
        x = point.x;
        y = point.y;
    }

    public static Point2D multiply(Point2D point, double k){
        return new Point2D(point.x * k, point.y * k);
    }

    public static Point2D add(Point2D a, Point2D b) {
        return new Point2D(a.x + b.x, a.y + b.y);
    }

    public static Point2D div(Point2D a, Point2D b) { return new Point2D(a.x - b.x, a.y - b.y); }

    public double length() { return Math.sqrt(x * x + y * y); }

    public Point2D normalize() throws Exception {
        double l = length();
        if (l == 0) throw new Exception("Attempt of normalizing zero length vector");

        return Point2D.multiply(this, 1 / l);
    }

    public Point2D transpose(Point2D half_window, double scale, Point2D position) {
        return Point2D.add(half_window, Point2D.multiply(Point2D.add(this, position), 1 / scale));
    }

    public Point2D u_minus() { return new Point2D(-x, -y); }

    public static Point2D find_intersect_point(Point2D p11, Point2D p12, Point2D p21, Point2D p22) {
        Point2D v1 = Point2D.div(p12, p11), v2 = Point2D.div(p22, p21), v3 = Point2D.div(p11, p21);
        double k = 0, t = 0;
        if (v3.x == 0 && v3.y == 0) return new Point2D(p11);

        if (v2.x == 0) {
            if (v1.x == 0) return new Point2D(p12.x, (p12.y  + p22.y) / 2);
            else return Point2D.add(p11, Point2D.multiply(v1, -v3.x / v1.x));
        }
        else {
            if (v1.x == 0) return Point2D.add(p21, Point2D.multiply(v2, v3.x / v2.x));
            else {
                if (v2.y == 0) {
                    if (v1.y == 0) return new Point2D((p12.x + p22.x) / 2, p12.y);
                    else return Point2D.add(p11, Point2D.multiply(v1, -v3.y / v1.y));
                }
                return Point2D.add(p11, Point2D.multiply(v1, (v3.x - v3.y * v2.x) / (-v1.x + v1.y / v2.y * v2.x)));
            }
        }
    }

    public Point tip(int type) {
        if (type == 2) return new Point((int)Math.ceil(x), (int)Math.ceil(y));
        if (type == 1) return new Point((int)Math.round(x), (int)Math.round(y));
        return new Point((int)x, (int)y);
    }

    @Override
    public String toString() {
        return "Point2D(" + x + " " + y + ")";
    }
}
