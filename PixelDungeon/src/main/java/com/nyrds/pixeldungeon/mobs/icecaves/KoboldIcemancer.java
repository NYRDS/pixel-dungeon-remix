package com.nyrds.pixeldungeon.mobs.icecaves;

import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.mobs.common.IZapper;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.ResultDescriptions;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.Slow;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.items.Generator;
import com.watabou.pixeldungeon.items.weapon.enchantments.Death;
import com.watabou.pixeldungeon.mechanics.Ballistica;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.utils.Random;

import androidx.annotation.NonNull;

public class KoboldIcemancer extends Mob implements IZapper {

	public KoboldIcemancer() {
		hp(ht(70));
		defenseSkill = 18;

		exp = 11;
		maxLvl = 21;

		loot = Generator.Category.POTION;
		lootChance = 0.83f;

		RESISTANCES.add(Death.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(15, 17);
	}

	@Override
	public int attackSkill(Char target) {
		return 25;
	}

	@Override
	public int dr() {
		return 11;
	}

	@Override
    public boolean canAttack(Char enemy) {
		return Ballistica.cast(getPos(), enemy.getPos(), false, true) == enemy.getPos();
	}

	@Override
	public boolean zap(@NonNull Char enemy) {
		if(zapHit(enemy)) {

			if (enemy == Dungeon.hero && Random.Int(2) == 0) {
				Buff.prolong( enemy, Slow.class, 1 );
			}

			enemy.damage(damageRoll(), this);

			if (!enemy.isAlive() && enemy == Dungeon.hero) {
				Dungeon.fail(Utils.format(ResultDescriptions.getDescription(ResultDescriptions.Reason.MOB),
						Utils.indefinite(getName()), Dungeon.depth));
				GLog.n(Game.getVar(R.string.KoboldIcemancer_Killed), getName());
			}
			return true;
		}
		return false;
	}
}
