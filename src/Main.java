import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class Main extends JFrame {
    Point2D selectedPoint = null;
    JPanel canvas;
    List<BezierLine> bezierLines = new ArrayList<>();
    int selectedLine = -1;
    Color lineColor = Color.black, pointColor = Color.green, bgColor = Color.WHITE;
    int[] pointSize = new int[]{12, 12};
    int bezierCount = 100;
    boolean isShowPoints = true;
    boolean is_draw_dev = false;
    int count_dev = 3;
    List<Point2D> connectedPoints = null;
    boolean connect_points = false;
    Point2D position = new Point2D(0, 0);
    Point2D move_speed = new Point2D(-20, -20);
    Point2D window_half_size;
    double scale = 1;
    boolean scale_changed = true;

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public Main(String title) throws IOException {
        super(title);
        int w = 1200, h = 600;
        setSize(w, h);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel canvasPanel = new JPanel(true);

        canvas = new MyPanel(true);
        canvas.setPreferredSize(new Dimension(w - 20, h - 100));
        canvas.setBackground(Color.WHITE);

        window_half_size = new Point2D((w - 20) / 2., (h - 100) / 2.);

        JButton buttonSave = new JButton(),
                hidePointsButton = new JButton(),
                importFileButton = new JButton(),
                deletePointButton = new JButton();

        Action saveAction = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveAsFile();
                System.out.println("saved");
            }
        }, hideAction = new AbstractAction("Visibility") {
            @Override
            public void actionPerformed(ActionEvent e) {
                isShowPoints = !isShowPoints;
                canvas.repaint();
            }
        }, connect_button_action = new AbstractAction("ConnectSwitch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect_points = !connect_points;
            }
        }, importAction = new AbstractAction("Import") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportFromFile();
                System.out.println("imported");
            }
        }, deletePointAction = new AbstractAction("DeletePoint") {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedPoint();
            }
        }, connectPointsAction = new AbstractAction("ConnectPoints") {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (bezierLines.isEmpty()) return;
                if (selectedPoint == null) return;
                if (connectedPoints != null) {
                    for (Point2D p : connectedPoints) {
                        p.x = selectedPoint.x;
                        p.y = selectedPoint.y;
                    }
                    return;
                }

                int r = 20;
                r = r * r;
                connectedPoints = new ArrayList<>();
                for (BezierLine line : bezierLines)
                    for (Point2D point : line.points)
                        if ((point.x - selectedPoint.x) * (point.x - selectedPoint.x) + (point.y - selectedPoint.y) * (point.y - selectedPoint.y) <= r && point != selectedPoint) {
                            point.x = selectedPoint.x;
                            point.y = selectedPoint.y;
                            connectedPoints.add(point);
                        }
                repaint();
            }
        };

        buttonSave.setAction(saveAction);
        //saveAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        buttonSave.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "Save");
        buttonSave.getActionMap().put("Save", saveAction);

        hidePointsButton.setAction(hideAction);
        hidePointsButton.getActionMap().put("Visibility", hideAction);
        hidePointsButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "Visibility");

        importFileButton.setAction(importAction);

        deletePointButton.setAction(deletePointAction);
        deletePointButton.getActionMap().put("DeletePoint", deletePointAction);
        deletePointButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DeletePoint");

        canvas.setLocation(0, 50);

        canvasPanel.add(canvas);
        Container content = getContentPane();
        canvasPanel.add(buttonSave);
        canvasPanel.add(hidePointsButton);
        canvasPanel.add(importFileButton);
        canvasPanel.add(deletePointButton);

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "ConnectPoints");
        canvasPanel.getActionMap().put("ConnectPoints", connectPointsAction);

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "ConnectSwitch");
        canvasPanel.getActionMap().put("ConnectSwitch", connect_button_action);


        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, true), "ReleaseE");
        canvasPanel.getActionMap().put("ReleaseE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectedPoints = null;
                canvas.repaint();
            }
        });

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true), "CreateComp");
        canvasPanel.getActionMap().put("CreateComp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedPoint == null) return;
                if (selectedLine == -1) return;
                BezierLine res = bezierLines.get(selectedLine).addCompositeLine(selectedPoint);
                if (res == null) return;
                bezierLines.add(res);
                canvas.repaint();
            }
        });

        bind_actions(canvasPanel);

        content.add(BorderLayout.CENTER, canvasPanel);

        setVisible(true);
    }

    private void bind_actions(JPanel canvasPanel) {
        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "MoveUp");
        canvasPanel.getActionMap().put("MoveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                position.y -= move_speed.y * scale;
                canvas.repaint();
            }
        });

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "MoveDown");
        canvasPanel.getActionMap().put("MoveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                position.y += move_speed.y * scale;
                canvas.repaint();
            }
        });

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "MoveLeft");
        canvasPanel.getActionMap().put("MoveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                position.x -= move_speed.x * scale;
                canvas.repaint();
            }
        });


        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "MoveRight");
        canvasPanel.getActionMap().put("MoveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                position.x += move_speed.x * scale;
                canvas.repaint();
            }
        });

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ScalePlus");
        canvasPanel.getActionMap().put("ScalePlus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scale_changed = true;
                scale *= 1.1;
                if (scale > 1000) scale = 1000;
                canvas.repaint();
            }
        });
        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "ScaleMin");
        canvasPanel.getActionMap().put("ScaleMin", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scale_changed = true;
                scale /= 1.1;
                if (scale < 0.001) scale = 0.001;
                canvas.repaint();
            }
        });
    }


    public static void main(String[] args) throws IOException {
        Main main = new Main("Первый тест курсового проекта");

    }

    public void deleteSelectedPoint() {
        if (selectedPoint == null || selectedLine == -1) return;

        // потом придумать, как лучше удалять или точнее - что делать при удалении точек участвующих в создании составных кривых
        BezierLine line = bezierLines.get(selectedLine);
        if ((line.is_composite == -1 || line.is_composite == 2) &&
                (selectedPoint == line.points.getFirst() || selectedPoint == line.points.get(1)))
            return;

        if ((line.is_composite == 1 || line.is_composite == 2) &&
                (selectedPoint == line.points.getLast() || selectedPoint == line.points.get(line.points.size() - 2)))
            return;

        if (line.points.size() == 1) {
            bezierLines.remove(selectedLine);
            selectedLine = -1;
        } else {
            line.points.remove(selectedPoint);
        }
        selectedPoint = null;
        connectedPoints = null;
        repaint();
    }

    public void SaveAsFile() {
        JFileChooser j = new JFileChooser();

        if (j.showSaveDialog(null) == 0) {
            String filePath = j.getSelectedFile().getAbsolutePath();

            try (FileWriter fw = new FileWriter(filePath)) {
                fw.write(bezierLines.size() + "\n");
                for (BezierLine bez : bezierLines) {
                    fw.write(bez.points.size() + "|");
                    for (Point2D point : bez.points) {
                        fw.write(point.x + " " + point.y);
                        fw.write("|");
                    }
                    fw.write('\n');
                }
            } catch (
                    IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void ImportFromFile() {
        JFileChooser j = new JFileChooser();

        if (j.showOpenDialog(null) == 0) {
            String filePath = j.getSelectedFile().getAbsolutePath();
            if (j.getSelectedFile().isFile()) {
                try (BufferedReader fw = new BufferedReader(new FileReader(filePath))) {
                    int countBeziers = Integer.parseInt(fw.readLine().strip()), pointsCount;
                    List<Point2D> bez;
                    String[] points;
                    String[] point;
                    for (int i = 0; i < countBeziers; i++) {
                        points = fw.readLine().strip().split("\\|");
                        pointsCount = Integer.parseInt(points[0]);
                        bez = new ArrayList<>();
                        for (int k = 1; k < pointsCount + 1; k++) {
                            point = points[k].split(" ");
                            bez.add(new Point2D(Double.parseDouble(point[0]), Double.parseDouble(point[1])));
                        }
                        bezierLines.add(new BezierLine(bez));
                    }
                    selectedLine = -1;
                    selectedPoint = null;
                    connectedPoints = null;
                    canvas.repaint();
                } catch (
                        IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public class MyPanel extends JPanel implements MouseListener, MouseMotionListener {

        public MyPanel(boolean isDoubleBuffered) {
            super(isDoubleBuffered);
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(bgColor);
            int w = this.getWidth(), h = this.getHeight();
            g.fillRect(0, 0, w, h);
            BezierLine line;
            for (int i = 0; i < bezierLines.size(); i++) {
                line = bezierLines.get(i);
                if (isShowPoints) {
                    if (selectedLine == i) line.pointColor = Color.GREEN;
                    else line.pointColor = Color.DARK_GRAY;
                    line.redrawLines(window_half_size, scale, position, g, w, h);
                }
                if (selectedLine == i && is_draw_dev) {
                    double step = (double) 1 / (count_dev + 1);
                    for (int k = 1; k <= count_dev; k++)
                        line.drawTangent(k * step, Color.DARK_GRAY, g, w, h);
                }

                line.drawBezierPoints(window_half_size, scale, position, bezierCount, g, scale_changed || line.need_redraw);

                line.need_redraw = false;
            }
            scale_changed = false;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == 3) {
                bezierLines.add(new BezierLine(new Point2D(e.getPoint()).transpose(position.u_minus(), 1 / scale, window_half_size.u_minus())));
                selectedLine = bezierLines.size() - 1;
            } else if (selectedLine >= 0 && bezierLines.get(selectedLine).is_composite < 1) {
                BezierLine bezierLine = bezierLines.get(selectedLine);
                bezierLine.points.add(new Point2D(e.getPoint()).transpose(position.u_minus(), 1 / scale, window_half_size.u_minus()));
                bezierLine.need_redraw = true;
            }
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (bezierLines.isEmpty()) return;

            //if (points.isEmpty()) return;
            Point2D pos = new Point2D(e.getPoint()).transpose(position.u_minus(), 1 / scale, window_half_size.u_minus()), sel;
            Point2D p2 = new Point2D(e.getPoint());
            int p = -1, d = Math.min(Math.max((int) ((pointSize[0] / 2. + 5) * scale), 1), 100);
            for (BezierLine line : bezierLines) {
                sel = line.getNearPoint(pos, d);
                //sel = line.getNearPoint_transpose(window_half_size, scale, position, p2, (pointSize[0] / 2. + 5));
                if (sel != null) {
                    line.need_redraw = true;
                    if (connect_points) {
                        if (selectedLine != -1 && selectedPoint != null) {
                            BezierLine l2 = bezierLines.get(selectedLine);
                            if (l2 != null) l2.need_redraw = true;
                            System.out.println("start");
                            if (line != l2 && l2 != null && l2.points.size() > 2 && line.points.size() > 2) {
                                int t1 = -1, t2 = -1;
                                if (selectedPoint == l2.points.getLast()) t1 = 1;
                                else if (selectedPoint == l2.points.getFirst()) t1 = 0;
                                if (sel == line.points.getLast()) t2 = 1;
                                else if (sel == line.points.getFirst()) t2 = 0;

                                if (t1 != -1 && t2 != -1 && l2.joinedLines[t1] == null && line.joinedLines[t2] == null) {
                                    BezierLine comp = new BezierLine(new Point2D(selectedPoint));
                                    Point2D p11 = l2.points.get(1 + t1 * (l2.points.size() - 3)), p21 = line.points.get(1 + t2 * (line.points.size() - 3));
//                                    comp.points.add(Point2D.find_intersect_point(p11, selectedPoint, p21, sel));
                                    comp.points.add(Point2D.div(Point2D.multiply(selectedPoint, 2), p11));
                                    comp.points.add(Point2D.div(Point2D.multiply(sel, 2), p21));

                                    comp.points.add(new Point2D(sel));
                                    comp.joined_points = new Point2D[][]{new Point2D[]{selectedPoint, p11}, new Point2D[]{sel, p21}};
                                    comp.joinedLines[0] = l2;
                                    comp.joinedLines[1] = line;
                                    l2.joinedLines[t1] = comp;
                                    line.joinedLines[t2] = comp;
                                    l2.joined_points[t1][0] = comp.points.getFirst();
                                    l2.joined_points[t1][1] = comp.points.get(1);
                                    line.joined_points[t2][0] = comp.points.getLast();
                                    line.joined_points[t2][1] = comp.points.get(comp.points.size() - 2);
                                    comp.is_composite = 2;
                                    l2.add_comp(t1);
                                    line.add_comp(t2);
                                    bezierLines.add(comp);

                                }

                            }
                        }
                        connect_points = false;
                        System.out.println("off");
                    }
                    selectedPoint = sel;
                    selectedLine = p + 1;
                    repaint();
                    return;
                }
                ++p;
            }

            selectedPoint = null;
            connectedPoints = null;

            /*for (Point point : points) {
                if ((point.x - pos.x) * (point.x - pos.x) + (point.y - pos.y) * (point.y - pos.y) <= (pointSize[0] / 2 + 5) * (pointSize[0] / 2 + 5)) {
                    selectedPoint = point;
                    return;
                }
            }*/
            //points.add(e.getPoint());
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point2D ePoint = new Point2D(e.getPoint()).transpose(position.u_minus(), 1 / scale, window_half_size.u_minus());
            if (selectedPoint != null) {
                int type_m = 0;
                BezierLine selected = bezierLines.get(selectedLine);
                boolean b = selected.shift_compose(selectedPoint, ePoint, type_m);
                selected.move_not_end_compose(selectedPoint, ePoint, type_m);
                if (b) {
                    selectedPoint.x = ePoint.x;
                    selectedPoint.y = ePoint.y;
                }
                selected.need_redraw = true;
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }
}