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

import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.Light;
import com.watabou.pixeldungeon.effects.particles.FlameParticle;
import com.watabou.pixeldungeon.sprites.ItemSpriteSheet;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Torch extends Item {

	public static final String AC_LIGHT	= "Torch_ACLight";

	public static final float TIME_TO_LIGHT = 1;
	
	{
		name = Game.getVar(R.string.Torch_Name);
		image = ItemSpriteSheet.TORCH;
		
		stackable = true;

		info = Game.getVar(R.string.Torch_Info2);

		setDefaultAction(AC_LIGHT);
	}
	
	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LIGHT );
		return actions;
	}
	
	@Override
	public void _execute(@NotNull Char chr, @NotNull String action ) {
		
		if (action.equals(AC_LIGHT)) {
			
			chr.spend( TIME_TO_LIGHT );

			chr.getSprite().operate( chr.getPos());
			
			detach( chr.getBelongings().backpack );
			Buff.affect(chr, Light.class, Light.DURATION );
			
			Emitter emitter = chr.getSprite().centerEmitter();
			emitter.start( FlameParticle.FACTORY, 0.2f, 3 );
			
		} else {
			
			super._execute(chr, action );
			
		}
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int price() {
		return 10 * quantity();
	}
}
