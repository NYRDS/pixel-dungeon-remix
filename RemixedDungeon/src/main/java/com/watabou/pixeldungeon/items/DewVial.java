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
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.effects.particles.ShaftParticle;
import com.watabou.pixeldungeon.sprites.CharSprite;
import com.watabou.pixeldungeon.sprites.Glowing;
import com.watabou.pixeldungeon.ui.QuickSlot;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DewVial extends Item {

	private static final int MAX_VOLUME	= 10;
	
	private static final String AC_DRINK	= "DewVial_ACDRINK";
	
	private static final float TIME_TO_DRINK = 1f;
	
	private static final String TXT_VALUE	= "%+dHP";
	private static final String TXT_STATUS	= "%d/%d";
	
	{
		imageFile = "items/vials.png";
		image = 0;
		
		setDefaultAction(AC_DRINK);
	}

	@Packable
	private int volume = 0;

	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (getVolume() > 0) {
			actions.add( AC_DRINK );
		}
		return actions;
	}
	
	private static final double NUM = 20;
	private static final double POW = Math.log10( NUM );
	
	@Override
	public void _execute(@NotNull final Char chr, @NotNull String action ) {
		if (action.equals( AC_DRINK )) {
			
			if (getVolume() > 0) {

				int value = (int)Math.ceil( Math.pow( getVolume(), POW ) / NUM * chr.ht() );
				int effect = Math.min( chr.ht() - chr.hp(), value );
				if (effect > 0) {
					chr.heal(effect,this);
					chr.getSprite().showStatus( CharSprite.POSITIVE, TXT_VALUE, effect );
				}
				
				setVolume(0);
				
				chr.spend( TIME_TO_DRINK );

				Sample.INSTANCE.play( Assets.SND_DRINK );
				chr.getSprite().operate( chr.getPos());

                QuickSlot.refresh(chr);

            } else {
				GLog.w( Game.getVar(R.string.DewVial_Empty) );
			}
			
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
	
	public boolean isFull() {
		return getVolume() >= MAX_VOLUME;
	}
	
	public void collectDew(@NotNull Dewdrop dew ) {
		
		GLog.i( Game.getVar(R.string.DewVial_Collected) );
		setVolume(getVolume() + dew.quantity());
		if (getVolume() >= MAX_VOLUME) {
			setVolume(MAX_VOLUME);
			GLog.p( Game.getVar(R.string.DewVial_Full) );
		}

        QuickSlot.refresh(getOwner());
    }
	
	public void fill() {
		setVolume(MAX_VOLUME);
        QuickSlot.refresh(getOwner());
    }
	
	public static void autoDrink(@NotNull Hero hero ) {
		DewVial vial = hero.getBelongings().getItem( DewVial.class );
		if (vial != null && vial.isFull()) {

			hero.hp(1);//resurrect Hero because simple heal can't
			vial.execute( hero );
			hero.getSprite().emitter().start( ShaftParticle.FACTORY, 0.2f, 3 );
			
			GLog.w( Game.getVar(R.string.DewVial_AutoDrink) );
		}
	}
	
	private static final Glowing WHITE = new Glowing( 0xFFFFCC );
	
	@Override
	public Glowing glowing() {
		return isFull() ? WHITE : null;
	}
	
	@Override
	public String status() {
		return Utils.format( TXT_STATUS, getVolume(), MAX_VOLUME );
	}
	
	@NotNull
    @Override
	public String toString() {
		return super.toString() + " (" + status() +  ")" ;
	}

	@Override
	public int image() {
		if(volume == 0) {
			return  0;
		} else if(volume < MAX_VOLUME/2) {
			return  1;
		} else if(volume < MAX_VOLUME) {
			return  2;
		}
		return  3;
	}

	private int getVolume() {
		return volume;
	}

	private void setVolume(int volume) {
		this.volume = volume;
	}
}
