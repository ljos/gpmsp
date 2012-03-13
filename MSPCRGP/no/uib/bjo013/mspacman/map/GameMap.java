package no.uib.bjo013.mspacman.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import jef.video.BitMap;

public class GameMap {
	private BitMap bitmap;
	private LinkedHashSet<Point> MAP = new LinkedHashSet<Point>();
	private LinkedHashSet<Point> pills = new LinkedHashSet<Point>();
	private LinkedHashSet<Point> superPills = new LinkedHashSet<Point>();

	private PathFinder pf;

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
		pf = new PathFinder(MAP);
	}

	public void update(BitMap bitmap) {
		this.bitmap = bitmap;
		try {
			Point p = this.getMsPacman().iterator().next();
		pills.remove(p);
		pills.remove(new Point(p.x + 1, p.y));
		pills.remove(new Point(p.x - 1, p.y));
		pills.remove(new Point(p.x, p.y + 1));
		pills.remove(new Point(p.x, p.y - 1));
		superPills.remove(p);
		superPills.remove(new Point(p.x + 1, p.y));
		superPills.remove(new Point(p.x - 1, p.y));
		superPills.remove(new Point(p.x, p.y + 1));
		superPills.remove(new Point(p.x, p.y - 1));
		} catch (NoSuchElementException e) {}
	}

	public LinkedHashSet<Point> getMap() {
		return MAP;
	}

	public LinkedHashSet<Point> getPills() {
		return pills;
	}

	public LinkedHashSet<Point> getSuperPills() {
		return superPills;
	}

	public LinkedHashSet<Point> getGhosts() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		map.addAll(this.getBlinky());
		map.addAll(this.getPinky());
		map.addAll(this.getInky());
		map.addAll(this.getSue());
		return map;
	}

	public LinkedHashSet<Point> getBlueGhosts() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		boolean blue = false;
		for (Point p : MAP) {
			if (!blue 
					&& this.getBlinky().isEmpty() 
					&& this.getPinky().isEmpty()
					&& this.getInky().isEmpty() 
					&& this.getSue().isEmpty()) {
				break;
			} else if (containsBlueGhost(p.x, p.y)) {
				map.add(p);
				blue = true;
			}
		}
		return map;
	}

	private Point mspacman = new Point();
	public LinkedHashSet<Point> getMsPacman() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		if (containsMsPacman(mspacman.x, mspacman.y)) {
			map.add(mspacman);
		} else if (containsMsPacman(mspacman.x + 1, mspacman.y)) {
			map.add(new Point(mspacman.x + 1, mspacman.y));
		} else if (containsMsPacman(mspacman.x - 1, mspacman.y)) {
			map.add(new Point(mspacman.x - 1, mspacman.y));
		} else if (containsMsPacman(mspacman.x, mspacman.y + 1)) {
			map.add(new Point(mspacman.x, mspacman.y + 1));
		} else if (containsMsPacman(mspacman.x, mspacman.y - 1)) {
			map.add(new Point(mspacman.x, mspacman.y - 1));
		} else {
			for (Point p : MAP) {
				if (containsMsPacman(p.x, p.y)) {
					map.add(p);
					mspacman = p;
					return map;
				}
			}
		}
		if(map.isEmpty()) {
			map.add(mspacman);
		}
		return map;
	}

	private Point blinky = new Point();
	public LinkedHashSet<Point> getBlinky() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		if (containsGhost(GameEntity.BLINKY.colour(), blinky.x, blinky.y)) {
			map.add(blinky);
		} else if (containsGhost(GameEntity.BLINKY.colour(), blinky.x+1, blinky.y)) {
			map.add(new Point(blinky.x+1, blinky.y));
		} else if (containsGhost(GameEntity.BLINKY.colour(), blinky.x-1, blinky.y)) {
			map.add(new Point(blinky.x-1, blinky.y));
		} else if (containsGhost(GameEntity.BLINKY.colour(), blinky.x, blinky.y+1)) {
			map.add(new Point(blinky.x, blinky.y+1));
		} else if (containsGhost(GameEntity.BLINKY.colour(), blinky.x, blinky.y-1)) {
			map.add(new Point(blinky.x, blinky.y-1));
		} else {
			for (Point p : MAP) {
				if (containsGhost(GameEntity.BLINKY.colour(), p.x, p.y)) {
					map.add(p);
					return map;
				}
			}
		}
		if(map.isEmpty()) {
			map.add(blinky);
		}
		return map;
	}

	private Point pinky = new Point();
	public LinkedHashSet<Point> getPinky() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		if (containsGhost(GameEntity.PINKY.colour(), pinky.x, pinky.y)) {
			map.add(pinky);
		} else if (containsGhost(GameEntity.PINKY.colour(), pinky.x+1, pinky.y)) {
			map.add(new Point(pinky.x+1, pinky.y));
		} else if (containsGhost(GameEntity.PINKY.colour(), pinky.x-1, pinky.y)) {
			map.add(new Point(pinky.x-1, pinky.y));
		} else if (containsGhost(GameEntity.PINKY.colour(), pinky.x, pinky.y+1)) {
			map.add(new Point(pinky.x, pinky.y+1));
		} else if (containsGhost(GameEntity.PINKY.colour(), pinky.x, pinky.y-1)) {
			map.add(new Point(pinky.x, pinky.y-1));
		} else {
			for (Point p : MAP) {
				if (containsGhost(GameEntity.PINKY.colour(), p.x, p.y)) {
					map.add(p);
					return map;
				}
			}
		}
		return map;
	}

	private Point inky = new Point();
	public LinkedHashSet<Point> getInky() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		if (containsGhost(GameEntity.INKY.colour(), inky.x, inky.y)) {
			map.add(inky);
		} else if (containsGhost(GameEntity.INKY.colour(), inky.x+1, inky.y)) {
			map.add(new Point(inky.x+1, inky.y));
		} else if (containsGhost(GameEntity.INKY.colour(), inky.x-1, inky.y)) {
			map.add(new Point(inky.x-1, inky.y));
		} else if (containsGhost(GameEntity.INKY.colour(), inky.x, inky.y+1)) {
			map.add(new Point(inky.x, inky.y+1));
		} else if (containsGhost(GameEntity.INKY.colour(), inky.x, inky.y-1)) {
			map.add(new Point(inky.x, inky.y-1));
		} else {
			for (Point p : MAP) {
				if (containsGhost(GameEntity.INKY.colour(), p.x, p.y)) {
					map.add(p);
					return map;
				}
			}
		}
		return map;
	}

	private Point sue = new Point();
	public LinkedHashSet<Point> getSue() {
		LinkedHashSet<Point> map = new LinkedHashSet<Point>();
		if (containsGhost(GameEntity.SUE.colour(), sue.x, sue.y)) {
			map.add(sue);
		} else if (containsGhost(GameEntity.SUE.colour(), sue.x+1, sue.y)) {
			map.add(new Point(sue.x+1, sue.y));
		} else if (containsGhost(GameEntity.SUE.colour(), sue.x-1, sue.y)) {
			map.add(new Point(sue.x-1, sue.y));
		} else if (containsGhost(GameEntity.SUE.colour(), sue.x, sue.y+1)) {
			map.add(new Point(sue.x, sue.y+1));
		} else if (containsGhost(GameEntity.SUE.colour(), sue.x, sue.y-1)) {
			map.add(new Point(sue.x, sue.y-1));
		} else {
			for (Point p : MAP) {
				if (containsGhost(GameEntity.SUE.colour(), p.x, p.y)) {
					map.add(p);
					return map;
				}
			}
		}
		return map;
	}

	public List<Point> calculatePath(Point to) {
		PathFinder.setTarget(to);
		List<Point> ret = null;
		try {
			ret = pf.compute(getMsPacman().iterator().next());
		} catch (NoSuchElementException e) {}
		return ret == null ? new ArrayList<Point>() : ret;
	}
	public boolean containsBlueGhost(int x, int y) {
		return containsBlueGhost1(x, y)
				|| containsBlueGhost1(x + 1, y);
	}
	private boolean containsBlueGhost1(int x, int y) {
		return ((bitmap.getPixel(x + 5, y + 6) == 16758935
				&& bitmap.getPixel(x + 6, y + 7) == 16758935
				&& bitmap.getPixel(x + 9, y + 6) == 16758935
				&& bitmap.getPixel(x + 10, y + 7) == 16758935
				&& bitmap.getPixel(x + 6, y + 6) == 16758935
				&& bitmap.getPixel(x + 10, y + 6) == 16758935
				&& bitmap.getPixel(x + 4, y + 5) == 2171358
				&& bitmap.getPixel(x + 7, y + 8) == 2171358
				&& bitmap.getPixel(x + 8, y + 5) == 2171358 
				&& bitmap.getPixel(x + 11, y + 8) == 2171358) 
				|| (bitmap.getPixel(x + 5, y + 6) == 16711680
				&& bitmap.getPixel(x + 6, y + 7) == 16711680
				&& bitmap.getPixel(x + 9, y + 6) == 16711680
				&& bitmap.getPixel(x + 10, y + 7) == 16711680
				&& bitmap.getPixel(x + 6, y + 6) == 16711680
				&& bitmap.getPixel(x + 10, y + 6) == 16711680
				&& bitmap.getPixel(x + 4, y + 5) == 14606046
				&& bitmap.getPixel(x + 7, y + 8) == 14606046
				&& bitmap.getPixel(x + 8, y + 5) == 14606046 
				&& bitmap.getPixel(x + 11, y + 8) == 14606046));
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
		return containsMsPacman1(x-1, y) || containsMsPacman1(x, y);
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
