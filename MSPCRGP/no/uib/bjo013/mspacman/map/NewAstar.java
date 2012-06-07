package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class NewAstar {
	public static List<Node> computePath(Node start, Node goal, Map<Node, Double> weights) {
		Map<Node, Node> path = new HashMap<Node, Node>();
		Set<Node> closedSet = new HashSet<Node>();
		PriorityQueue<Node> openSet = new PriorityQueue<Node>();
		
		start.setH(calculateHeuristics(start.p, goal.p));
		start.setF();
		openSet.add(start);
		
		while(!openSet.isEmpty()) {
			Node current = openSet.poll();
			if(current.equals(goal)) {
				LinkedList<Node> ret = new LinkedList<Node>();
				for (Node p = current; !p.equals(start) && path.containsKey(p); p = path.get(p)) {
					ret.offerFirst(p);
				}
				return ret;
			}
			
			closedSet.add(current);
			
			List<Node> successors = current.getNeighbors();
			for (Node neighbor : successors) {
				if (closedSet.contains(neighbor)) {
					continue;
				} else {
					double tentativeGScore = 
							current.getG() 
							+ (weights.containsKey(neighbor) ? weights.get(neighbor) : 0)
							+ calculateHeuristics(neighbor.p, goal.p);
					boolean tentativeIsBetter = false;
					if (!openSet.contains(neighbor)) {
						neighbor.setH(calculateHeuristics(neighbor.p, goal.p));
						openSet.add(neighbor);
						tentativeIsBetter = true;
					} else if (tentativeGScore < neighbor.getG()) {
						tentativeIsBetter = true;
					}
					if (tentativeIsBetter) {
						neighbor.setG(tentativeGScore);
						neighbor.setF();
						path.put(neighbor, current);
					}
				}
			}
		}
		
		
		return new LinkedList<Node>();
	}
	
	private static Double calculateHeuristics(Point from, Point to) {
		if (from.x == 223 && to.x == 0) {
			return 1.0;
		} else if (from.x == 0 && to.x == 223) {
			return 1.0;
		}
		
		return from.distance(to);
	}
}