package com.nyrds.pixeldungeon.levels;

import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.levels.TerrainFlags;
import com.watabou.pixeldungeon.scenes.GameScene;

/**
 * Created by mike on 27.02.2016.
 */
public class Tools {

	public static void buildShadowLordMaze(Level level, final int roomStep) {
		int w = level.getWidth();
		int h = level.getHeight();

		if(level.cellValid(level.entrance)) {
			level.set(level.entrance, Terrain.EMPTY_DECO);
			level.set(level.getExit(0), Terrain.EMPTY_DECO);
		}
		level.entrance = -1;
		level.setExit(-1,0);

		int im = (int) (Math.floor((float)(w)/roomStep)*roomStep+2);
		int jm = (int) (Math.floor((float)(h)/roomStep)*roomStep+2);

		for (int i = 0; i <im; i++) {
			for (int j = 0; j <jm; j++) {
				if (i == 0 || j == 0 || i == im-1 || j == jm-1) {
					level.set(i, j, Terrain.WALL_DECO);
					continue;
				}

				if ((i - 1) % roomStep == 0 && (j - 1) % roomStep == 0) {
					level.set(i, j, Terrain.WALL_DECO);
					continue;
				}

				if ((i - 1) % roomStep == roomStep/2 && (j - 1) % roomStep == roomStep/2 ) {

					if(level.get(i, j)==Terrain.EMPTY) {
						level.set(i, j, Terrain.PEDESTAL);
					}
					continue;
				}

				if ((i - 1) % roomStep == 0) {

					if (TerrainFlags.is(level.get(i - 1, j), TerrainFlags.SOLID) || TerrainFlags.is(level.get(i + 1, j), TerrainFlags.SOLID)) {
						setCellIfEmpty(level, i, j, Terrain.WALL_DECO);
						continue;
					}

					setCellIfEmpty(level, i, j, Terrain.WALL);
					continue;
				}

				if((j-1)%roomStep==0) {
					if (TerrainFlags.is(level.get(i, j-1), TerrainFlags.SOLID) ||
							TerrainFlags.is(level.get(i, j + 1), TerrainFlags.SOLID)) {
						setCellIfEmpty(level, i, j, Terrain.WALL_DECO);
						continue;
					}

					setCellIfEmpty(level, i, j, Terrain.WALL);
					continue;
				}
			}
		}

		int[] doorPositions = {144, 87, 150, 93, 156, 264, 207, 270, 213, 276, 327, 333};
		for (int i = 0; i < doorPositions.length; i++) {
			level.set(doorPositions[i], Terrain.DOOR);
		}

		GameScene.updateMap();
	}

	private static void setCellIfEmpty(Level level, int x, int y, int terrain) {
		int cell = level.cell(x, y);
		if (Actor.findChar(cell) == null) {
			level.set(cell, terrain);
		}
	}

	public static void makeEmptyLevel(Level level) {
		int width = level.getWidth();
		int height = level.getHeight();

		for (int i = 1; i < width; i++) {
			for (int j = 1; j < height; j++) {
				level.set(i, j, Terrain.EMPTY);
			}
		}

		for (int i = 1; i < width; i++) {
			level.set(i, 1,        Terrain.WALL);
			level.set(i, height-1, Terrain.WALL);
		}

		for (int j = 1; j < height; j++) {
			level.set(1, j,        Terrain.WALL);
			level.set(width-1, j , Terrain.WALL);
		}

		level.entrance = level.cell(width/4,height/4);
		level.set(level.entrance, Terrain.ENTRANCE);

		level.setExit(level.cell(width-width/4,height-height/4),0);
		level.set(level.getExit(0), Terrain.EXIT);

		if(GameScene.isSceneReady()) {
			GameScene.updateMap();
		}
	}

	public static void makeShadowLordLevel(Level level) {
		int width = level.getWidth();
		int height = level.getHeight();

		for (int i = 1; i < width; i++) {
			for (int j = 1; j < height; j++) {
				level.set(i, j, Terrain.EMPTY);
			}
		}

		for (int i = 1; i < width; i++) {
			level.set(i, 1,        Terrain.WALL);
			level.set(i, height-1, Terrain.WALL);
		}

		for (int j = 1; j < height; j++) {
			level.set(1, j,        Terrain.WALL);
			level.set(width-1, j , Terrain.WALL);
		}

		for (int i = width/4; i < width/2 + width/4; i++) {
			level.set(i, height/4,        Terrain.WALL);
			level.set(i, height/2 + height/4 - 1, Terrain.WALL);
		}

		for (int j = height/4; j < height/2 + height/4; j++) {
			level.set(width/4, j,        Terrain.WALL);
			level.set(width/2 + width/4 - 1, j , Terrain.WALL);
		}

		level.entrance = level.cell(width/4 + 1,height/4 + 1);
		level.set(level.entrance, Terrain.ENTRANCE);

		level.setExit(level.cell(width-width/4 + 1,height-height/4 + 1),0);
		level.set(level.getExit(0), Terrain.EXIT);

		if(GameScene.isSceneReady()) {
			GameScene.updateMap();
		}
	}
}