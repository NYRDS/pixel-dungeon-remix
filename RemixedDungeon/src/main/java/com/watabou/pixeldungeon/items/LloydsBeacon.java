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
package com.watabou.pixeldungeon.items;

import com.nyrds.Packable;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.utils.DungeonGenerator;
import com.nyrds.pixeldungeon.utils.Position;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.sprites.Glowing;
import com.watabou.pixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class LloydsBeacon extends Item {

	private static final float TIME_TO_USE = 1;

	private static final String AC_SET    = "LloidsBeacon_ACSet";
	private static final String AC_RETURN = "LloidsBeacon_ACReturn";

	@NotNull
	@Packable
	private Position returnTo = new Position();
	
	public LloydsBeacon() {
		image = ItemSpriteSheet.BEACON;
		returnTo.cellId = -1;
		
		name = Game.getVar(R.string.LloidsBeacon_Name);
	}

	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_SET );
		if (returnTo.cellId != -1) {
			actions.add( AC_RETURN );
		}
		return actions;
	}
	
	@Override
	public void _execute(@NotNull Char chr, @NotNull String action ) {
		
		if (action.equals(AC_SET) || action.equals(AC_RETURN)) {
			
			if (Dungeon.bossLevel()) {
				chr.spend( LloydsBeacon.TIME_TO_USE );
				GLog.w( Game.getVar(R.string.LloidsBeacon_Preventing) );
				return;
			}
			
			for (int i=0; i < Level.NEIGHBOURS8.length; i++) {
				if (Actor.findChar( chr.getPos() + Level.NEIGHBOURS8[i] ) != null) {
					GLog.w( Game.getVar(R.string.LloidsBeacon_Creatures) );
					return;
				}
			}
		}
		
		if (action.equals(AC_SET)) {
			
			returnTo = Dungeon.currentPosition();
			
			chr.spend( LloydsBeacon.TIME_TO_USE );

			chr.getSprite().operate( chr.getPos());
			Sample.INSTANCE.play( Assets.SND_BEACON );
			
			GLog.i( Game.getVar(R.string.LloidsBeacon_Return) );
			
		} else if (action.equals(AC_RETURN)) {
			Position target = new Position(returnTo);
			reset();
			chr.teleportTo(target);
		} else {
			super._execute(chr, action );
		}
	}
	
	public void reset() {
		returnTo.cellId = -1;
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	private static final Glowing WHITE = new Glowing( 0xFFFFFF );
	
	@Override
	public Glowing glowing() {
		return returnTo.cellId != -1 ? WHITE : null;
	}
	
	@Override
	public String info() {
		return Game.getVar(R.string.LloidsBeacon_Info) + (returnTo.cellId == -1 ? Utils.EMPTY_STRING : Utils.format( Game.getVar(R.string.LloidsBeacon_Set), DungeonGenerator.getLevelDepth(returnTo.levelId) ) );
	}
}
