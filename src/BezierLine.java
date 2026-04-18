import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BezierLine {
    List<Point> points;
    int[] pointSize = new int[]{12, 12};
    Color lineColor = Color.black, pointColor = Color.green;
    int bezierCount = 100;

    // 0 - not composite, 1 - composite at the right, -1 - composite at the left
    // 2 - composite at the both sides
    int is_composite = 0;
    Point[][] joined_points = new Point[2][2];
    BezierLine[] joinedLines = new BezierLine[2];


    public BezierLine addCompositeLine(Point selectedPoint) {
        if (is_composite == 2) return null;
        if (points.size() < 2) return null;

        Point pointNow = points.getFirst();
        if (selectedPoint == pointNow) {
            if (is_composite == -1) return null;
            if (is_composite == 1) is_composite = 2;
            else is_composite = -1;
            BezierLine line = new BezierLine(new Point(pointNow.x, pointNow.y));
            line.is_composite = -1;

            Point secPoint = points.get(1);
            line.joined_points[0][0] = pointNow;
            line.joined_points[0][1] = secPoint;
            joinedLines[0] = line;
            line.joinedLines[0] = this;
            line.points.add(new Point(2 * pointNow.x - secPoint.x, 2 * pointNow.y - secPoint.y));

            joined_points[0][0] = line.points.getFirst();
            joined_points[0][1] = line.points.getLast();
            return line;
        } else {
            pointNow = points.getLast();
            if (selectedPoint == pointNow) {
                if (is_composite == 1) return null;
                if (is_composite == -1) is_composite = 2;
                else is_composite = 1;
                BezierLine line = new BezierLine(new Point(pointNow.x, pointNow.y));
                line.is_composite = -1;

                Point secPoint = points.get(points.size() - 2);
                line.joined_points[0][0] = pointNow;
                line.joined_points[0][1] = secPoint;
                line.joinedLines[0] = this;
                line.points.add(new Point(2 * pointNow.x - secPoint.x, 2 * pointNow.y - secPoint.y));

                joined_points[1][0] = line.points.getFirst();
                joined_points[1][1] = line.points.getLast();
                joinedLines[1] = line;
                return line;
            }
        }
        return null;
    }

    public BezierLine(Point p0) {
        points = new ArrayList<>();
        points.add(p0);
    }

    public BezierLine(List<Point> points) {
        this.points = points;
    }

    public void drawBezierPoints(int count, Graphics g) {
        if (count <= 1) return;
        Point[] resPoints = BezierPoints(points, count);
        if (resPoints.length == 0) return;

        int r = 1;
        g.setColor(Color.black);
        Point lastPoint = null;
        for (Point point : resPoints) {
            if (lastPoint != null) g.drawLine(lastPoint.x, lastPoint.y, point.x, point.y);
            lastPoint = point;
        }
    }

    public void drawDottedLine(int x1, int y1, int x2, int y2, int len, Graphics g) {
        if (len <= 0) return;
        double lineLen = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        int count = (int) (lineLen / (len * len));
        if (count < 7) count = 7;

        double dx = (double) (x2 - x1) / count, dy = (double) (y2 - y1) / count;
        double lastX = x1, lastY = y1, newX, newY;

        for (int i = 0; i < count; i += 2) {
            newX = lastX + dx;
            newY = lastY + dy;
            g.drawLine((int) lastX, (int) lastY, (int) newX, (int) newY);
            lastX = newX + dx;
            lastY = newY + dy;
        }
    }

    public Point[] BezierPoints(List<Point> ys, int count) {
        if (ys.isEmpty()) return new Point[0];

        double k = 1. / (count - 1);
        int n = ys.size(), cnt = n * (n + 1) / 2;
        Point[] res = new Point[count];
        Point2D[] arrY = new Point2D[cnt];

        for (int i = 0; i < n; i++)
            arrY[i] = new Point2D(ys.get(i));

        double x = 0, mx = 1;
        int pos = n, index = 0;
        res[0] = ys.getFirst();

        for (int c = 1; c < count - 1; c++) {
            x += k;
            mx -= k;
            for (int j = n - 1; j > 0; j--) {
                for (int i = 0; i < j; i++) {
                    arrY[pos] = Point2D.add(Point2D.multiply(arrY[index], mx), Point2D.multiply(arrY[index + 1], x));
                    index++;
                    pos++;
                }
                index++;
            }
            res[c] = new Point((int) arrY[cnt - 1].x, (int) arrY[cnt - 1].y);
            pos = n;
            index = 0;
        }
        res[count - 1] = ys.getLast();
        return res;
    }

    public Point2D[] getDerivative(double x) {
        if (points.size() < 2) return new Point2D[]{new Point2D(0, 0), new Point2D(0, 0)};

        int n = points.size(), cnt = n * (n + 1) / 2;
        Point2D[] arrY = new Point2D[cnt];
        int pos = n, index = 0;
        double mx = 1 - x;

        for (int i = 0; i < n; i++)
            arrY[i] = new Point2D(points.get(i));

        for (int j = n - 1; j > 0; j--) {
            for (int i = 0; i < j; i++) {
                arrY[pos] = Point2D.add(Point2D.multiply(arrY[index], mx), Point2D.multiply(arrY[index + 1], x));
                index++;
                pos++;
            }
            index++;
        }
        return new Point2D[]{new Point2D((arrY[cnt - 2].x - arrY[cnt - 3].x) * n, (arrY[cnt - 2].y - arrY[cnt - 3].y) * n),
                new Point2D((int) arrY[cnt - 1].x, (int) arrY[cnt - 1].y)
        };
    }

    public boolean drawTangent(double t, Color lineColor, Graphics g, int width, int height) {
        if (t < 0 || t > 1) return false;

        Point2D[] der = getDerivative(t);
        if (der[0].x == 0 && der[0].y == 0) return false;


        g.setColor(lineColor);
        if (Math.abs(der[0].x) <= 1e-5) {
            if (Math.abs(der[0].y) <= 1e-5) return false;
            double k = der[0].x / der[0].y, b;
            b = der[1].x - der[1].y * k;
            g.drawLine((int) b, 0, (int) (k * height + b), height);
            return true;
        }
        double k = der[0].y / der[0].x, b;
        b = der[1].y - der[1].x * k;
        g.drawLine(0, (int) b, width, (int) (k * width + b));
        return true;
    }

    private void redrawWithColors(Color pointC, Color line, Graphics g, int width, int height) {
        if (points.isEmpty())
            return;

        Point lastPoint = points.getFirst();
        Point pointNow;

        g.setColor(line);
        for (int i = 1; i < points.size(); i++) {
            pointNow = points.get(i);
            drawDottedLine(lastPoint.x, lastPoint.y, pointNow.x, pointNow.y, 100, g);
            lastPoint = pointNow;
        }

        g.setColor(pointC);
        for (Point point : points)
            g.fillOval(point.x - pointSize[0] / 2, point.y - pointSize[1] / 2, pointSize[0], pointSize[1]);
        g.setColor(Color.white);
        lastPoint = points.getLast();
        g.fillOval(lastPoint.x - pointSize[0] / 2 + 2, lastPoint.y - pointSize[1] / 2 + 2, pointSize[0] - 4, pointSize[1] - 4);
    }

    public Point proec(Point p1, Point p2, Point p3) {
        Point2D n1 = new Point2D(p1);
        Point2D n2 = new Point2D(p2);
        Point2D n3 = new Point2D(p3);

        Point2D vec = Point2D.div(n2, n1);
        Point2D t1 = Point2D.div(n3, n1);

        double len2 = vec.x * vec.x + vec.y * vec.y;
        if (len2 == 0) return null;

        double dot = t1.x * vec.x + t1.y * vec.y;

        double t = dot / len2;

        Point2D projection = Point2D.add(n1, Point2D.multiply(vec, t));

        return projection.toIntegerPoint(1);
    }

    public boolean shift_compose(Point selectedPoint, Point ePoint, int type_m) {
        if (type_m == 0) {
            if ((is_composite == -1 || is_composite == 2) && points.getFirst() == selectedPoint) {
                Point res = proec(joined_points[0][1], points.get(1), ePoint);
                if (res == null) return true;
                Point p1 = points.getFirst();
                p1.x = res.x;
                p1.y = res.y;
                joined_points[0][0].x = res.x;
                joined_points[0][0].y = res.y;
                return false;
            }
            else if (is_composite > 0 && points.getLast() == selectedPoint) {
                Point res = proec(joined_points[1][1], points.get(points.size() - 2), ePoint);
                if (res == null) return true;
                Point p1 = points.getLast();
                p1.x = res.x;
                p1.y = res.y;
                joined_points[1][0].x = res.x;
                joined_points[1][0].y = res.y;
                return false;
            }

        }
        else {
            Point[] pp = null;
            if ((is_composite == -1 || is_composite == 2) && points.getFirst() == selectedPoint)
                pp = new Point[]{joined_points[0][0], joined_points[0][1], points.get(1)};
            else if (is_composite > 0 && points.getLast() == selectedPoint)
                pp = new Point[]{joined_points[1][0], joined_points[1][1], points.get(points.size() - 2)};

            if (pp != null) {
                for (Point p : pp) {
                    p.x += ePoint.x - selectedPoint.x;
                    p.y += ePoint.y - selectedPoint.y;
                }
            }
        }
        return true;
    }

    public void move_not_end_compose(Point selectedPoint, Point ePoint, int type_m) {
        if (type_m == 0) {
            if ((is_composite == -1 || is_composite == 2) && points.get(1) == selectedPoint)
                setIf_composite(selectedPoint, ePoint, 0);
            if (is_composite > 0 && points.get(points.size() - 2) == selectedPoint)
                setIf_composite(selectedPoint, ePoint, 1);
        } else {
            if ((is_composite == -1 || is_composite == 2) && points.get(1) == selectedPoint) {
                if (rotate_triangle(selectedPoint, ePoint, type_m, 0))
                    move_compose(points.getFirst(), selectedPoint, joined_points[0][1], ePoint);
            }
            if (is_composite > 0 && points.get(points.size() - 2) == selectedPoint) {
                if (rotate_triangle(selectedPoint, ePoint, type_m, 1))
                    move_compose(points.getLast(), selectedPoint, joined_points[1][1], ePoint);
            }
        }
    }

    private void setIf_composite(Point selectedPoint, Point ePoint, int lr) {
        try {
            selectedPoint.x = ePoint.x;
            selectedPoint.y = ePoint.y;
            Point2D next = new Point2D(joined_points[lr][1]);
            Point np = Point2D.add(Point2D.multiply(Point2D.div(new Point2D(selectedPoint), next).normalize(), Point2D.div(new Point2D(joined_points[lr][0]), next).length()), next).toIntegerPoint(1);
            Point first =  lr == 0 ? points.getFirst() : points.getLast();
            joined_points[lr][0].x = np.x;
            joined_points[lr][0].y = np.y;
            first.x = np.x;
            first.y = np.y;
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    private void move_compose(Point sp, Point next, Point prev, Point ePoint) {
        try {
            next.x = ePoint.x;
            next.y = ePoint.y;

            Point2D sp2D = new Point2D(sp),
                    next2D = new Point2D(next),
                    prev2D = new Point2D(prev);
            double t = Point2D.div(sp2D, prev2D).length();
            Point t2 = Point2D.add(sp2D, Point2D.multiply(Point2D.div(sp2D, next2D).normalize(), t)).toIntegerPoint(1);
            prev.x = t2.x;
            prev.y = t2.y;
        } catch (Exception exception) {
            // теоретически должно быть, но тогда нужно сделать то, как оттаскивать эти точки
            /*selected.joined_points[0][1].x = ePoint.x;
            selected.joined_points[0][1].y = ePoint.y;*/
            System.out.println(exception.getMessage());
        }
    }

    public boolean rotate_triangle(Point selectedPoint, Point ePoint, int type_m, int lr) {
        try {
            BezierLine s2 = joinedLines[lr];
            if (s2 != null && s2.points.size() == 3) {
                if (type_m == 0) {
                    selectedPoint.x = ePoint.x;
                    selectedPoint.y = ePoint.y;
                    Point2D next = new Point2D(joined_points[lr][1]);
                    Point np = Point2D.add(Point2D.multiply(Point2D.div(new Point2D(selectedPoint), next).normalize(), Point2D.div(new Point2D(joined_points[lr][0]), next).length()), next).toIntegerPoint(1);
                    Point first = points.getFirst();
                    joined_points[lr][0].x = np.x;
                    joined_points[lr][0].y = np.y;
                    first.x = np.x;
                    first.y = np.y;
                    return false;
                } else {
                    if (s2.joinedLines[1] != null && s2.joinedLines[1] != this) {
                        move_compose(points.getLast(), selectedPoint, joined_points[lr][1], ePoint);
                        move_compose(s2.points.getLast(), joined_points[lr][1], s2.joined_points[1][1], ePoint);
                        return false;
                    } else if (s2.joinedLines[0] != null && s2.joinedLines[0] != this) {
                        move_compose(points.getFirst(), selectedPoint, joined_points[lr][1], ePoint);
                        move_compose(s2.points.getFirst(), joined_points[lr][1], s2.joined_points[0][1], ePoint);
                        return false;
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        return true;
    }

    public void redrawLines(Graphics g, int w, int h) {
        redrawWithColors(pointColor, lineColor, g, w, h);
    }

    public Point getNearPoint(Point p0, int dist) {
        double d2 = dist * dist;

        for (Point point : points) {
            if ((point.x - p0.x) * (point.x - p0.x) + (point.y - p0.y) * (point.y - p0.y) <= d2)
                return point;
        }
        return null;
    }
}
