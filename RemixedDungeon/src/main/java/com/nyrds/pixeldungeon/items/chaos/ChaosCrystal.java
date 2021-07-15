package com.nyrds.pixeldungeon.items.chaos;

import com.nyrds.Packable;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.items.armor.Armor;
import com.watabou.pixeldungeon.items.rings.UsableArtifact;
import com.watabou.pixeldungeon.items.scrolls.Scroll;
import com.watabou.pixeldungeon.items.scrolls.ScrollOfWeaponUpgrade;
import com.watabou.pixeldungeon.items.wands.Wand;
import com.watabou.pixeldungeon.items.weapon.melee.KindOfBow;
import com.watabou.pixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.pixeldungeon.scenes.CellSelector;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.sprites.Glowing;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndBag;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ChaosCrystal extends UsableArtifact {

	private static final float TIME_TO_USE = 1;

	private static final String AC_FUSE             = "ChaosCrystal_Fuse";

	private static final int CHAOS_CRYSTAL_IMAGE = 9;
	private static final float TIME_TO_FUSE = 10;

	@Packable
	private int identetifyLevel = 0;

	@Packable
	private int charge = 0;

	public ChaosCrystal() {
		imageFile = "items/artifacts.png";
		image = CHAOS_CRYSTAL_IMAGE;
	}

	@Override
	public boolean isIdentified() {
		return identetifyLevel == 2;
	}

	@Override
	public Glowing glowing() {
		return new Glowing((int) (Math.random() * 0xffffff));
	}

	private final CellSelector.Listener chaosMark = new ChaosMarkListener();

	private final WndBag.Listener itemSelector = (item, selector) -> {
		if (item != null) {
			
			if (item.quantity() > 1) {
				item.detach(selector.getBelongings().backpack);
			} else {
				item.removeItemFrom(selector);
			}

			removeItemFrom(selector);

			selector.getSprite().operate(selector.getPos());
			selector.spend(TIME_TO_FUSE);

			if (item instanceof Scroll) {
				Item newItem = new ScrollOfWeaponUpgrade();
				selector.collect(newItem);
				GLog.p(Game.getVar(R.string.ChaosCrystal_ScrollFused), newItem.name());
				return;
			}

			if (item instanceof KindOfBow) {
				selector.collect(new ChaosBow());
				GLog.p(Game.getVar(R.string.ChaosCrystal_BowFused));
				return;
			}

			if (item instanceof MeleeWeapon) {
				selector.collect(new ChaosSword());
				GLog.p(Game.getVar(R.string.ChaosCrystal_SwordFused));
				return;
			}

			if (item instanceof Armor) {
				selector.collect(new ChaosArmor());
				GLog.p(Game.getVar(R.string.ChaosCrystal_ArmorFused));
				return;
			}

			if (item instanceof Wand) {
				selector.collect(new ChaosStaff());
				GLog.p(Game.getVar(R.string.ChaosCrystal_StaffFused));
			}
		}
	};

	private void fuse(Char hero) {
		GameScene.selectItem(hero, itemSelector, WndBag.Mode.FUSEABLE, Game.getVar(R.string.ChaosCrystal_SelectForFuse));
		hero.getSprite().operate(hero.getPos());
	}

	@Override
	public void _execute(@NotNull final Char ch, @NotNull String action) {
		switch (action) {
			case AC_USE:
				ch.selectCell(chaosMark);
				break;
			case AC_FUSE:
				fuse(ch);
				break;
			default:
				super._execute(ch, action);
				break;
		}
	}

	@Override
	public ArrayList<String> actions(Char hero) {
		ArrayList<String> actions = super.actions(hero);
		if (charge == 0 || identetifyLevel == 0) {
			actions.remove(AC_USE);
		} else {
			setDefaultAction(AC_USE);
		}

		if (charge >= 50 && identetifyLevel > 1) {
			actions.add(AC_FUSE);
		}
		return actions;
	}

	@Override
	public Item identify() {
		identetifyLevel++;
		return this;
	}

	@Override
	public String name() {
		switch (identetifyLevel) {
			default:
				return super.name();
			case 1:
				return Game.getVar(R.string.ChaosCrystal_Name_1);
			case 2:
				return Game.getVar(R.string.ChaosCrystal_Name_2);
		}
	}

	@Override
	public String info() {
		switch (identetifyLevel) {
			default:
				return super.info();
			case 1:
				return Game.getVar(R.string.ChaosCrystal_Info_1);
			case 2:
				return Game.getVar(R.string.ChaosCrystal_Info_2);
		}
	}

	@Override
	public String getText() {
		if (identetifyLevel > 0) {
			return Utils.format("%d/100", charge);
		} else {
			return null;
		}
	}

	@Override
	public int getColor() {
		return 0xe0a0a0;
	}

	@Override
	public void ownerTakesDamage(int damage) {
		if (damage > 0) {
			charge++;
			if (charge > 100) {
				charge = 100;
			}
		}
	}

	@Override
	public void ownerDoesDamage(int damage) {
		if (isCursed()) {
			if (charge > 0) {
				ChaosCommon.doChaosMark(getOwner().getPos(), charge);
			}
		}
	}

	private class ChaosMarkListener implements CellSelector.Listener {
		@Override
		public void onSelect(Integer cell, @NotNull Char selector) {
			if (cell != null) {

				if (isCursed()) {
					cell = selector.getPos();
				}

				ChaosCommon.doChaosMark(cell, charge);
				charge = 0;
			}
			selector.spend(TIME_TO_USE);
		}

		@Override
		public String prompt() {
			return Game.getVar(R.string.ChaosCrystal_Prompt);
		}

		@Override
		public Image icon() {
			return null;
		}
	}
}
