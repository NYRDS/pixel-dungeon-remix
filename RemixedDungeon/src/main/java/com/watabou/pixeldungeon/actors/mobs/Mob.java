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
package com.watabou.pixeldungeon.actors.mobs;

import com.nyrds.LuaInterface;
import com.nyrds.Packable;
import com.nyrds.android.util.JsonHelper;
import com.nyrds.android.util.ModdingMode;
import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.ai.AiState;
import com.nyrds.pixeldungeon.ai.Horrified;
import com.nyrds.pixeldungeon.ai.Hunting;
import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.ai.RunningAmok;
import com.nyrds.pixeldungeon.ai.Sleeping;
import com.nyrds.pixeldungeon.ai.Wandering;
import com.nyrds.pixeldungeon.items.Treasury;
import com.nyrds.pixeldungeon.items.common.ItemFactory;
import com.nyrds.pixeldungeon.items.common.Library;
import com.nyrds.pixeldungeon.items.common.armor.NecromancerRobe;
import com.nyrds.pixeldungeon.items.necropolis.BlackSkull;
import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.mobs.common.IDepthAdjustable;
import com.nyrds.pixeldungeon.mobs.common.MobFactory;
import com.nyrds.pixeldungeon.utils.CharsList;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.Statistics;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.CharUtils;
import com.watabou.pixeldungeon.actors.buffs.Amok;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.Burning;
import com.watabou.pixeldungeon.actors.buffs.Poison;
import com.watabou.pixeldungeon.actors.buffs.Regeneration;
import com.watabou.pixeldungeon.actors.buffs.Sleep;
import com.watabou.pixeldungeon.actors.buffs.Terror;
import com.watabou.pixeldungeon.actors.hero.Belongings;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.hero.HeroSubClass;
import com.watabou.pixeldungeon.effects.Flare;
import com.watabou.pixeldungeon.effects.Pushing;
import com.watabou.pixeldungeon.effects.Wound;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.features.Chasm;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.scenes.InterlevelScene;
import com.watabou.pixeldungeon.sprites.CharSprite;
import com.watabou.pixeldungeon.sprites.MobSpriteDef;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import lombok.SneakyThrows;

public abstract class Mob extends Char {

	public static final String TXT_RAGE = "#$%^";

	private static final float SPLIT_DELAY = 1f;

	protected Object spriteClass;

	protected int exp    = 1;
	protected int maxLvl = 50;

	@Packable(defaultValue = "false")
	public boolean enemySeen;

	public static final float TIME_TO_WAKE_UP = 1f;

	static private final Map<String, JSONObject> defMap = new HashMap<>();

	private static final String STATE      = "state";
	private static final String FRACTION   = "fraction";
    protected int dmgMin = 0;
    protected int dmgMax = 0;
    protected int attackSkill = 0;
    protected int dr = 0;

    public Mob() {
		super();
		setupCharData();
	}

	public void releasePet() {
		setFraction(Fraction.DUNGEON);
		setOwnerId(getId());
	}

	@LuaInterface
	@NotNull
	public static Mob makePet(@NotNull Mob pet, @NotNull Char owner) {
		return makePet(pet,owner.getId());
	}

	@LuaInterface
	@NotNull
	public Mob makePet(@NotNull Char owner) {
		return Mob.makePet(this, owner);
	}

	@NotNull
	public static Mob makePet(@NotNull Mob pet, int ownerId) {
		if (pet.canBePet()) {
			pet.setFraction(Fraction.HEROES);
			pet.setOwnerId(ownerId);
		}
		return pet;
	}

	@Override
	public boolean followOnLevelChanged(InterlevelScene.Mode changeMode) {
		return getOwner() instanceof Hero;
	}

	@LuaInterface
	public int getOwnerPos() {
		return getOwner().getPos();
	}

	public void setFraction(Fraction fr) {
		fraction = fr;
		setEnemy(CharsList.DUMMY);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);

		bundle.put(STATE,  getState().getTag());
		bundle.put(FRACTION, fraction.ordinal());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {

		super.restoreFromBundle(bundle);

		String state = bundle.getString(STATE);
		setState(state);

		fraction = Fraction.values()[bundle.optInt(FRACTION, Fraction.DUNGEON.ordinal())];

		if (bundle.contains("loot")) { //pre 29.6 saves compatibility
			loot(bundle.get("loot"), 1);
		}
	}

	@LuaInterface
	public void setState(String state) {
		setState(MobAi.getStateByTag(state));
	}

