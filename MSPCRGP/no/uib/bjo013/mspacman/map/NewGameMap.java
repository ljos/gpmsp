package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jef.video.BitMap;

public class NewGameMap {
	private BitMap bitmap;
	private TreeSet<Node> MAP = new TreeSet<Node>();
	
	private Map<Integer, Node> pills = new LinkedHashMap<Integer, Node>();
	private Map<Node, Integer> revpills = new HashMap<Node,Integer>();
	
	private Map<Integer, Node> superPills = new LinkedHashMap<Integer,Node>();
	private Map<Node, Integer> revsuperPills = new HashMap<Node, Integer>();
	
	private Node mspacman;
	private Node[] ghosts = new Node[4];
	private List<Node> blues = new ArrayList<Node>();

	public NewGameMap(BitMap bitmap) {
		this.bitmap = bitmap;
		for (int y = 124; y < 155; ++y) {
			for (int x = 84; x < 139; ++x) {
				bitmap.setPixel(x, y, 16711680);
			}
		}

		for (int y = 108; y < 122; ++y) {
			for (int x = 65; x < 157; ++x) {
				bitmap.setPixel(x, y, 0);
			}
		}
		for (int y = 156; y < 170; ++y) {
			for (int x = 80; x < 145; ++x) {
				bitmap.setPixel(x, y, 0);
			}
		}
		
		
		for (int y = 24; y < 270; ++y) {
			for (int x = 0; x < 208; ++x) {
				boolean print = true;
				for (int k = 0; k < 16 && print; k++) {
					for (int l = 0; l < 16 && print; l++) {
						if (!(bitmap.getPixel(l + x, k + y) == 0 
								|| bitmap.getPixel(l + x, k + y) == 14606046)) {
							print = false;
						}
						if ((k == 0 && l == 0) 
								|| (k == 15 && l == 0)
								|| (k == 0 && l == 15) 
								|| (k == 15 && l == 15)) {
							print = true;
						}
					}
				}
				if (print) {
					Point p = new Point(x, y);
					MAP.add(new Node(p));
				}
			}
		}
		for (int x = 204; x < 224; x++) {
			MAP.add(new Node(new Point(x, 84)));
		}
		for (int x = 204; x < 224; x++) {
			MAP.add(new Node(new Point(x, 156)));
		}
		
		Integer pilln = 0;
		Integer superpilln = 0;
		for(Node node : MAP) {
			if (this.containsPill(node.p.x, node.p.y)) {
				node.setGameEntity(GameEntity.PILL);
				pills.put(pilln, node);
				revpills.put(node, pilln);
				pilln++;
			} else if(this.containsSuperPill(node.p.x, node.p.y)){
				node.setGameEntity(GameEntity.SUPER_PILL);
				superPills.put(superpilln, node);
				revsuperPills.put(node, superpilln);
				superpilln++;
			}
			for(Node neigh : MAP) {
				if (node.p.distance(neigh.p) == 1) {
					node.addNeighbor(neigh);
				}
			}
		}
	}

	public void update(BitMap bitmap) {
		this.bitmap = bitmap;
		ghosts = new Node[4];
		blues.clear();
		for (Node node : MAP) {
			if (!this.isBlack(node.p.x, node.p.y)) {
				if(this.containsPill(node.p.x, node.p.y) && !pills.containsKey(node)) {
					pills.put(revpills.get(node), node);
				} else if (this.containsMsPacman(node.p.x, node.p.y)) {
					mspacman = node;
					pills.remove(revpills.get(node));
					if(!superPills.isEmpty()) {
						for(Node s : superPills.values()) {
							if(mspacman.p.distance(s.p) < 6) {
								superPills.remove(revsuperPills.get(s));
								break;
							}
						}
					}
				} else {
					GameEntity[] ghs = new GameEntity[] { GameEntity.BLINKY,
							GameEntity.PINKY, GameEntity.INKY, GameEntity.SUE };
					for (GameEntity ghost : ghs) {
						if (this.containsGhost(ghost.colour(), node.p.x, node.p.y)) {
							switch (ghost) {
							case BLINKY:
								ghosts[0] = node;
								break;
							case PINKY:
								ghosts[1] = node;
								break;
							case INKY:
								ghosts[2] = node;
								break;
							case SUE:
								ghosts[3] = node;
								break;
							}
						}
					}
					if (this.containsBlueGhost(node.p.x, node.p.y)) {
						boolean add = true;
						for (Node b : blues) {
							if (node.p.x + 1 == b.p.x || node.p.x - 1 == b.p.x) {
								add = false;
								break;
							}
						}
						if (add) {
							blues.add(node);
						}
					}
				}
			} else {
				pills.remove(revpills.get(node));
			}
		}
	}

	public Map<Integer,Node> getPills() {
		return pills;
	}

	public Map<Integer,Node> getSuperPills() {
		return superPills;
	}

	public List<Node> getGhosts() {
		List<Node> gs = new ArrayList<Node>();
		for(Node p : ghosts) {
			gs.add(p);
		}
		return gs;
	}
	
	public List<Node> getBlueGhosts() {
		return blues;
	}
	
	public Node getMsPacman() {
		return mspacman;
	}

	public Node getBlinky() {
		return ghosts[0];
	}

	public Node getPinky() {
		return ghosts[1];
	}

	public Node getInky() {
		return ghosts[2];
	}

