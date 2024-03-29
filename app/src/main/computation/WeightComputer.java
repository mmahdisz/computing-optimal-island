package main.computation;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import main.graph.*;
import main.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeightComputer {

    private final Graph graph;
    private final List<Vertex> blueList;
    private final PreProcessor preProcessor;
    private List<Vertex> optimalIslandVerticesList;

    public WeightComputer(@NotNull Graph graph) {
        this.graph = graph;
        graph.sortY();
        System.out.println(graph.getVertexList().toString());
        preProcessor = new PreProcessor(graph);

        blueList = new ArrayList<>();
        for (Vertex v : graph.getVertexList())
            if (v.getCircle().getFill().equals(Color.BLUE))
                blueList.add(v);
    }

    public void run() {
        List<Point> pointsBelowP = new ArrayList<>();
        for (int i = 0; i < blueList.size(); i++) {
            if (i == blueList.size() - 1) continue;

            Vertex p = blueList.get(i);
            List<Point> orderedPoints = orderPointsBelowHp(i, p);
            List<Edge> usableEdgesBelowHp = usableEdgesBelowHp(p, orderedPoints);

            if (usableEdgesBelowHp == null || usableEdgesBelowHp.size() == 0) continue;

            pointsBelowP = processEdgesContainsP(p, orderedPoints, usableEdgesBelowHp);
            p.setBelowPointsList(pointsBelowP);
        }

        // Finding an edge with maximum weight possible for result
        int max = 0;
        Edge maxE = null;
        Vertex maxP = null;
        for (Vertex p : blueList) {
            if (p == null || p.getBelowPointsList() == null) continue;
            for (Point pi : p.getBelowPointsList()) {
                if (pi == null || pi.getLbi() == null) continue;
                for (Edge e : pi.getLbi()) {
                    if (e == null) continue;
                    if (max <= e.getWeight()) {
                        max = e.getWeight();
                        maxE = e;
                        maxP = p;
                    }
                }
            }
        }
        if (max == 0) return;
        System.out.println("\n\n" + maxP);
        System.out.println(maxE);
        System.out.println(max);

        // Finding borders of Optimal Island
        optimalIslandVerticesList = new ArrayList<>();
        addIslandBorder(maxP, maxE.getQ());
        while (maxE != null) {
            addIslandBorder(maxE.getP(), maxE.getQ());
            if (maxE.getPrev() == null)
                addIslandBorder(maxP, maxE.getP());
            maxE = maxE.getPrev();
        }
        graph.setMaxW(max);
        System.out.println("\noptimalIslandVerticesList => " + optimalIslandVerticesList);
    }

    private void addIslandBorder(@NotNull Vertex v1,@NotNull Vertex v2) {
        if (!optimalIslandVerticesList.contains(v1))
            optimalIslandVerticesList.add((v1 instanceof Point) ? new Vertex(v1.getCircle(), v1.getGlobalLabel()) : v1);
        if (!optimalIslandVerticesList.contains(v2))
            optimalIslandVerticesList.add((v2 instanceof Point) ? new Vertex(v2.getCircle(), v2.getGlobalLabel()) : v2);
        Line line = new Line(v1.getCircle().getCenterX(), v1.getCircle().getCenterY()
                , v2.getCircle().getCenterX(), v2.getCircle().getCenterY());
        line.setStroke(Color.BLUE);
        graph.addLine(line);
        graph.addBorder("\n" + v1.getGlobalLabel() + " to " + v2.getGlobalLabel());
    }

    // order points below p in circular order base on angel
    private @NotNull List<Point> orderPointsBelowHp(int pIndex, Vertex mP) {
        List<Point> positiveA = new ArrayList<>();
        List<Point> negativeA = new ArrayList<>();

        for (int i = pIndex + 1; i < blueList.size(); i++) {
            Vertex vertex = blueList.get(i);
            if (!vertex.getCircle().getFill().equals(Color.BLUE)) continue;
            double angel = Utils.calculateAofLine(vertex, mP);

            Point point = new Point(vertex.getCircle(), vertex.getGlobalLabel());

            // prevent small data error
            if (angel == 0.0)
                if (mP.getCircle().getCenterX() < vertex.getCircle().getCenterX())
                    angel = 0.0001;
                else
                    angel = -1000;
            point.setAngel(angel);

            // we should order them separately because we need negative angels first
            if (angel > 0)
                positiveA.add(point);
            else
                negativeA.add(point);
        }
        positiveA.sort((p1, p2) -> (int) ((p2.getAngel() - p1.getAngel()) * 10000));
        negativeA.sort((p1, p2) -> (int) ((p2.getAngel() - p1.getAngel()) * 10000));

        List<Point> orderPointsBelowHp = new ArrayList<>();
        orderPointsBelowHp.addAll(negativeA);
        orderPointsBelowHp.addAll(positiveA);

        return orderPointsBelowHp;
    }

    // ignoring edges which their △ has at least one point with colors other than blue
    private @Nullable List<Edge> usableEdgesBelowHp(Vertex p, @NotNull List<Point> orderedPoints) {
        if (orderedPoints.size() < 2) return null;
        List<Edge> usableEdgesBelowHp = new ArrayList<>();

        for (int i = 0; i < orderedPoints.size() - 1; i++) {
            for (int j = i + 1; j < orderedPoints.size(); j++) {
                Delta delta = new Delta(p, orderedPoints.get(i), orderedPoints.get(j));

                if (PreProcessor.hasOnlyBluePoint(delta, graph.getVertexList())) {
                    Edge edge = new Edge(orderedPoints.get(i), orderedPoints.get(j));
                    usableEdgesBelowHp.add(edge);
                }

            }
        }
        return usableEdgesBelowHp;
    }

    private @NotNull List<Point> processEdgesContainsP(Vertex p, @NotNull List<Point> orderedPoints, List<Edge> fineEdgesBelowHp) {
        List<Point> pointsContainsWeightedEdges = new ArrayList<>();
        for (Point pi : orderedPoints) {
            // we should order them separately
            List<Edge> LaiFirst = new ArrayList<>();    // page 5 of paper, list of incoming edges to Pi; La,i = {a1,i , ... , aq,i}
            List<Edge> LaiNext = new ArrayList<>();     // page 5 of paper, list of incoming edges to Pi; La,i = {a1,i , ... , aq,i}
            List<Edge> LbiFirst = new ArrayList<>();    // page 5 of paper, list of outgoing edges to Pi; Lb,i = {b1,i , ... , bq,i}
            List<Edge> LbiNext = new ArrayList<>();     // page 5 of paper, list of outgoing edges to Pi; Lb,i = {b1,i , ... , bq,i}

            for (Edge e : fineEdgesBelowHp) {
                if (e.getQ().equals(pi)) {                                                      // incoming
                    if (pi.getCircle().getCenterX() < p.getCircle().getCenterX()) {             // pi is left of p
                        if (pi.getCircle().getCenterX() < e.getP().getCircle().getCenterX())    // incoming point is top right of pi
                            LaiFirst.add(e);
                        else                                                                    // incoming point is top left of pi
                            LaiNext.add(e);
                    } else {                                                                    // pi is right of p
                        if (pi.getCircle().getCenterX() < e.getP().getCircle().getCenterX())    // incoming point is bottom right of pi
                            LaiNext.add(e);
                        else                                                                    // incoming point is bottom left of pi
                            LaiFirst.add(e);
                    }
                } else if (e.getP().equals(pi)) {                                               // outgoing
                    if (pi.getCircle().getCenterX() < p.getCircle().getCenterX()) {             // pi is left of p
                        if (pi.getCircle().getCenterX() < e.getQ().getCircle().getCenterX())    // outgoing point is bottom right of pi
                            LbiFirst.add(e);
                        else                                                                    // outgoing point is bottom left of pi
                            LbiNext.add(e);
                    } else {                                                                    // pi is right of p
                        if (pi.getCircle().getCenterX() < e.getQ().getCircle().getCenterX())    // outgoing point is top right of pi
                            LbiNext.add(e);
                        else                                                                    // outgoing point is top left of pi
                            LbiFirst.add(e);
                    }
                }

                double angel = Utils.calculateAofLine(e.getP(), e.getQ());
                e.setAngel(angel);
            }

            LaiFirst.sort((o1, o2) -> (int) ((o2.getAngel() - o1.getAngel()) * 10000));
            LaiNext.sort((o1, o2) -> (int) ((o2.getAngel() - o1.getAngel()) * 10000));
            LbiFirst.sort((o1, o2) -> (int) ((o1.getAngel() - o2.getAngel()) * 10000));
            LbiNext.sort((o1, o2) -> (int) ((o1.getAngel() - o2.getAngel()) * 10000));

            List<Edge> Lai = new ArrayList<>();
            Lai.addAll(LaiFirst);
            Lai.addAll(LaiNext);
            List<Edge> Lbi = new ArrayList<>();
            Lbi.addAll(LbiFirst);
            Lbi.addAll(LbiNext);

            observation2(p, pi, orderedPoints.subList(0, orderedPoints.indexOf(pi))
                    , Lai, Lbi);
            pointsContainsWeightedEdges.add(pi);  // for point pi
        }
        return pointsContainsWeightedEdges;
    }

    private Point observation2(Vertex p, Point pi, List<Point> prevPiz, List<Edge> Lai, @NotNull List<Edge> Lbi) {
        for (Point prevPi : prevPiz) {
            for (Edge bi : prevPi.getLbi()) {
                for (Edge ai : Lai) {
                    if (bi.getP().getGlobalLabel().equals(ai.getP().getGlobalLabel())
                            && bi.getQ().getGlobalLabel().equals(pi.getGlobalLabel()))
                        ai.setWeight(bi.getWeight());
                }
            }
        }

        for (Edge bmi : Lbi) {
            int smIndex = -1;
            Delta deltaB = new Delta(p, bmi.getP(), bmi.getQ());
            for (int i = 0; i < Lai.size(); i++) {
                Delta deltaA = new Delta(p, Lai.get(i).getP(), Lai.get(i).getQ());
                if (!preProcessor.isPCompatible(deltaA, deltaB)) continue;
                smIndex = i;
            }
            if (smIndex == -1)
                bmi.setWeight(preProcessor.BlueDelta(deltaB, graph.getVertexList()));
            else {
                int max = Lai.get(0).getWeight();
                int hsm = 0;
                for (int i = 0; i < smIndex + 1; i++) {
                    if (Lai.get(i).getWeight() > max) {
                        hsm = i;
                        max = Lai.get(i).getWeight();
                    }
                }
                bmi.setPrev(Lai.get(hsm));
                for (Point prevPi : prevPiz) {
                    for (Edge bi : prevPi.getLbi()) {
                        if (bi.getQ().getGlobalLabel().equals(pi.getGlobalLabel())
                                && bi.getP().getGlobalLabel().equals(bmi.getPrev().getP().getGlobalLabel())) {
                            bmi.setWeight(bi.getWeight() + preProcessor.BlueDelta(deltaB, graph.getVertexList()) - 2);
                            bmi.setPrev(bi);
                        }
                    }
                }
            }
        }

        pi.setLai(Lai);
        pi.setLbi(Lbi);
        return pi;
    }

}
