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
import com.nyrds.pixeldungeon.items.Treasury;
import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.utils.ItemsList;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.Statistics;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.Burning;
import com.watabou.pixeldungeon.actors.buffs.Frost;
import com.watabou.pixeldungeon.actors.mobs.Mimic;
import com.watabou.pixeldungeon.actors.mobs.Wraith;
import com.watabou.pixeldungeon.effects.CellEmitter;
import com.watabou.pixeldungeon.effects.Speck;
import com.watabou.pixeldungeon.effects.Splash;
import com.watabou.pixeldungeon.effects.particles.ElmoParticle;
import com.watabou.pixeldungeon.effects.particles.FlameParticle;
import com.watabou.pixeldungeon.effects.particles.ShadowParticle;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.plants.Seed;
import com.watabou.pixeldungeon.sprites.ItemSprite;
import com.watabou.pixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import lombok.Getter;

public class Heap implements Bundlable, NamedEntityKind {

	private static final int SEEDS_TO_POTION = 3;

	@Override
	public String getEntityKind() {
		return getClass().getSimpleName();
	}

	@Override
	public String name() {
		return getEntityKind();
	}

	public enum Type {
		HEAP, 
		CHEST,
		LOCKED_CHEST, 
		CRYSTAL_CHEST,
		TOMB, 
		SKELETON,
		MIMIC
	}

	@NotNull
	@Getter
	public Type type = Type.HEAP;

	public static Map<Type, Float> regularHeaps = new HashMap<>();
	static {
		regularHeaps.put(Type.SKELETON,1f);
		regularHeaps.put(Type.CHEST,4f);
		regularHeaps.put(Type.MIMIC,1f);
		regularHeaps.put(Type.HEAP,14f);
	}

	@Packable
	public int pos = Level.INVALID_CELL;

	@Nullable
	public ItemSprite sprite;

	@NotNull
	public LinkedList<Item> items = new LinkedList<>();

	public String imageFile() {
		if (type == Type.HEAP) {
			return size() > 0 ? items.peek().imageFile() : Assets.ITEMS;
		}
		return Assets.ITEMS;
	}

	public float scale() {
		Item topItem = items.peek();
		if (topItem != null) {
			return topItem.heapScale();
		}
		return 1.f;
	}

	public int image() {
		switch (type) {
		case HEAP:
			return size() > 0 ? items.peek().image() : 0;
		case CHEST:
		case MIMIC:
			return ItemSpriteSheet.CHEST;
		case LOCKED_CHEST:
			return ItemSpriteSheet.LOCKED_CHEST;
		case CRYSTAL_CHEST:
			return ItemSpriteSheet.CRYSTAL_CHEST;
		case TOMB:
			return ItemSpriteSheet.TOMB;
		case SKELETON:
			return ItemSpriteSheet.BONES;
		default:
			return 0;
		}
	}
	
	public ItemSprite.Glowing glowing() {
		return (type == Type.HEAP) && items.size() > 0 ? items.peek().glowing() : null;
	}
	
	public void open( Char chr ) {
		switch (type) {
		case MIMIC:
			if (Mimic.spawnAt( pos, items ) != null) {
				GLog.n( Game.getVar(R.string.Heap_Mimic) );
				destroy();
			} else {
				type = Type.CHEST;
			}
			break;
		case TOMB:
			Wraith.spawnAround( chr.getPos() );
			break;
		case SKELETON:
			CellEmitter.center( pos ).start( Speck.factory( Speck.RATTLE ), 0.1f, 3 );
			for (Item item : items) {
				if (item.isCursed()) {
					if (Wraith.spawnAt( pos ) == null) {
						chr.getSprite().emitter().burst( ShadowParticle.CURSE, 6 );
						chr.damage( chr.hp() / 2, this );
					}
					Sample.INSTANCE.play( Assets.SND_CURSED );
					break;
				}
			}
			break;
		default:
		}
		
		if (type != Type.MIMIC) {
			type = Type.HEAP;
			sprite.link();
			sprite.drop();
		}
	}
	
	public int size() {
		return items.size();
	}
	
	public Item pickUp() {
		Item item = items.removeFirst();
		updateHeap();
		return item;
	}

	public void pickUpFailed() {
		if(!isEmpty()) {
			Item item = items.removeFirst();
			items.addLast(item);
		}
		updateHeap();
	}

	public Item peek() {
		return items.peek();
	}
	
	public void drop(@NotNull Item item ) {

		if(!item.valid()) {
			EventCollector.logException("Invalid item");
			return;
		}

		if(items.contains(item)) { //TODO fix me
			return;
		}

		if (item.stackable) {
            String c = item.getEntityKind();
			for (Item i : items) {
                if (i.getEntityKind().equals(c)) {
					i.quantity(i.quantity() + item.quantity());
					item = i;
					break;
				}
			}
			items.remove( item );
		}

		items.addFirst(item);

		updateHeap();
	}
	
	public void replace( Item a, Item b ) {
		int index = items.indexOf( a );
		if (index != -1) {
			items.set(index,b);
		}
	}
	
