package com.watabou.pixeldungeon.items.armor;

import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.hero.HeroSubClass;
import com.watabou.pixeldungeon.utils.GLog;

import org.jetbrains.annotations.NotNull;

public class WitchdoctorArmor extends GnollArmor {
	
	{
		name = Game.getVar(R.string.GnollArmor_Name);
		image = 25;
		hasHelmet = true;
		coverHair = true;
	}

	@Override
	public boolean doEquip(@NotNull Char hero ) {
		if (hero.getSubClass() == HeroSubClass.WITCHDOCTOR) {
			return super.doEquip( hero );
		} else {
			GLog.w( Game.getVar(R.string.GnollArmor_NotGnoll) );
			return false;
		}
	}
}