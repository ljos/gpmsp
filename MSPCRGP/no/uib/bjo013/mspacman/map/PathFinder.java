package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PathFinder extends AStar<Point> {
	private static Point target;
	private static Set<Point> open;

	public PathFinder(Set<Point> map) {
		PathFinder.open = map;
	}

	@Override
	protected boolean isGoal(Point node) {
		return node.equals(target);
	}

	@Override
	protected Double g(Point from, Point to) {
		if (from.x == to.x && from.y == to.y) {
			return 0.0;
		}

		if (open.contains(to)) {
			return 1.0;
		}

		return Double.MAX_VALUE;
	}

	@Override
	protected Double h(Point from, Point to) {
		return new Double(Math.abs(224 - 1 - to.x) + Math.abs(288 - 1 - to.y));
	}

	@Override
	protected List<Point> generateSuccessors(Point node) {
		List<Point> ls = new LinkedList<Point>();
		List<Point> ret = new LinkedList<Point>();
		int x = node.x;
		int y = node.y;
		if (x >= 0 && x < 224 && y > 23 && y < 288) {
			int x1 = x-1;
			if(x1 < 0) {
				ls.add(new Point(223, y));
			} else {
				ls.add(new Point(x1, y));
			}
			x1=x+1;
			if(x > 222) {
				ls.add(new Point(0, y));
			} else {
				ls.add(new Point(x1, y));
			}
			ls.add(new Point(x, y + 1));
			ls.add(new Point(x, y - 1));
			for (Point p : ls) {
				if (open.contains(p)) {
					ret.add(p);
				} 
			}
		}

		return ret;
	}

	public static void setTarget(Point target) {
		PathFinder.target = target;
	}

	public static void setOpen(Set<Point> open) {
		PathFinder.open = open;
	}

}