	public Node getSue() {
		return ghosts[3];
	}

	
	public List<Node> calculatePath(Node to, Map<Node, Double> weights) {
		return NewAstar.computePath(getMsPacman(), pills.values().iterator().next(), weights);
	}
	
	
	
	private boolean isBlack(int x, int y) {
		return bitmap.getPixel(x + 6, y + 6) == 0
				&& bitmap.getPixel(x + 9, y + 6) == 0
				&& bitmap.getPixel(x + 6, y + 9) == 0
				&& bitmap.getPixel(x + 9, y + 9) == 0
				&& bitmap.getPixel(x + 7, y + 7) == 0
				&& bitmap.getPixel(x + 7, y + 8) == 0
				&& bitmap.getPixel(x + 8, y + 7) == 0
				&& bitmap.getPixel(x + 8, y + 8) == 0;
	}
	
	public boolean containsBlueGhost(int x, int y) {
		return (containsBlueGhost1(x, y) || containsBlueGhost1(x+1, y));
	}
	/*
	 * 2171358 BLUE 16758935 EYE
	 * 16711680 WHITE 16711680 EYE
	 */
	private boolean containsBlueGhost1(int x, int y) {
		return (bitmap.getPixel(x + 5, y + 6) == 16758935
			 && bitmap.getPixel(x + 6, y + 7) == 16758935
			 && bitmap.getPixel(x + 9, y + 6) == 16758935
			 && bitmap.getPixel(x + 10, y + 7) == 16758935
			 && bitmap.getPixel(x + 6, y + 6) == 16758935
		     && bitmap.getPixel(x + 10, y + 6) == 16758935
			 && bitmap.getPixel(x + 4, y + 5) == 2171358
			 && bitmap.getPixel(x + 7, y + 8) == 2171358
			 && bitmap.getPixel(x + 8, y + 5) == 2171358 
			 && bitmap.getPixel(x + 11, y + 8) == 2171358) 
		  || 
		       (bitmap.getPixel(x + 5, y + 6) == 16711680
			 && bitmap.getPixel(x + 6, y + 7) == 16711680
			 && bitmap.getPixel(x + 9, y + 6) == 16711680
			 && bitmap.getPixel(x + 10, y + 7) == 16711680
			 && bitmap.getPixel(x + 6, y + 6) == 16711680
			 && bitmap.getPixel(x + 10, y + 6) == 16711680
			 && bitmap.getPixel(x + 4, y + 5) == 14606046
			 && bitmap.getPixel(x + 7, y + 8) == 14606046
			 && bitmap.getPixel(x + 8, y + 5) == 14606046 
			 && bitmap.getPixel(x + 11, y + 8) == 14606046);
	}

	public boolean containsGhost(int ghost, int x, int y) {
		return (bitmap.getPixel(x + 5, y + 1) != ghost
				&& bitmap.getPixel(x + 6, y + 1) == ghost
				&& bitmap.getPixel(x + 9, y + 1) == ghost
				&& bitmap.getPixel(x + 10, y + 1) != ghost
				&& bitmap.getPixel(x + 1, y + 6) != ghost
				&& bitmap.getPixel(x + 1, y + 7) == ghost
				&& bitmap.getPixel(x + 14, y + 6) != ghost 
				&& bitmap.getPixel(x + 14, y + 7) == ghost);
	}

	public boolean containsMsPacman(int x, int y) {
		return containsMsPacman1(x, y) || containsMsPacman1(x-1, y);
	}

	private boolean containsMsPacman1(int x, int y) {
		return (bitmap.getPixel(x + 10, y + 5) == 16776960
				&& bitmap.getPixel(x + 11, y + 3) == 2171358
				&& bitmap.getPixel(x + 12, y + 4) == 2171358
				&& bitmap.getPixel(x + 11, y + 4) == 16711680 
				&& bitmap.getPixel(x + 12, y + 3) == 16711680)
				|| (bitmap.getPixel(x + 5, y + 5) == 16776960
						&& bitmap.getPixel(x + 4, y + 3) == 2171358
						&& bitmap.getPixel(x + 3, y + 4) == 2171358
						&& bitmap.getPixel(x + 4, y + 4) == 16711680 
						&& bitmap.getPixel(x + 3, y + 3) == 16711680)
				|| (bitmap.getPixel(x + 5, y + 10) == 16776960
						&& bitmap.getPixel(x + 4, y + 12) == 2171358
						&& bitmap.getPixel(x + 3, y + 11) == 2171358
						&& bitmap.getPixel(x + 4, y + 11) == 16711680 
						&& bitmap.getPixel(x + 3, y + 12) == 16711680);
	}

	private boolean containsPill(int x, int y) {
		return bitmap.getPixel(x + 6, y + 6) == 0
				&& bitmap.getPixel(x + 9, y + 6) == 0
				&& bitmap.getPixel(x + 6, y + 9) == 0
				&& bitmap.getPixel(x + 9, y + 9) == 0
				&& bitmap.getPixel(x + 7, y + 7) == 14606046
				&& bitmap.getPixel(x + 7, y + 8) == 14606046
				&& bitmap.getPixel(x + 8, y + 7) == 14606046
				&& bitmap.getPixel(x + 8, y + 8) == 14606046;
	}

	private boolean containsSuperPill(int x, int y) {
		return bitmap.getPixel(x + 5, y + 5) == 14606046
				&& bitmap.getPixel(x + 10, y + 5) == 14606046
				&& bitmap.getPixel(x + 5, y + 10) == 14606046
				&& bitmap.getPixel(x + 10, y + 10) == 14606046;
	}
}
