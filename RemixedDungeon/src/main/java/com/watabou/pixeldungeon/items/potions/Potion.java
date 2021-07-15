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
package com.watabou.pixeldungeon.items.potions;

import com.nyrds.pixeldungeon.items.common.UnknownItem;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.utils.ItemsList;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.hero.Belongings;
import com.watabou.pixeldungeon.effects.Splash;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.items.ItemStatusHandler;
import com.watabou.pixeldungeon.items.bags.PotionBelt;
import com.watabou.pixeldungeon.items.food.RottenFood;
import com.watabou.pixeldungeon.items.scrolls.Scroll;
import com.watabou.pixeldungeon.items.weapon.missiles.Arrow;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.sprites.ItemSprite;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndBag;
import com.watabou.pixeldungeon.windows.WndOptions;
import com.watabou.utils.Bundle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;

public class Potion extends Item implements UnknownItem {

	private static final String AC_DRINK   = "Potion_ACDrink";
	private static final String AC_MOISTEN = "Potion_ACMoisten";
	
	private static final float TIME_TO_DRINK = 1f;
	private static final float TIME_TO_MOISTEN = 1f;

	protected int labelIndex = -1;
	
	private static final Class<?>[] potions = {
		PotionOfHealing.class, 
		PotionOfExperience.class, 
		PotionOfToxicGas.class, 
		PotionOfLiquidFlame.class,
		PotionOfStrength.class,
		PotionOfParalyticGas.class,
		PotionOfLevitation.class,
		PotionOfMindVision.class, 
		PotionOfPurity.class,
		PotionOfInvisibility.class,
		PotionOfMight.class,
		PotionOfFrost.class,
		PotionOfMana.class
	};

	private static final Integer[] images;

	private static ItemStatusHandler<Potion> handler;
	
	private final String color;

	static {
		images = new Integer[13];
		for (int i = 0;i<13;i++) {
			images[i] = 32 + i;
		}
	}

	{	
		stackable = true;
		setDefaultAction(AC_DRINK);
		imageFile = "items/potions.png";
	}
	
	
	private boolean shattered = false;

	@SuppressWarnings("unchecked")
	public static void initColors() {
		handler = new ItemStatusHandler<>((Class<? extends Potion>[]) potions, images);
	}
	
	public static void save( Bundle bundle ) {
		handler.save( bundle );
	}
	
	@SuppressWarnings("unchecked")
	public static void restore( Bundle bundle ) {
		handler = new ItemStatusHandler<>((Class<? extends Potion>[]) potions, images, bundle);
	}
	
	public Potion() {
		image = handler.index( this );
		color = Game.getVars(R.array.Potion_Colors)[ItemStatusHandler.indexByImage(image, images)];
	}
	
	@Override
	public ArrayList<String> actions(Char hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		actions.add( AC_MOISTEN );
		return actions;
	}
	
	@Override
	public void _execute(@NotNull final Char chr, @NotNull String action ) {
		switch (action) {
			case AC_DRINK:
				if (isKnown() && (
						this instanceof PotionOfLiquidFlame ||
								this instanceof PotionOfToxicGas ||
								this instanceof PotionOfParalyticGas ||
								this instanceof PotionOfFrost)) {

					GameScene.show(
							new WndOptions(Game.getVar(R.string.Potion_Harmfull),
									Game.getVar(R.string.Potion_SureDrink),
									Game.getVar(R.string.Potion_Yes),
									Game.getVar(R.string.Potion_No)) {
								@Override
								public void onSelect(int index) {
									if (index == 0) {
										drink(chr);
									}
								}
							}
					);

				} else {
					drink(chr);
				}

				break;
			case AC_MOISTEN:
				moisten(chr);
				break;
			default:

				super._execute(chr, action);

				break;
		}
	}
	
	@Override
	public void doThrow(@NotNull final Char chr) {

		if (isKnown() && (
			this instanceof PotionOfExperience || 
			this instanceof PotionOfHealing || 
			this instanceof PotionOfLevitation ||
			this instanceof PotionOfMindVision ||
			this instanceof PotionOfStrength ||
			this instanceof PotionOfInvisibility || 
			this instanceof PotionOfMight ||
			this instanceof PotionOfMana)) {
		
			GameScene.show( 
				new WndOptions( Game.getVar(R.string.Potion_Beneficial),
						Game.getVar(R.string.Potion_SureThrow),
						Game.getVar(R.string.Potion_Yes),
						Game.getVar(R.string.Potion_No) ) {
					@Override
					public void onSelect(int index) {
						if (index == 0) {
							Potion.super.doThrow(chr);
						}
					}
				}
			);
			
		} else {
			super.doThrow(chr);
		}
	}
	
	protected void drink(Char hero ) {
		
		detach( hero.getBelongings().backpack );
		
		hero.spend( TIME_TO_DRINK );
		onThrow( hero.getPos(), hero);
		
		Sample.INSTANCE.play( Assets.SND_DRINK );
		
		hero.getSprite().operate( hero.getPos());
		shattered = false;
	}
	
