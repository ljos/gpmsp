package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Node implements Comparable<Node> {
	public Point p;
	private double gScore;
	private double hScore;
	private double fScore;
	private GameEntity ge = GameEntity.NONE;
	List<Node> neighbors = new ArrayList<Node>();
	
	public Node(Point p) {
		this.p = p;
	}
	
	public void setGameEntity(GameEntity ge) {
		this.ge = ge;
	}
	
	public GameEntity getGameEntity() {
		return ge;
	}
	
	public void addNeighbor(Node neighbor) {
		this.neighbors.add(neighbor);
	}
	
	public List<Node> getNeighbors() {
		return neighbors;
	}
	
	public void setG(double gScore) {
		this.gScore = gScore;
	}
	
	public double getG() {
		return gScore;
	}
	
	public void setH(double hScore) {
		this.hScore = hScore;
	}
	
	public double getH() {
		return hScore;
	}
	
	public void setF() {
		this.fScore = gScore + hScore;
	}
	
	public double getF() {
		return fScore;
	}
	
	@Override
	public int compareTo(Node o) {
		if (this.fScore <= o.getF()) {
			return -1;
		} else if (this.fScore > o.getF()) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public boolean equals(Node o) {
		return this.p.equals(o.p);
	}
}
