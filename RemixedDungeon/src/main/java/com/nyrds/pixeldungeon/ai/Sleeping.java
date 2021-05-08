package com.nyrds.pixeldungeon.ai;

import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.utils.Utils;

public class Sleeping extends MobAi implements AiState {

    public Sleeping(){}

    @Override
    public void act(Mob me) {

        if(returnToOwnerIfTooFar(me, 3)) {
            return;
        }

        Char enemy = chooseEnemy(me, 0.5f);
        me.setEnemy(enemy);

        if (me.isEnemyInFov() ){

            huntEnemy(me);
            me.spend(Mob.TIME_TO_WAKE_UP);
        } else {
            me.enemySeen = false;
            me.spend(Actor.TICK);
        }
    }

    @Override
    public void gotDamage(Mob me, NamedEntityKind src, int dmg) {
        seekRevenge(me,src);
    }

    @Override
    public String status(Char me) {
        return Utils.format(Game.getVar(R.string.Mob_StaSleepingStatus),
                me.getName());
    }
}