	private void moisten(Char hero) {
		
		hero.spend(TIME_TO_MOISTEN);

		GameScene.selectItem(hero, itemSelector, WndBag.Mode.MOISTABLE, Game.getVar(R.string.Potion_SelectForMoisten));
		
		hero.getSprite().operate( hero.getPos());
	}
	
	@Override
	protected void onThrow(int cell, @NotNull Char thrower) {
		if (thrower.getPos() == cell) {
			apply( thrower );
		} else if (Dungeon.level.map[cell] == Terrain.WELL || Dungeon.level.pit[cell]) {
			super.onThrow( cell, thrower);
		} else  {
			shatter( cell );
		}
	}
	
	protected void apply(Char hero ) {
		shatter( hero.getPos() );
	}

	protected boolean canShatter() {
		if(!shattered) {
			shattered = true;
			return true;
		}
		return false;
	}

	public void shatter( int cell ) {
		GLog.i(Utils.format(Game.getVar(R.string.Potion_Shatter), color()));
		Sample.INSTANCE.play( Assets.SND_SHATTER );
		splash( cell );
	}
	
	public boolean isKnown() {
		return handler.isKnown( this );
	}
	
	public void setKnown() {
		if (!isKnown()) {
			handler.know( this );
		}
		
		Badges.validateAllPotionsIdentified();
	}
	
	@Override
	public Item identify() {
		setKnown();
		return this;
	}
	
	protected String color() {
		return color;
	}
	
	@Override
	public String name() {
		return isKnown() ? name : Utils.format(Game.getVar(R.string.Potion_Name), color);
	}
	
	@Override
	public String info() {
		return isKnown() ? desc() : Utils.format(Game.getVar(R.string.Potion_Info), color);
	}
	
	@Override
	public boolean isIdentified() {
		return isKnown();
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	public static HashSet<Class<? extends Potion>> getKnown() {
		return handler.known();
	}
	
	public static HashSet<Class<? extends Potion>> getUnknown() {
		return handler.unknown();
	}
	
	public static boolean allKnown() {
		return handler.known().size() == potions.length;
	}
	
	protected void splash( int cell ) {

		if(!GameScene.isSceneReady()) {
			return;
		}

		final int color = ItemSprite.pick( image, 8, 10 );
		Splash.at( cell, color, 5 );
	}

	public int basePrice() {
		return 20;
	}

	@Override
	public int price() {
		return (int) ((isKnown() ? basePrice() : 20) * quantity() * qualityFactor());
	}
	
	@Override
	public Item freeze(int cell){
		shatter(cell);
		return null;
	}
	
	private final WndBag.Listener itemSelector = (item, selector) -> {
		if (item != null) {

			if(item instanceof Arrow) {
				moistenArrow ((Arrow) item, item.getOwner());
			}

			if(item instanceof Scroll) {
				moistenScroll ((Scroll) item, item.getOwner());
			}

			if(item instanceof RottenFood) {
				moistenRottenFood((RottenFood) item, item.getOwner());
			}
		}
	};

	protected int detachMoistenItems(Item item, int maxQuantity) {
		setKnown();
		int quantity = item.quantity();
		
		if(quantity <= maxQuantity){
			Char owner = getOwner();

			if(item.equals(owner.getItemFromSlot(Belongings.Slot.WEAPON))) {
				owner.getBelongings().setItemForSlot(ItemsList.DUMMY, Belongings.Slot.WEAPON);
			} else {
				item.detachAll( owner.getBelongings().backpack );
			}
		} else {
			item.quantity(item.quantity() - maxQuantity);
			quantity = maxQuantity;
		}
		return quantity;
	}

	double qualityFactor() {
		return 1;
	}

	protected int reallyMoistArrows(Arrow arrow) {
		int quantity = detachMoistenItems(arrow, (int) (10*qualityFactor()));
		moistenEffective();
		GLog.i(Game.getVar(R.string.Potion_ArrowMoisten));
		return quantity;
	}

	protected void moistenRottenFood(RottenFood scroll, Char owner) {
		moistenUseless();
	}
	
	protected void moistenScroll(Scroll scroll, Char owner) {
		moistenUseless();
	}
	
	protected void moistenArrow(Arrow arrow, Char owner) {
		moistenUseless();
	}
	
	private void moistenUseless() {
		Char owner = getOwner();

		detach(owner.getBelongings().backpack );
		GLog.i(Game.getVar(R.string.Potion_MoistenUseless));
		owner.getSprite().operate( owner.getPos());
		owner.spend( TIME_TO_MOISTEN );
	}
	
	protected void moistenEffective() {
		Char owner = getOwner();

		detach(owner.getBelongings().backpack );
		identify();
		owner.getSprite().operate( owner.getPos());
		owner.spend( TIME_TO_MOISTEN );
	}

	@Override
	public int overlayIndex() {
		if(!isIdentified()) {
			return super.overlayIndex();
		}
		return labelIndex;
	}

	@Override
	public String bag() {
		return PotionBelt.class.getSimpleName();
	}
}
