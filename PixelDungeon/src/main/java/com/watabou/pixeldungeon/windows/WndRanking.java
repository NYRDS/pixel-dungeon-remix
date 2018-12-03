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
package com.watabou.pixeldungeon.windows;

import com.nyrds.android.util.GuiProperties;
import com.nyrds.pixeldungeon.mechanics.spells.Spell;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.windows.ScrollableList;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Text;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Button;
import com.watabou.noosa.ui.Component;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.Statistics;
import com.watabou.pixeldungeon.actors.hero.Belongings;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.scenes.PixelScene;
import com.watabou.pixeldungeon.sprites.HeroSpriteDef;
import com.watabou.pixeldungeon.ui.BadgesList;
import com.watabou.pixeldungeon.ui.Icons;
import com.watabou.pixeldungeon.ui.ItemSlot;
import com.watabou.pixeldungeon.ui.QuickSlot;
import com.watabou.pixeldungeon.ui.RedButton;
import com.watabou.pixeldungeon.ui.ScrollPane;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.elements.RankingTab;
import com.watabou.pixeldungeon.windows.elements.Tab;

import java.util.Locale;

public class WndRanking extends WndTabbed {
	
	private static final int WIDTH			= 112;
	private static final int HEIGHT			= 144;
	
	private static final int TAB_WIDTH	= 40;
	
	private Thread thread;
	private String error = null;

	private Image busy;
	
	public WndRanking( final String gameFile ) {
		
		super();
		resize( WIDTH, HEIGHT );
		
		thread = new Thread() {
			@Override
			public void run() {
				try {
					Badges.loadGlobal();
					Dungeon.loadGameForRankings( gameFile );
				} catch (Exception e ) {
					error = Game.getVar(R.string.WndRanking_Error) + "->" +e.getMessage();
					GLog.i(error);
				}
			}
		};
		thread.start();
		
		busy = Icons.BUSY.get();	
		busy.origin.set( busy.width / 2, busy.height / 2 );
		busy.angularSpeed = 720;
		busy.x = (WIDTH - busy.width) / 2;
		busy.y = (HEIGHT - busy.height) / 2;
		add( busy );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (thread != null && !thread.isAlive()) {
			thread = null;
			if (error == null) {
				remove( busy );
				createControls();
			} else {
				hide();
				Game.scene().add( new WndError( Game.getVar(R.string.WndRanking_Error) ) );
			}
		}
	}
	
	private void createControls() {
		
		String[] labels = 
			{ Game.getVar(R.string.WndRanking_Stats),
					Game.getVar(R.string.WndRanking_Items),
					Game.getVar(R.string.WndRanking_Badges) };
		Group[] pages = 
			{new StatsTab(), new ItemsTab(), new BadgesTab()};
		
		for (int i=0; i < pages.length; i++) {
			
			add( pages[i] );
			
			Tab tab = new RankingTab(this, labels[i], pages[i] );
			tab.setSize( TAB_WIDTH, tabHeight() );
			add( tab );
		}
		
		select( 0 );
	}
	
	private class StatsTab extends Group {
		
		private static final int GAP	= 4;

