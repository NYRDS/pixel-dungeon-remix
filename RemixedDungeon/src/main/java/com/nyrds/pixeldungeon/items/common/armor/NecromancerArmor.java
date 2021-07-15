package com.nyrds.pixeldungeon.items.common.armor;

import com.nyrds.pixeldungeon.mechanics.buffs.Necrotism;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.items.armor.ClassArmor;
import com.watabou.pixeldungeon.utils.GLog;

import org.jetbrains.annotations.NotNull;

public class NecromancerArmor extends ClassArmor {


	public NecromancerArmor() {
		image = 22;
		hasHelmet = true;
	}

	public String desc() {
		return info2;
	}

	@Override
	public String special() {
		return "Necrotism_ACSpecial";
	}

	@Override
	public void doSpecial(@NotNull Char user) {
		user.spend( Actor.TICK );
		user.getSprite().operate( user.getPos());

		Buff.affect( user, Necrotism.class ).set(Necrotism.duration, 1);

		user.getSprite().burst( 0x6935a5, 3 );
		Sample.INSTANCE.play( Assets.SND_READ );
	}
	
	@Override
	public boolean doEquip(@NotNull Char hero ) {
		if (hero.getHeroClass() == HeroClass.NECROMANCER) {
			return super.doEquip( hero );
		} else {
			GLog.w( Game.getVar(R.string.NecromancerArmor_NotNecromancer) );
			return false;
		}
	}
}