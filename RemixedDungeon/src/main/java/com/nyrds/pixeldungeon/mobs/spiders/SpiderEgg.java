package com.nyrds.pixeldungeon.mobs.spiders;

import com.nyrds.pixeldungeon.items.Treasury;
import com.nyrds.pixeldungeon.mobs.common.MobSpawner;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.mobs.Mob;

public class SpiderEgg extends Mob {

	public SpiderEgg() {
		hp(ht(2));
		baseDefenseSkill = 0;
		baseSpeed = 0f;

		exp = 0;
		maxLvl = 9;

		postpone(20);
		
		loot(Treasury.Category.SEED, 0.2f);

		movable = false;
	}

	@Override
    public boolean act() {
		super.act();

		Char newSpider = MobSpawner.spawnRandomMob(level(), getPos(), 15);

		if(newSpider.valid()) {
			remove();
			return true;
		}

		postpone(20);

		return true;
	}

	@Override
	public boolean canBePet() {
		return false;
	}
}
