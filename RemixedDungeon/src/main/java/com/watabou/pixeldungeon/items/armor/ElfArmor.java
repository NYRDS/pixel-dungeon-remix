package com.watabou.pixeldungeon.items.armor;

import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.blobs.Blob;
import com.watabou.pixeldungeon.actors.blobs.Regrowth;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.utils.GLog;

import org.jetbrains.annotations.NotNull;

public class ElfArmor extends ClassArmor {
	
	public ElfArmor() {
		image = 17;
		hasHelmet = true;
	}	
	
	@Override
	public String special() {
		return "ElfArmor_ACSpecial";
	}
	
	@Override
	public void doSpecial(@NotNull Char user) {

		final Level level = user.level();

		for (Mob mob : level.getCopyOfMobsArray()) {
			if (level.fieldOfView[mob.getPos()]) {
				GameScene.add( Blob.seed( mob.getPos(), 100, Regrowth.class ) );
			}
		}

		user.spend( Actor.TICK );
		user.getSprite().operate( user.getPos());

		Sample.INSTANCE.play( Assets.SND_READ );
		
		GameScene.add( Blob.seed( user.getPos(), 100, Regrowth.class ) );
	}
	
	@Override
	public boolean doEquip(@NotNull Char hero ) {
		if (hero.getHeroClass() == HeroClass.ELF) {
			return super.doEquip( hero );
		} else {
			GLog.w( Game.getVar(R.string.ElfArmor_NotElf) );
			return false;
		}
	}
	
	@Override
	public String desc() {
		return Game.getVar(R.string.ElfArmor_Desc);
	}
}