	protected int getKind() {
		return 0;
	}

	@SneakyThrows
	public CharSprite newSprite() {
		String descName = "spritesDesc/" + getEntityKind() + ".json";
		if (ModdingMode.isResourceExist(descName) || ModdingMode.isAssetExist(descName)) {
			return new MobSpriteDef(descName, getKind());
		}

		if (spriteClass instanceof Class) {
			CharSprite sprite = (CharSprite) ((Class<?>) spriteClass).newInstance();
			sprite.selectKind(getKind());
			return sprite;
		}

		if (spriteClass instanceof String) {
			return new MobSpriteDef((String) spriteClass, getKind());
		}

		throw new TrackedRuntimeException(String.format("sprite creation failed - me class %s", getEntityKind()));
	}

	@Override
	public boolean act() {

		super.act(); //Calculate FoV

		getSprite().hideAlert();

		if (paralysed) {
			enemySeen = false;
			spend(TICK);
			return true;
		}

		if (Random.Float() < 0.01/lvl()) {
			lvl(lvl()+1);
		}

		float timeBeforeAct = actorTime();

		script.runOptional("onAct");
		GLog.debug("%s is %s", getEntityKind(), getState().getTag());
		getState().act(this);

/*		if(actorTime() == timeBeforeAct && Util.isDebug()) {
			var error = String.format("actor %s has same timestamp after %s act!", getEntityKind(), getState().getTag());
			if(Util.isDebug()) {
				throw new ModError(error);
			} else {
				spend(MICRO_TICK);
				EventCollector.logException(error);
			}
		}*/

		return true;
	}


	public boolean isEnemyInFov(){
		final Char enemy = getEnemy();
		final int enemyPos = enemy.getPos();
		return enemy.valid() && enemy.isAlive() && level().cellValid(enemyPos) && level().fieldOfView[enemyPos]
                && enemy.invisible <= 0;
    }

	public void moveSprite(int from, int to) {

		if (getSprite().isVisible()
				&& (Dungeon.isPathVisible(from, to))) {
			getSprite().move(from, to);
		} else {
			getSprite().place(to);
		}
	}

	@Override
	public boolean add(Buff buff) {
		super.add(buff);

		if (!GameScene.isSceneReady()) {
			return true;
		}

		if (buff instanceof Amok) {
			getSprite().showStatus(CharSprite.NEGATIVE, TXT_RAGE);
			setState(MobAi.getStateByClass(RunningAmok.class));
		} else if (buff instanceof Terror) {
			setState(MobAi.getStateByClass(Horrified.class));
		} else if (buff instanceof Sleep) {
			new Flare(4, 32).color(0x44ffff, true).show(getSprite(), 2f);
			setState(MobAi.getStateByClass(Sleeping.class));
			postpone(Sleep.SWS);
		}
		return true;
	}

	public boolean canAttack(@NotNull Char enemy) {
		return !pacified && super.canAttack(enemy);
	}

	public boolean getCloser(int target) {
		int step = Dungeon.findPath(this, target, walkingType.passableCells(level()));
		return _doStep(step);
	}

	public boolean getFurther(int target) {
		int step = Dungeon.flee(this, target, walkingType.passableCells(level()));
		return _doStep(step);
	}

	public boolean _doStep(int step) {
		if (level().cellValid(step)) {
			move(step);
			return true;
		}
		return false;
	}

	@Override
	public void move(int step) {
		script.run("onMove", step);

		super.move(step);
	}

	public void doAttack(Char enemy) {

		setEnemy(enemy);

		spend(attackDelay());

		final int pos = getPos();
		final int enemyPos = enemy.getPos();

		if (level().distance(pos, enemyPos) <= 1) {
			if(Dungeon.visible[pos]) {
				getSprite().attack(enemyPos);
				onAttackComplete();
			}
		} else {
			if (Dungeon.isPathVisible(pos, enemyPos)) {
				getSprite().zap(enemyPos);
				onZapComplete();
			}
		}
	}

	@Override
	public final void onZapComplete() {
		zap(getEnemy());
		super.onZapComplete();
	}

	@Override
	public int defenseSkill(Char enemy) {
		return enemySeen ? super.defenseSkill(enemy) : 0;
	}

