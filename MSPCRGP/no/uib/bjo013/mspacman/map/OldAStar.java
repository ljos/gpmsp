package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class OldAStar {
	private Map<Point, Double> open;
	private Map<Point, Point> cameFrom = new HashMap<Point, Point>();

	public OldAStar(Map<Point, Double> open) {
		this.open = new HashMap<Point, Double>(open);
	}

	private class Node implements Comparable<Node> {
		public Point p;
		public double g_score;
		public double h_score;
		public double f_score = Double.MAX_VALUE;
		public int n;

		public Node(Point p) {
			this.p = p;
		}

		@Override
		public int compareTo(Node o) {
			if (this.f_score <= o.f_score) {
				return -1;
			} else if (this.f_score > o.f_score) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Node) {
				return ((Node) o).p.equals(this.p);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.p.hashCode();
		}
	}

	public Double h(Point from, Point to) {
		if (from.x == 223 && to.x == 0) {
			return 1.0;
		} else if (from.x == 0 && to.x == 223) {
			return 1.0;
		}
		return from.distance(to);
	}

	protected Double g(Point from, Point to) {
		if (from.equals(to)) {
			return 0.0;
		}

		if (open.containsKey(to)) {
			return open.get(to);
		}

		return Double.MAX_VALUE;
	}

	protected List<Point> generateSuccessors(Point node) {
		List<Point> ls = new LinkedList<Point>();
		List<Point> ret = new LinkedList<Point>();
		int x = node.x;
		int y = node.y;
		if (x >= 0 && x < 224 && y > 23 && y < 288) {
			int x1 = x - 1;
			if (x1 < 0) {
				ls.add(new Point(223, y));
			} else {
				ls.add(new Point(x1, y));
			}
			x1 = x + 1;
			if (x1 >= 223) {
				ls.add(new Point(0, y));
			} else {
				ls.add(new Point(x1, y));
			}
			ls.add(new Point(x, y + 1));
			ls.add(new Point(x, y - 1));
			for (Point p : ls) {
				if (open.containsKey(p)) {
					ret.add(p);
				}
			}
		}

		return ret;
	}

	private List<Point> reconstructPath(Point start, Point current) {
		LinkedList<Point> ret = new LinkedList<Point>();
		for (Point p = current; !p.equals(start) && cameFrom.containsKey(p); p = cameFrom.get(p)) {
			ret.offerFirst(p);
		}
		return ret;
	}

	public List<Point> computePath(Point start, Point goal) {
		if (cameFrom.containsKey(goal) && cameFrom.containsKey(start)) {
			return reconstructPath(start, goal);
		}

		Set<Point> closedSet = new HashSet<Point>();
		PriorityQueue<Node> openSet = new PriorityQueue<Node>();
		Node node = this.new Node(start);
		node.g_score = 1.0;
		node.h_score = this.h(start, goal);
		node.f_score = node.g_score + node.h_score;
		node.n = 0;
		openSet.add(node);

		while (!openSet.isEmpty()) {
			Node current = openSet.poll();
			if (current.p.equals(goal)) {
				return this.reconstructPath(start, current.p);
			}

			closedSet.add(current.p);
			List<Point> successors = this.generateSuccessors(current.p);
			for (Point succ : successors) {
				Node neighbor = new Node(succ);
				if (closedSet.contains(neighbor.p)) {
					continue;
				} else {
					double tentative_g_score = current.g_score + g(current.p, neighbor.p)
							+ this.h(current.p, neighbor.p);
					boolean tentative_is_better = false;
					if (!openSet.contains(neighbor)) {
						neighbor.h_score = this.h(neighbor.p, goal);
						openSet.add(neighbor);
						tentative_is_better = true;
					} else if (tentative_g_score < neighbor.g_score) {
						tentative_is_better = true;
					}
					if (tentative_is_better) {
						neighbor.g_score = tentative_g_score;
						neighbor.f_score = neighbor.g_score + neighbor.h_score;
						neighbor.n = current.n + 1;
						cameFrom.put(neighbor.p, current.p);
					}
				}
			}
		}
		return new LinkedList<Point>();
	}

	public void resetSearch() {
		this.open.clear();
		this.cameFrom.clear();
	}

	public void adjustScores(Map<Point, Double> p) {
		open.putAll(p);
	}
}