package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jef.video.BitMap;

public class GameMap {
	private BitMap bitmap;
	private List<Point> MAP = new ArrayList<Point>();
	private List<Point> pills = new ArrayList<Point>();
	private List<Point> superPills = new ArrayList<Point>();
	private Point mspacman = new Point();
	private Point[] ghosts = new Point[4];
	private List<Point> blues = new ArrayList<Point>();

	private AStar astar;

	public GameMap(BitMap bitmap) {
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
					MAP.add(p);
					if (containsPill(x, y)) {
						pills.add(p);
					}
					if (containsSuperPill(x, y)) {
						superPills.add(p);
					}
				}
			}
		}
		for (int x = 204; x < 224; x++) {
			MAP.add(new Point(x, 156));
		}
		for (int x = 204; x < 224; x++) {
			MAP.add(new Point(x, 84));
		}
		astar = new AStar(new HashSet<Point>(MAP));
	}

	public void update(BitMap bitmap) {
		this.bitmap = bitmap;
		ghosts = new Point[4];
		blues.clear();
		
		for (Point p : MAP) {
			if (!this.isBlack(p.x, p.y)) {
				if(this.containsPill(p.x, p.y) && !pills.contains(p)) {
					pills.add(p);
				} else if (this.containsSuperPill(p.x, p.y) && !superPills.contains(p)) {
					superPills.add(p);
				} else if (this.containsMsPacman(p.x, p.y)) {
					mspacman = p;
					pills.remove(p);
					superPills.remove(p);
				} else {
					GameEntity[] ghs = new GameEntity[] { GameEntity.BLINKY,
							GameEntity.PINKY, GameEntity.INKY, GameEntity.SUE };
					for (GameEntity ghost : ghs) {
						if (this.containsGhost(ghost.colour(), p.x, p.y)) {
							switch (ghost) {
							case BLINKY:
								ghosts[0] = p;
								break;
							case PINKY:
								ghosts[1] = p;
								break;
							case INKY:
								ghosts[2] = p;
								break;
							case SUE:
								ghosts[3] = p;
								break;
							}
						}
					}
					if (this.containsBlueGhost(p.x, p.y)) {
						boolean add = true;
						for (Point b : blues) {
							if (p.x + 1 == b.x || p.x - 1 == b.x) {
								add = false;
								break;
							}
						}
						if (add) {
							blues.add(p);
						}
					}
				}
			} else {
				pills.remove(p);
			}
		}
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

	public List<Point> getMap() {
		return MAP;
	}

	public List<Point> getPills() {
		return pills;
	}

	public List<Point> getSuperPills() {
		return superPills;
	}

	public Point[] getGhosts() {
		return ghosts;
	}
	
	public List<Point> getBlueGhosts() {
		return blues;
	}
	
	public Point getMsPacman() {
		return mspacman;
	}

	public Point getBlinky() {
		return ghosts[0];
	}

	public Point getPinky() {
		return ghosts[1];
	}

	public Point getInky() {
		return ghosts[2];
	}

	public Point getSue() {
		return ghosts[3];
	}

	public List<Point> calculatePath(Point to, Set<Point> closed) {
		return astar.computePath(getMsPacman(), to, closed);
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