	@Override
	public int attackProc(@NotNull Char enemy, int damage) {
		int baseDamage = super.attackProc(enemy, damage);
		return script.run("onAttackProc", enemy, baseDamage).optint(baseDamage);
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		int baseDamage = super.defenseProc(enemy, damage);

		if (!enemySeen && enemy.getSubClass() == HeroSubClass.ASSASSIN) {
			baseDamage += Random.Int(1, baseDamage);
			Wound.hit(this);
		}

		if(getOwnerId() !=enemy.getId()) {
			setEnemy(enemy);
		}

		return script.run("onDefenceProc", enemy, baseDamage).optint(baseDamage);
	}

	@Override
	public void damage(int dmg,@NotNull NamedEntityKind src) {
		script.run("onDamage", dmg, src);

        getState().gotDamage(this,src, dmg);

		super.damage(dmg, src);
	}

	@Override
	public void destroy() {

    	spend(MICRO_TICK);

		super.destroy();

		level().mobs.remove(this);
	}

	public void remove() {
		super.die(this);
	}

	public void die(NamedEntityKind cause) {

    	spend(Actor.MICRO_TICK);
		getState().onDie(this);

		script.run("onDie", cause);

		Hero hero = Dungeon.hero;

		{
			//TODO we should move this block out of Mob class ( in script for example )
			if (hero.getHeroClass() == HeroClass.NECROMANCER){
				if (hero.isAlive()) {
					if(hero.getItemFromSlot(Belongings.Slot.ARMOR) instanceof NecromancerRobe){
						hero.accumulateSkillPoints();
					}
				}
			}

			for (Item item : hero.getBelongings()) {
				if (item instanceof BlackSkull && item.isEquipped(hero)) {
					((BlackSkull) item).mobDied(this, hero);
				}
			}
		}

		if (hero.isAlive()) {
			if (!friendly(hero)) {
				Statistics.enemiesSlain++;
				Badges.validateMonstersSlain();
				Statistics.qualifiedForNoKilling = false;

				if (Dungeon.nightMode) {
					Statistics.nightHunt++;
					Badges.validateNightHunter();
				} else {
					Statistics.nightHunt = 0;
				}

				if (!(cause instanceof Mob) || hero.getHeroClass() == HeroClass.NECROMANCER) {
					if (hero.lvl() <= (maxLvl + lvl()) && exp > 0) {
						hero.earnExp(exp);
					}
				}
			}
		}

		super.die(cause);

		Library.identify(Library.MOB, getEntityKind());

		if (!(cause instanceof Chasm)) {
			getBelongings().dropAll();
		}

		if (hero.isAlive() && !CharUtils.isVisible(this)) {
			GLog.i(Game.getVar(R.string.Mob_Died));
		}
	}

	public Mob split(int cell, int damage) {

		Mob clone = (Mob)makeClone();

		clone.hp(Math.max((hp() - damage) / 2, 1));
		clone.setPos(cell);
		clone.setState(MobAi.getStateByClass(Hunting.class));

		clone.ensureOpenDoor();

		level().spawnMob(clone, SPLIT_DELAY, getPos());

		if (hasBuff(Burning.class)) {
			Buff.affect(clone, Burning.class).reignite(clone);
		}
		if (hasBuff(Poison.class)) {
			Buff.affect(clone, Poison.class,2);
		}

		return clone;
	}

	protected void resurrect() {
		resurrect(this);
	}

	public void resurrect(Char parent) {

		int spawnPos = level().getEmptyCellNextTo(parent.getPos());
		Mob new_mob = MobFactory.mobByName(getMobClassName());

		if (level().cellValid(spawnPos)) {
			new_mob.setPos(spawnPos);
			if (parent instanceof Hero) {
				Mob.makePet(new_mob, parent.getId());
				Actor.addDelayed(new Pushing(new_mob, parent.getPos(), new_mob.getPos()), -1);
			}
			level().spawnMob(new_mob);
			level().press(spawnPos, new_mob);
		}
	}

	public boolean reset() {
		return false;
	}

	public void beckon(int cell) {
		notice();
		setState(MobAi.getStateByClass(Wandering.class));
		setTarget(cell);
	}

	public void fromJson(JSONObject mobDesc) throws JSONException, InstantiationException, IllegalAccessException {
		if (mobDesc.has("loot")) {
			float lootChance = (float) mobDesc.optDouble("lootChance", 1f);
			loot(ItemFactory.createItemFromDesc(mobDesc.getJSONObject("loot")), lootChance);
		}

		getBelongings().setupFromJson(mobDesc);

		if (this instanceof IDepthAdjustable) {
			((IDepthAdjustable) this).adjustStats(mobDesc.optInt("level", 1));
		}

		setState(mobDesc.optString("aiState",getState().getTag()));
	}

