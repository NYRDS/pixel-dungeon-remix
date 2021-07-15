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
package com.watabou.pixeldungeon.items.quest;

import com.nyrds.Packable;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Hunger;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.actors.mobs.Bat;
import com.watabou.pixeldungeon.effects.CellEmitter;
import com.watabou.pixeldungeon.effects.Speck;
import com.watabou.pixeldungeon.items.weapon.Weapon;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.sprites.Glowing;
import com.watabou.pixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.pixeldungeon.ui.BuffIndicator;
import com.watabou.pixeldungeon.ui.QuickSlot;
import com.watabou.pixeldungeon.utils.GLog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Pickaxe extends Weapon {

	public static final String AC_MINE	= "Pickaxe_ACMine";

	public static final float TIME_TO_MINE = 2;
	
	private static final Glowing BLOODY = new Glowing( 0x550000 );
	
	{
		image = ItemSpriteSheet.PICKAXE;

		setDefaultAction(AC_MINE);
		
		STR = 14;
		MIN = 3;
		MAX = 12;
		animation_class = SWORD_ATTACK;
	}

	@Packable
	public boolean bloodStained = false;
	
	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_MINE );
		return actions;
	}
	
	@Override
	public void _execute(@NotNull final Char chr, @NotNull String action ) {
		
		if (action.equals(AC_MINE)) {

			Level level = chr.level();

			/*
			if (!(level instanceof CavesLevel || level instanceof CavesBossLevel) ) {
				GLog.w( Game.getVar(R.string.Pickaxe_NoVein) );
				return;
			}
			*/
			for (int i=0; i < Level.NEIGHBOURS8.length; i++) {
				
				final int pos = chr.getPos() + Level.NEIGHBOURS8[i];
				if (level.map[pos] == Terrain.WALL_DECO) {
				
					chr.spend( TIME_TO_MINE );

					CellEmitter.center( pos ).burst( Speck.factory( Speck.STAR ), 7 );
					Sample.INSTANCE.play( Assets.SND_EVOKE );

					level.set( pos, Terrain.WALL );
					GameScene.updateMap( pos );

					DarkGold gold = new DarkGold();
					if (gold.doPickUp( chr )) {
						GLog.i( Hero.getHeroYouNowHave(), gold.name() );
					} else {
						gold.doDrop(chr);
					}

					chr.hunger().satisfy( -Hunger.STARVING / 10 );
					BuffIndicator.refreshHero();


					chr.getSprite().dummyAttack(pos);
					
					return;
				}
			}
			
			GLog.w( Game.getVar(R.string.Pickaxe_NoVein) );
			
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
	public void attackProc(Char attacker, Char defender, int damage ) {
		if (!bloodStained && defender instanceof Bat && (defender.hp() <= damage)) {
			bloodStained = true;
            QuickSlot.refresh(attacker);
        }
	}

	@Override
	public Glowing glowing() {
		return bloodStained ? BLOODY : null;
	}
	
	@Override
	public String info() {
		return Game.getVar(R.string.Pickaxe_Info);
	}
}
