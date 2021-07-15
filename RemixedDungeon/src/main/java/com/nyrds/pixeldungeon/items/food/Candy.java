package com.nyrds.pixeldungeon.items.food;

import com.nyrds.pixeldungeon.mechanics.CommonActions;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.effects.SpellSprite;
import com.watabou.pixeldungeon.items.food.Food;
import com.watabou.pixeldungeon.utils.GLog;

import org.jetbrains.annotations.NotNull;

public class Candy extends Food {

	public Candy() {
		imageFile = "items/artifacts.png";
		image = 21;
	}

	@Override
	public void _execute(@NotNull Char chr, @NotNull String action ) {
		if (action.equals( CommonActions.AC_EAT )) {

			detach( chr.getBelongings().backpack );

			GLog.w(Game.getVar(R.string.Candy_Warning_1));

			chr.getSprite().operate( chr.getPos());
			SpellSprite.show(chr, SpellSprite.FOOD );
			Sample.INSTANCE.play( Assets.SND_EAT );

			chr.spend( TIME_TO_EAT );

		} else {

			super._execute(chr, action );

		}
	}

	@Override
	public int price() {
		return 20 * quantity();
	}

}
