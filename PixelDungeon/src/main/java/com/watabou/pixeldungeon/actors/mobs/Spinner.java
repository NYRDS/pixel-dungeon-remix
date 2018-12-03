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
package com.watabou.pixeldungeon.actors.mobs;

import com.nyrds.pixeldungeon.ai.ThiefFleeing;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.blobs.Blob;
import com.watabou.pixeldungeon.actors.blobs.Web;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.Poison;
import com.watabou.pixeldungeon.actors.buffs.Roots;
import com.watabou.pixeldungeon.actors.buffs.Terror;
import com.watabou.pixeldungeon.items.food.MysteryMeat;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

import androidx.annotation.NonNull;

public class Spinner extends Mob {
	
	public Spinner() {
		hp(ht(50));
		defenseSkill = 14;
		
		exp = 9;
		maxLvl = 16;
		
		loot = new MysteryMeat();
		lootChance = 0.125f;
		
		FLEEING = new ThiefFleeing(this);
		
		RESISTANCES.add( Poison.class );
		IMMUNITIES.add( Roots.class );
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 12, 16 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 20;
	}
	
	@Override
	public int dr() {
		return 6;
	}
	
	@Override
	protected boolean act() {
		boolean result = super.act();
		
		if ((getState() == FLEEING) && !hasBuff(Terror.class) && enemySeen && !getEnemy().hasBuff(Poison.class)) {
			setState(HUNTING);
		}
		return result;
	}
	
	@Override
	public int attackProc(@NonNull Char enemy, int damage ) {
		if (Random.Int( 2 ) == 0) {
			Buff.affect( enemy, Poison.class ).set( Random.Int( 7, 9 ) * Poison.durationFactor( enemy ) );
			setState(FLEEING);
		}
		
		return damage;
	}
	
	@Override
	public void move( int step ) {
		if (getState() == FLEEING) {
			GameScene.add( Blob.seed( getPos(), Random.Int( 5, 7 ), Web.class ) );
		}
		super.move( step );
	}

}