		public StatsTab() {
			super();
			
			Hero hero = Dungeon.hero;
			String heroClass = hero.className();

			HeroSpriteDef heroSprite = HeroSpriteDef.createHeroSpriteDef(hero);

			IconTitle title = new IconTitle();
			title.icon( heroSprite.avatar() );
			title.label( Utils.format( Game.getVar(R.string.WndRanking_StaTitle), hero.lvl(), heroClass ).toUpperCase( Locale.ENGLISH ) );
			title.setRect( 0, 0, WIDTH, 0 );
			title.color(0xCC33FF);
			add( title );
			
			float pos = title.bottom();
			
			if (Dungeon.challenges > 0) {
				RedButton btnCatalogus = new RedButton( Game.getVar(R.string.WndRanking_StaChallenges) ) {
					@Override
					protected void onClick() {
						Game.scene().add( new WndChallenges( Dungeon.challenges, false ) );
					}
				};
				btnCatalogus.setRect( 0, pos + GAP, btnCatalogus.reqWidth() + 2, btnCatalogus.reqHeight() + 2 );
				add( btnCatalogus );
				
				pos = btnCatalogus.bottom();
			}
			
			pos += GAP + GAP;
			
			pos = statSlot( this, difficultyToText(hero.getDifficulty()), "", pos );
			
			pos += GAP;
			
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaStr), Integer.toString( hero.effectiveSTR() ), pos );
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaHealth), Integer.toString( hero.ht() ), pos );
			
			pos += GAP;
			
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaDuration), Integer.toString( (int)Statistics.duration ), pos );
			
			pos += GAP;
			
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaDepth), Integer.toString( Statistics.deepestFloor ), pos );
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaEnemies), Integer.toString( Statistics.enemiesSlain ), pos );
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaGold), Integer.toString( Statistics.goldCollected ), pos );
			
			pos += GAP;
			
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaFood), Integer.toString( Statistics.foodEaten ), pos );
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaAlchemy), Integer.toString( Statistics.potionsCooked ), pos );
			pos = statSlot( this, Game.getVar(R.string.WndRanking_StaAnkhs), Integer.toString( Statistics.ankhsUsed ), pos );
		}
		
		private String difficultyToText(int difficulty) {

			switch (difficulty) {
			case 0:
				return Game.getVar(R.string.StartScene_DifficultyEasy);
			case 1:
				return Game.getVar(R.string.StartScene_DifficultyNormalWithSaves);
			case 2:
				return Game.getVar(R.string.StartScene_DifficultyNormal);
			case 3:
				return Game.getVar(R.string.StartScene_DifficultyExpert);
			}
			
			return "";
		}
		
		private float statSlot( Group parent, String label, String value, float pos ) {
			
			Text txt = PixelScene.createText( label, GuiProperties.regularFontSize() );
			txt.y = pos;
			parent.add( txt );
			
			txt = PixelScene.createText( value, GuiProperties.regularFontSize() );
			txt.x = PixelScene.align( WIDTH * 0.65f );
			txt.y = pos;
			parent.add( txt );
			
			return pos + GAP + txt.baseLine();
		}
	}
	
	private class ItemsTab extends Component {
		
		private float posY;

		ScrollPane list;

		public ItemsTab() {
			super();

			setSize(WIDTH,HEIGHT);
			camera = WndRanking.this.camera;

			list = new ScrollableList(new Component());
			add(list);

			Belongings stuff = Dungeon.hero.belongings;
			if (stuff.weapon != null) {
				addItem( stuff.weapon );
			}
			if (stuff.armor != null) {
				addItem( stuff.armor );
			}
			if (stuff.ring1 != null) {
				addItem( stuff.ring1 );
			}
			if (stuff.ring2 != null) {
				addItem( stuff.ring2 );
			}
			
			for(int i = 0;i<25;++i) {
				Item qsItem = QuickSlot.getEarlyLoadItem(i);
				if(qsItem != null && (Dungeon.hero.belongings.backpack.contains(qsItem) || qsItem instanceof Spell.SpellItem)){
					addItem(qsItem);
				}
			}

			list.content().setRect(0,0,width, posY);
			list.setSize(WIDTH,HEIGHT);
			list.scrollTo(0,0);
		}
		
		private void addItem( Item item ) {
			ItemButton slot = new ItemButton( item );
			slot.setRect( 0, posY, width, ItemButton.HEIGHT );
			list.content().add( slot );
			
			posY += slot.height() + 1;
		}
	}
	
	private class BadgesTab extends Group {
		
		public BadgesTab() {
			super();
			
			camera = WndRanking.this.camera;
			
			ScrollPane list = new BadgesList( false );
			add( list );
			
			list.setSize( WIDTH, HEIGHT );
		}
	}
	
	private class ItemButton extends Button {
		
		public static final int HEIGHT	= 23;
		
		private Item item;
		
		private ItemSlot slot;
		private ColorBlock bg;
		private Text name;
		
		public ItemButton( Item item ) {
			
			super();

			this.item = item;
			
			slot.item( item );
			if (item.cursed && item.cursedKnown) {
				bg.ra = +0.2f;
				bg.ga = -0.1f;
			} else if (!item.isIdentified()) {
				bg.ra = 0.1f;
				bg.ba = 0.1f;
			}
		}
		
		@Override
		protected void createChildren() {	
			
			bg = new ColorBlock( HEIGHT, HEIGHT, 0xFF4A4D44 );
			add( bg );
			
			slot = new ItemSlot();
			add( slot );
			
			name = PixelScene.createText( "?", GuiProperties.smallFontSize());
			add( name );
			
			super.createChildren();
		}
		
		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;
			
			slot.setRect( x, y, HEIGHT, HEIGHT );

			hotArea.x = x;
			hotArea.y = y;
			hotArea.width = HEIGHT;
			hotArea.height = HEIGHT;

			name.x = slot.right() + 2;
			name.y = y + (height - name.baseLine()) / 2;
			
			String str = Utils.capitalize( item.name() );
			name.text( str );
			if (name.width() > width - name.x) {
				do {
					str = str.substring( 0, str.length() - 1 );
					name.text( str + "..." );
				} while (name.width() > width - name.x);
			}
		}

		@Override
		protected void onTouchDown() {
			bg.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.SND_CLICK, 0.7f, 0.7f, 1.2f );
		}

		protected void onTouchUp() {
			bg.brightness( 1.0f );
		}

		@Override
		protected void onClick() {
			Game.scene().add( new WndItem( null, item ) );
		}
	}
}