	public AiState getState() {
		return state;
	}

	protected JSONObject getClassDef(){
		if (!defMap.containsKey(getEntityKind())) {
			defMap.put(getEntityKind(), JsonHelper.tryReadJsonFromAssets("mobsDesc/" + getEntityKind() + ".json"));
		}

		return defMap.get(getEntityKind());
	}

	public void onSpawn(Level level) {
		Buff.affect(this, Regeneration.class);
		script.run("onSpawn",level);
	}


	public boolean isPet() {
		return fraction == Fraction.HEROES;
	}

	@Override
	public boolean friendly(@NotNull Char chr) {

		if(chr == this) {
			return true;
		}

		if(hasBuff(Amok.class) || chr.hasBuff(Amok.class)) {
			return false;
		}

		if(getOwnerId() == chr.getId()) {
			return true;
		}

		//if(getOwnerId() == chr.getOwnerId()) {
		//	return true;
		//}

		if(getEnemy() == chr) {
			return false;
		}

		if(getOwnerId()!=getId() && getOwner().friendly(chr)) {
			return true;
		}

		if(chr instanceof Hero) {
			return chr.getHeroClass().friendlyTo(getEntityKind());
		}

		return !this.fraction.isEnemy(chr.fraction);
	}

	@Override
	public boolean canBePet() {
		return true;
	}

	public boolean interact(Char chr) {
		return  script.run("onInteract", chr).optboolean(true) ||
				super.interact(chr);
	}

	@Override
	public boolean swapPosition(Char chr) {
		if(super.swapPosition(chr)) {
			setState(MobAi.getStateByClass(Wandering.class));
			return true;
		}
		return false;
	}

	public boolean zap(@NotNull Char enemy) {

		if(zapHit(enemy)) {
			int damage = zapProc(enemy,damageRoll());
			int effectiveDamage = enemy.defenseProc(this,damage);

			enemy.damage(effectiveDamage, this);
			return true;
		}

		return false;
	}

	protected int zapProc(@NotNull Char enemy, int damage) {
		return script.run("onZapProc", enemy, damage).optint(damage);
	}

	protected boolean zapHit(@NotNull Char enemy) {
		if (enemy == CharsList.DUMMY) {
			EventCollector.logException(String.format("%s zapping dummy enemy", getEntityKind()));
			return false;
		}

		if(!level().cellValid(enemy.getPos())) {
			EventCollector.logException(getEntityKind() + " zapping "+enemy.getEntityKind()+" on invalid cell");
			return false;
		}

		if (CharUtils.hit(this, enemy, true)) {
			return true;
		} else {
			enemy.getSprite().showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
			return false;
		}
	}

	@LuaInterface
	public String getMobClassName() {
		return getEntityKind();
	}

	@Nullable
	@LuaInterface
	public Object getLoot() {
		return getBelongings().randomUnequipped();
	}

	@Override
	public Char makeClone() {
		return MobFactory.mobByName(getEntityKind());
	}

	public void loot(Object loot, float lootChance) {

		if(Dungeon.hero.lvl() > maxLvl + 2 + lvl()) {
			return;
		}

		if (loot != null && Random.Float() <= lootChance) {
			Item item;
			if (loot instanceof Treasury.Category) {
				item = Treasury.getLevelTreasury().random((Treasury.Category) loot);
			} else if (loot instanceof Class<?>) {
				item = Treasury.getLevelTreasury().random((Class<? extends Item>) loot);
			} else if(loot instanceof String) {
				item = Treasury.getLevelTreasury().random((String) loot);
			} else {
				item = (Item) loot;
			}
			collect(item);
		}
	}

	@Override
	public int priceBuy(Item item) {
		return script.run("priceBuy", item, super.priceBuy(item)).toint();
	}

	@Override
	public int priceSell(Item item) {
		return script.run("priceSell", item, super.priceSell(item)).toint();
	}

	@Override
	public int damageRoll() {
		int dmg = Random.NormalIntRange(dmgMin, dmgMax);

		dmg += getActiveWeapon().damageRoll(this);

		if(!rangedWeapon.valid()) {
			dmg += getSecondaryWeapon().damageRoll(this);
		}

		return dmg;
	}

	@Override
	public int dr() {
		return getItemFromSlot(Belongings.Slot.ARMOR).effectiveDr() + dr;
	}
}
