/*
 * Pixel Dungeon
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.pixeldungeon.scenes;

import com.nyrds.android.util.GuiProperties;
import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.support.Ads;
import com.nyrds.pixeldungeon.utils.DungeonGenerator;
import com.nyrds.pixeldungeon.utils.Position;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.InterstitialPoint;
import com.watabou.noosa.Text;
import com.watabou.noosa.audio.Music;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.Statistics;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.items.Generator;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.windows.WndError;
import com.watabou.pixeldungeon.windows.WndStory;

import java.io.FileNotFoundException;
import java.io.IOException;

public class InterlevelScene extends PixelScene {

	private static final float TIME_TO_FADE = 0.3f;

	public enum Mode {
		DESCEND, ASCEND, CONTINUE, RESURRECT, RETURN, FALL
	}

	public static Mode mode;

	public static Position returnTo;

	public static boolean noStory = false;

	public static boolean fallIntoPit;

	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}

	private Phase phase;
	private float timeLeft;

	private Text message;

	private LevelChanger levelChanger;

	volatile private String error = null;

	class LevelChanger implements InterstitialPoint, Runnable {
		private volatile boolean ready;

		@Override
		public void returnToWork(boolean result) {
			ready = true;
		}
		
		@Override
		public void run() {
			try {
				Generator.reset();
					switch (mode) {
					case DESCEND:
						descend();
						break;
					case ASCEND:
						ascend();
						break;
					case CONTINUE:
						restore();
						break;
					case RESURRECT:
						resurrect();
						break;
					case RETURN:
						returnTo();
						break;
					case FALL:
						fall();
						break;
					}

			} catch (FileNotFoundException e) {
				error = Game.getVar(R.string.InterLevelScene_FileNotFound);
			} catch (IOException e) {
				EventCollector.logException(e);
				error = Game.getVar(R.string.InterLevelScene_ErrorGeneric) + "\n" + e.getMessage();
			}

			if(mode != Mode.CONTINUE) {
				Ads.displayEasyModeSmallScreenAd(this);
			} else {
				returnToWork(true);
			}
		}
	}

	static public void Do(InterlevelScene.Mode mode) {
		InterlevelScene.mode = mode;

		switch (mode) {
			case DESCEND:
			case ASCEND:
			case RETURN:
				if(Dungeon.hero != null) {
					Dungeon.save();
				}
				break;
			case FALL:
				Dungeon.saveCurrentLevel();
				break;
			default:

		}

		Game.switchScene( InterlevelScene.class );
	}

	@Override
	public void create() {
		super.create();

		String text = "";
		switch (mode) {
		case DESCEND:
			text = Game.getVar(R.string.InterLevelScene_Descending);
			break;
		case ASCEND:
			text = Game.getVar(R.string.InterLevelScene_Ascending);
			break;
		case CONTINUE:
			text = Game.getVar(R.string.InterLevelScene_Loading);
			break;
		case RESURRECT:
			text = Game.getVar(R.string.InterLevelScene_Resurrecting);
			break;
		case RETURN:
			text = Game.getVar(R.string.InterLevelScene_Returning);
			break;
		case FALL:
			text = Game.getVar(R.string.InterLevelScene_Falling);
			break;
		}

		message = PixelScene.createText(text, GuiProperties.titleFontSize());
		message.x = (Camera.main.width - message.width()) / 2;
		message.y = (Camera.main.height - message.height()) / 2;
		add(message);

		phase = Phase.FADE_IN;
		timeLeft = TIME_TO_FADE;

		levelChanger = new LevelChanger();
		Game.instance().executor.execute(levelChanger);
	}

	@Override
	public void update() {
		super.update();

		float p = timeLeft / TIME_TO_FADE;

		if (error != null) {
			add(new WndError(error) {
				public void onBackPressed() {
					super.onBackPressed();
					Game.switchScene(StartScene.class);
				}
			});
			error = null;
		}

		switch (phase) {

		case FADE_IN:
			message.alpha(1 - p);
			if ((timeLeft -= Game.elapsed) <= 0) {
				if (error == null && levelChanger.ready) {
					phase = Phase.FADE_OUT;
					timeLeft = TIME_TO_FADE;
				} else {
					phase = Phase.STATIC;
				}
			}
			break;

		case FADE_OUT:
			message.alpha(p);

			if (mode == Mode.CONTINUE
					|| (mode == Mode.DESCEND && Dungeon.depth == 1)) {
				Music.INSTANCE.volume(p);
			}
			if ((timeLeft -= Game.elapsed) <= 0) {
				Game.switchScene(GameScene.class);
			}
			break;

		case STATIC:
			if(levelChanger.ready) {
				phase = Phase.FADE_OUT;
			}
			break;
		}
	}

	private void descend() throws IOException {
		Actor.fixTime();
		
		if (Dungeon.hero == null) {
			Dungeon.init();
			if (noStory) {
				Dungeon.chapters.add(WndStory.ID_SEWERS);
				noStory = false;
			}
		} else {
			Dungeon.level.removePets();
		}

		Position next = DungeonGenerator.descend(Dungeon.currentPosition());
		Dungeon.depth = DungeonGenerator.getLevelDepth(next.levelId);
		Level level = Dungeon.loadLevel(next);

		Dungeon.switchLevel(level, level.entrance, next.levelId);
		
		Dungeon.hero.spawnPets(level, true);
	}

	private void fall() throws IOException {

		Actor.fixTime();

		Dungeon.level.removePets();

		Position next = DungeonGenerator.descend(Dungeon.currentPosition());
		Dungeon.depth = DungeonGenerator.getLevelDepth(next.levelId);
		Level level = Dungeon.loadLevel(next);
		
		Dungeon.switchLevel(level,
				fallIntoPit ? level.pitCell() : level.randomRespawnCell(), next.levelId);
	}

	private void ascend() throws IOException {
		Actor.fixTime();
		
		Position next = DungeonGenerator.ascend(Dungeon.currentPosition());
		
		Dungeon.level.removePets();

		Dungeon.depth=DungeonGenerator.getLevelDepth(next.levelId);
		
		Level level = Dungeon.loadLevel(next);

		int exitIndex = -(next.cellId + 1);

		Dungeon.switchLevel(level, level.getExit(exitIndex), next.levelId);

		Dungeon.hero.spawnPets(level, true);
	}

	private void returnTo() throws IOException {

		Actor.fixTime();

		Dungeon.level.removePets();

		Dungeon.depth = DungeonGenerator.getLevelDepth(returnTo.levelId);
		
		Level level = Dungeon.loadLevel(returnTo);

		returnTo.computeCell(level);

		Dungeon.switchLevel(level, returnTo.cellId, returnTo.levelId);
		Dungeon.hero.spawnPets(level, true);
	}

	private void problemWithSave() {
		Dungeon.deleteGame(true);
		throw new TrackedRuntimeException("something wrong with save");
	}

	private void restore() throws IOException{

		Actor.fixTime();

		Dungeon.loadGame();

		if (Dungeon.hero == null) {
			problemWithSave();
		}

		if (Dungeon.depth == -1) {
			Dungeon.depth = Statistics.deepestFloor;
			Dungeon.switchLevel(Dungeon.loadLevel(Dungeon.currentPosition()), -1, Dungeon.hero.levelId);

		} else {
			Level level = Dungeon.loadLevel(Dungeon.currentPosition());
			if (level == null) { // save file fucked up :(
				problemWithSave();
				return;
			}
			Dungeon.switchLevel(level, Dungeon.hero.getPos(), Dungeon.hero.levelId);

			Dungeon.hero.spawnPets(Dungeon.level, false);
		}
	}

	private void resurrect() {

		Actor.fixTime();

		Dungeon.level.removePets();

		if (Dungeon.bossLevel()) {
			Dungeon.hero.resurrect(Dungeon.depth);
			Level level = Dungeon.newLevel(Dungeon.currentPosition());
			Dungeon.switchLevel(level, level.entrance, Dungeon.hero.levelId);
		} else {
			Dungeon.hero.resurrect(-1);
			Dungeon.resetLevel();
		}

		Dungeon.hero.spawnPets(Dungeon.level, true);
	}


	@Override
	protected void onBackPressed() {
	}
}