	private void replaceOrRemoveItem(Item item, Item newItem){
		if(newItem == null){
			items.remove(item);
		}else{
			if(!item.equals(newItem) ){
				replace(item, newItem);
			}
		}
	}
	
	private void updateHeap(){
		if (isEmpty()) {
			destroy();
		} else {
			if (sprite != null) {
				float scale = scale();
				sprite.setScale(scale, scale);
				sprite.view(imageFile(), image(), glowing());
				sprite.place(pos);
			}
		}
	}
	
	public void burn() {
		
		if (type == Type.MIMIC) {
			Mimic m = Mimic.spawnAt( pos, items );
			if (m != null) {
				Buff.affect( m, Burning.class ).reignite( m );
				m.getSprite().emitter().burst( FlameParticle.FACTORY, 5 );
				destroy();
			}
		}
		if (type != Type.HEAP) {
			return;
		}
		
		boolean burnt = false;
		boolean evaporated = false;
		
		for (Item item : items.toArray(new Item[0])) {
			Item burntItem = item.burn(pos);
			
			if(!item.equals(burntItem) && !(item instanceof Dewdrop)){
				burnt = true;
			}

			if(item instanceof Dewdrop){
				evaporated = true;
			}

			replaceOrRemoveItem(item, burntItem);
		}
		
		if (burnt || evaporated) {
			
			if (Dungeon.visible[pos]) {
				if (burnt) {
					burnFX( pos );
				} else {
					evaporateFX( pos );
				}
			}
		}
		
		updateHeap();
	}
	
	public void freeze() {
		if (type == Type.MIMIC) {
			Mimic m = Mimic.spawnAt( pos, items );
			if (m != null) {
				Buff.prolong( m, Frost.class, Frost.duration( m ) * Random.Float( 1.0f, 1.5f ) );
				destroy();
			}
		}
		if (type != Type.HEAP) {
			return;
		}
		
		for (Item item : items.toArray(new Item[0])) {
			Item frozenItem = item.freeze(pos);
			
			replaceOrRemoveItem(item, frozenItem);
		}
		
		updateHeap();
	}
	
	public void poison(){
		if (type == Type.MIMIC) {
			Mimic m = Mimic.spawnAt( pos, items );
			if (m != null) {
				destroy();
			}
		}
		if (type != Type.HEAP) {
			return;
		}
		
		for (Item item : items.toArray(new Item[0])) {
			Item toxicatedItem = item.poison(pos);

			replaceOrRemoveItem(item, toxicatedItem);
		}
		
		updateHeap();
	}
	
	public Item transmute() {
		
		CellEmitter.get( pos ).burst( Speck.factory( Speck.BUBBLE ), 3 );
		Splash.at( pos, 0xFFFFFF, 3 );

		float[] chances = new float[items.size()];
		int count = 0;
		
		int index = 0;
		for (Item item : items) {
			if (item instanceof Seed) {
				count += item.quantity();
				chances[index++] = item.quantity();
			} else {
				count = 0;
				break;
			}
		}
		
		if (count >= SEEDS_TO_POTION) {
			
			CellEmitter.get( pos ).burst( Speck.factory( Speck.WOOL ), 6 );
			Sample.INSTANCE.play( Assets.SND_PUFF );
			
			if (Random.Int( count ) == 0) {
				
				CellEmitter.center( pos ).burst( Speck.factory( Speck.EVOKE ), 3 );
				
				destroy();
				
				Statistics.potionsCooked++;
				Badges.validatePotionsCooked();
				
				return Treasury.getLevelTreasury().random( Treasury.Category.POTION );
				
			} else {

				Seed proto = (Seed)items.get( Random.chances( chances ) );
				Class<? extends Item> itemClass = proto.alchemyClass;
				
				destroy();
				
				Statistics.potionsCooked++;
				Badges.validatePotionsCooked();
				
				if (itemClass == null) {
					return Treasury.getLevelTreasury().random( Treasury.Category.POTION );
				} else {
					try {
						return itemClass.newInstance();
					} catch (Exception e) {
						return ItemsList.DUMMY;
					}
				}
			}		
			
		} else {
			return ItemsList.DUMMY;
		}
	}
	
	public static void burnFX( int pos ) {
		CellEmitter.get( pos ).burst( ElmoParticle.FACTORY, 6 );
		Sample.INSTANCE.play( Assets.SND_BURNING );
	}
	
	private static void evaporateFX(int pos) {
		CellEmitter.get( pos ).burst( Speck.factory( Speck.STEAM ), 5 );
	}
	
	public boolean isEmpty() {
		return items.isEmpty();
	}
	
	public void destroy() {
		Dungeon.level.removeHeap( pos );
		if (sprite != null) {
			sprite.killAndErase();
		}
		items.clear();
	}

	private static final String TYPE	= "type";
	private static final String ITEMS	= "items";
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		try {
			type = Type.valueOf(bundle.getString(TYPE));
		} catch (Throwable e) {
			EventCollector.logException(e);
			type = Type.HEAP;
		}
		items = new LinkedList<>(bundle.getCollection(ITEMS, Item.class));
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( TYPE, type.toString() );
		bundle.put( ITEMS, items );
	}
	
	public boolean dontPack() {
		return false;
	}
}
