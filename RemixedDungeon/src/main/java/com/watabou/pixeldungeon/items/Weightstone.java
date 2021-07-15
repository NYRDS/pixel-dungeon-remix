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

import com.nyrds.android.util.GuiProperties;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.Text;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.weapon.Weapon;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.scenes.PixelScene;
import com.watabou.pixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.pixeldungeon.ui.RedButton;
import com.watabou.pixeldungeon.ui.Window;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.IconTitle;
import com.watabou.pixeldungeon.windows.WndBag;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Weightstone extends Item {
	
	private static final float TIME_TO_APPLY = 2;
	
	private static final String AC_APPLY = "Weightstone_ACApply";
	
	{
		name = Game.getVar(R.string.Weightstone_Name);
		image = ItemSpriteSheet.WEIGHT;
		
		stackable = true;
	}
	
	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_APPLY );
		return actions;
	}
	
	@Override
	public void _execute(@NotNull Char chr, @NotNull String action ) {
		if (action.equals(AC_APPLY)) {
			GameScene.selectItem(chr, itemSelector, WndBag.Mode.WEAPON, Game.getVar(R.string.Weightstone_Select));
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
	
	private void apply( Weapon weapon, boolean forSpeed ) {
		Char owner = getOwner();
		
		detach( owner.getBelongings().backpack );
		
		if (forSpeed) {
			weapon.imbue = Weapon.Imbue.SPEED;
			GLog.p( Game.getVar(R.string.Weightstone_Fast), weapon.name() );
		} else {
			weapon.imbue = Weapon.Imbue.ACCURACY;
			GLog.p( Game.getVar(R.string.Weightstone_Accurate), weapon.name() );
		}
		
		owner.getSprite().operate( owner.getPos());
		Sample.INSTANCE.play( Assets.SND_MISS );
		
		owner.spend( TIME_TO_APPLY );
	}
	
	@Override
	public int price() {
		return 40 * quantity();
	}
	
	private final WndBag.Listener itemSelector = (item, selector) -> {
		if (item != null) {
			GameScene.show( new WndBalance( (Weapon)item ) );
		}
	};
	
	public class WndBalance extends Window {

		private static final int WIDTH         = 120;
		private static final int BUTTON_WIDTH  = WIDTH - GAP * 2;
		
		public WndBalance( final Weapon weapon ) {
			super();
			
			IconTitle titlebar = new IconTitle( weapon );
			titlebar.setRect( 0, 0, WIDTH, 0 );
			add( titlebar );
			
			Text tfMesage = PixelScene.createMultiline( Utils.format( Game.getVar(R.string.Weightstone_WndChoice), weapon.name() ), GuiProperties.regularFontSize());
			tfMesage.maxWidth(WIDTH - GAP * 2);
			tfMesage.x = GAP;
			tfMesage.y = titlebar.bottom() + GAP;
			add( tfMesage );
			
			float pos = tfMesage.y + tfMesage.height();
			
			if (weapon.imbue != Weapon.Imbue.SPEED) {
				RedButton btnSpeed = new RedButton( Game.getVar(R.string.Weightstone_WndSpeed) ) {
					@Override
					protected void onClick() {
						hide();
						Weightstone.this.apply( weapon, true );
					}
				};
				btnSpeed.setRect(GAP, pos + GAP, BUTTON_WIDTH, BUTTON_HEIGHT );
				add( btnSpeed );
				
				pos = btnSpeed.bottom();
			}
			
			if (weapon.imbue != Weapon.Imbue.ACCURACY) {
				RedButton btnAccuracy = new RedButton( Game.getVar(R.string.Weightstone_WndAccuracy) ) {
					@Override
					protected void onClick() {
						hide();
						Weightstone.this.apply( weapon, false );
					}
				};
				btnAccuracy.setRect(GAP, pos + GAP, BUTTON_WIDTH, BUTTON_HEIGHT );
				add( btnAccuracy );
				
				pos = btnAccuracy.bottom();
			}
			
			RedButton btnCancel = new RedButton( Game.getVar(R.string.Weightstone_WndCancel) ) {
				@Override
				protected void onClick() {
					hide();
				}
			};
			btnCancel.setRect(GAP, pos + GAP, BUTTON_WIDTH, BUTTON_HEIGHT );
			add( btnCancel );
			
			resize( WIDTH, (int)btnCancel.bottom() + GAP);
		}
		
		protected void onSelect( int index ) {}
	}
}
