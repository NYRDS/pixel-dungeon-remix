package com.nyrds.pixeldungeon.mobs.necropolis;

import com.nyrds.Packable;
import com.nyrds.pixeldungeon.ai.Hunting;
import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.items.necropolis.BlackSkull;
import com.nyrds.pixeldungeon.items.necropolis.BlackSkullOfMastery;
import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.blobs.Blob;
import com.watabou.pixeldungeon.actors.blobs.ToxicGas;
import com.watabou.pixeldungeon.actors.buffs.Amok;
import com.watabou.pixeldungeon.actors.buffs.Blindness;
import com.watabou.pixeldungeon.actors.buffs.Paralysis;
import com.watabou.pixeldungeon.actors.buffs.Sleep;
import com.watabou.pixeldungeon.actors.buffs.Terror;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.mobs.Boss;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.actors.mobs.Skeleton;
import com.watabou.pixeldungeon.effects.CellEmitter;
import com.watabou.pixeldungeon.effects.particles.ShadowParticle;
import com.watabou.pixeldungeon.items.keys.SkeletonKey;
import com.watabou.pixeldungeon.items.potions.PotionOfHealing;
import com.watabou.pixeldungeon.items.wands.WandOfBlink;
import com.watabou.pixeldungeon.items.weapon.enchantments.Death;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.mechanics.Ballistica;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by DeadDie on 12.02.2016
 */
public class Lich extends Boss {

    private static final int SKULLS_BY_DEFAULT	= 3;
    private static final int SKULLS_MAX	= 4;
    private static final int HEALTH	= 200;
    private static final int SKULL_DELAY = 5;

    private RunicSkull activatedSkull;

    @Packable
    private boolean skullsSpawned = false;

    @Packable
    private boolean timeToJump = false;

	private final HashSet<RunicSkull> skulls = new HashSet<>();

    public Lich() {
        hp(ht(HEALTH));
        exp = 25;
        baseDefenseSkill = 23;
        baseAttackSkill  = 35;

        dmgMin = 12;
        dmgMax = 20;
        dr = 15;

        addImmunity( Paralysis.class );
        addImmunity( ToxicGas.class );
        addImmunity( Terror.class );
        addImmunity( Death.class );
        addImmunity( Amok.class );
        addImmunity( Blindness.class );
        addImmunity( Sleep.class );

        collect(new SkeletonKey());

        if ( Dungeon.hero.getHeroClass() == HeroClass.NECROMANCER){
            collect(new BlackSkullOfMastery());
        }
        else {
            collect(new BlackSkull());
        }
    }

    private int timeToSkull = SKULL_DELAY;


    @Override
    public boolean getCloser(int target) {
        if (Dungeon.level.fieldOfView[target]) {
            jump();
            return true;
        } else {
            return super.getCloser( target );
        }
    }

    @Override
    public boolean canAttack(@NotNull Char enemy) {
        return level().distance(getPos(), enemy.getPos()) < 4 && Ballistica.cast(getPos(), enemy.getPos(), false, true) == enemy.getPos();
    }

    @Override
    public void doAttack(Char enemy) {
        if(timeToJump) {
            jump();
        }

        super.doAttack(enemy);
    }

    private void jump() {
        for (int i = 0; i < 15; i++){
            Level level = Dungeon.level;
            int newPos = Random.Int( level.getLength() );

            if(level.fieldOfView[newPos] &&
                    level.passable[newPos] &&
                    !adjacent(getEnemy()) &&
                    Actor.findChar( newPos ) == null)
            {
                getSprite().move( getPos(), newPos );
                move( newPos );

                spend( 1 / speed() );
                break;
            }
        }
        timeToJump = false;
    }

    //Runic skulls handling
    //***

    private void activateRandomSkull(){
       if(!skullsSpawned) {
           skullsSpawned = true;
           spawnSkulls();
    }

        if (!skulls.isEmpty()){
            if (activatedSkull != null){
                activatedSkull.Deactivate();
            }

            RunicSkull skull = getRandomSkull();
            if(skull == null){
                activatedSkull = null;
            } else{
                skull.Activate();
                activatedSkull = skull;
            }
        }
    }

    private RunicSkull getRandomSkull() {
        while(!skulls.isEmpty()){
            RunicSkull skull = Random.element(skulls);
            if(skull.isAlive()){
                return skull;
            }
            else{
                skulls.remove(skull);
            }
        }
        return null;
    }

    private void useSkull(){
        getSprite().zap(getPos());

        switch (activatedSkull.getKind()) {
            case RunicSkull.RED_SKULL:
                PotionOfHealing.heal(this,0.07f * skulls.size());
                break;

            case RunicSkull.BLUE_SKULL:
                int i = 0;
                while (i < skulls.size()){
                    int pos = Dungeon.level.getEmptyCellNextTo(getPos());
                    if (Dungeon.level.cellValid(pos)) {
                        Skeleton skeleton = new Skeleton();
                        skeleton.setPos(pos);
                        skeleton.setState(MobAi.getStateByClass(Hunting.class));
                        Dungeon.level.spawnMob(skeleton, 0, getPos());
                        i++;
                    } else {
                        break;
                    }
                }
                break;

            case RunicSkull.GREEN_SKULL:
                GameScene.add( Blob.seed( getPos(), 30 * skulls.size(), ToxicGas.class ) );
                break;
        }
    }

    //***

    @Override
    public boolean act() {
        timeToSkull--;
        if (timeToSkull < 0){
            timeToSkull = SKULL_DELAY;
            activateRandomSkull();
            if (activatedSkull != null) {
                useSkull();
            }
        }
        return super.act();
    }


    @Override
    public int defenseProc(Char enemy, int damage) {
        if (activatedSkull != null)
        {
            if(activatedSkull.getKind() == RunicSkull.PURPLE_SKULL){
                return 0;
            }
        }

        if (Random.Int(2) == 1 && this.isAlive()){
            timeToJump = true;
        }

        return damage;
    }

    @Override
    public void die(NamedEntityKind cause) {
        super.die( cause );

        //Kill everything
        skulls.clear();

        for(Mob mob:level().getCopyOfMobsArray()) {
            mob.remove();
        }

        Badges.validateBossSlain(Badges.Badge.LICH_SLAIN);
    }

    private void spawnSkulls(){
        int nSkulls = SKULLS_BY_DEFAULT;
        if(Game.getDifficulty() == 0){
            nSkulls = 2;
        }
        else if(Game.getDifficulty() > 2){
            nSkulls = SKULLS_MAX;
        }

        Level level = Dungeon.level;
        ArrayList<Integer> pedestals = level.getAllTerrainCells(Terrain.PEDESTAL);

        Collections.shuffle(pedestals);

        Sample.INSTANCE.play( Assets.SND_CURSED );

        for (int i = 0;i < nSkulls && i < pedestals.size();++i) {
            RunicSkull skull = RunicSkull.makeNewSkull(i);

            CellEmitter.center(pedestals.get(i)).burst( ShadowParticle.CURSE, 8 );
            WandOfBlink.appear(skull, pedestals.get(i));

            skulls.add(skull);
        }
    }
}
