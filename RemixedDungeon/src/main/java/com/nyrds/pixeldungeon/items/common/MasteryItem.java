package com.nyrds.pixeldungeon.items.common;

import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.actors.hero.HeroSubClass;
import com.watabou.pixeldungeon.effects.Speck;
import com.watabou.pixeldungeon.effects.SpellSprite;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;

import org.jetbrains.annotations.NotNull;

public class MasteryItem extends Item {

	public static final float TIME_TO_READ = 10;

	@Override
	public boolean doPickUp(@NotNull Char hero ) {
		if(givesMasteryTo(hero)) {
			Badges.validateMastery(hero.getHeroClass());
		}
		return super.doPickUp( hero );
	}

	public boolean givesMasteryTo(Char hero) {
		return false;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	public static void choose(Item masteryItem, HeroSubClass way) {

		Char owner = masteryItem.getOwner();
		if(! (owner instanceof Hero) ) {
			throw new TrackedRuntimeException("Mobs can't subclass yet");
		}

		Hero hero = (Hero)owner;
		masteryItem.detach( hero.getBelongings().backpack );

		hero.setSubClass(way);

		hero.getSprite().operate( hero.getPos());
		Sample.INSTANCE.play( Assets.SND_MASTERY );

		SpellSprite.show(hero, SpellSprite.MASTERY );
		hero.getSprite().emitter().burst( Speck.factory( Speck.MASTERY ), 12 );
		if (way == HeroSubClass.LICH) {
			int penalty = 2;
			GLog.w(Utils.format(Game.getVar(R.string.Necromancy_BecameALich), penalty) );
			hero.STR(hero.STR() - penalty);
			hero.setMaxSkillPoints(hero.getSkillPointsMax() * 2);
		}

		GLog.w(Game.getVar(R.string.TomeOfMastery_Choose), Utils.capitalize( way.title() ) );

		hero.checkIfFurious();
		hero.updateSprite();

		hero.spend( TIME_TO_READ );
	}
}
