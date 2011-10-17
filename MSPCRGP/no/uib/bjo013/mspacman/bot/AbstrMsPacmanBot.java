package no.uib.bjo013.mspacman.bot;

import no.uib.bjo013.mspacman.MsPacman;

public abstract class AbstrMsPacmanBot implements Runnable {

	protected MsPacman game;
	protected Thread gamethread;

	private int tries;
	private String code;
	private long score;

	public AbstrMsPacmanBot(String code, int tries) {
		this.code = code;
		this.tries = tries;
	}

	protected void logic() {
		gamethread = new Thread(game);
		gamethread.start();
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		try {
			while (gamethread.isAlive()) {
	/*			IFn fn = (IFn) clojure.lang.Compiler.load(new StringReader(code));
				fn.invoke(game); */
				
				//BOT LOGIC
				
				if (game.isGameOver()) {
					tries--;
					if (tries == 0) {
						this.stop(true);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.stop(true);
		}
	}

	public long getScore() {
		return this.score;
	}

	public void stop(boolean stop) {
		score = game.getScore();
		game.stop(true);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Fitness: " + this.getScore() + "\n" + "Code: " + this.code;
	}
}
