package com.nyrds.pixeldungeon.items.chaos;

import com.nyrds.Packable;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.weapon.melee.SpecialWeapon;
import com.watabou.utils.Bundle;

public class ChaosSword extends SpecialWeapon implements IChaosItem {

	@Packable
	private int charge = 0;
	
	public ChaosSword() {
		super(3, 1, 1);
		
		imageFile = "items/chaosSword.png";
		image = 0;
		animation_class = HEAVY_ATTACK;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	private int chargeForLevel() {
		return (int) (5 * Math.pow(level(), 1.4));
	}
	
	@Override
	public void ownerTakesDamage(int damage) {
		charge--;
		if(charge < 0) {
			charge = 0;
		}
		
		if(level() > 3) {
			if(charge == 0) {
				degrade();
				enchant(null);
				charge = chargeForLevel();
				selectImage();
			}
		}
	}
	
	@Override
	public void ownerDoesDamage(Char ch,int damage) {
		
		if(cursed) {
			return;
		}
		
		if(damage > 0) {
			charge++;
			if(charge > chargeForLevel()) {
				upgrade(true);
				upgrade(true);
				selectImage();
				charge = 0;
			}
		}
	}
	
	private void selectImage() {
		image = Math.max(0, Math.min(level()/3, 4));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		selectImage();
	}
}
