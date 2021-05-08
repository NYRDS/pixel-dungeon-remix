package com.nyrds.pixeldungeon.ml.actions;

import com.nyrds.pixeldungeon.ai.KillOrder;
import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.ai.MoveOrder;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.CharUtils;
import com.watabou.pixeldungeon.scenes.CellSelector;
import com.watabou.pixeldungeon.utils.Utils;

import org.jetbrains.annotations.NotNull;

import static com.watabou.pixeldungeon.actors.Actor.TICK;

public class Order extends CharAction {
    private final Char target;

    public Order(@NotNull Char target) {
        this.target = target;
        dst = target.getPos();
    }

    @Override
    public boolean act(Char hero) {
        hero.spend(TICK/10);
        hero.readyAndIdle();
        hero.selectCell(new OrderCellSelector());
        return false;
    }

    private class OrderCellSelector implements CellSelector.Listener {
        @Override
        public void onSelect(Integer cell, @NotNull Char selector) {
            if(cell == null) {
                return;
            }

            CharAction action = CharUtils.actionForCell(target, cell,target.level());

            if(action instanceof Move) {
                target.setState(MobAi.getStateByClass(MoveOrder.class));
                target.setTarget(cell);
                return;
            }

            if(action instanceof Attack) {
                Attack attack = (Attack)action;
                target.setState(MobAi.getStateByClass(KillOrder.class));
                target.setEnemy(attack.target);
                return;
            }

            target.say(Utils.format(Game.getVar(R.string.Mob_CantDoIt)));
        }

        @Override
        public String prompt() {
            return Utils.capitalize(Utils.format(Game.getVar(R.string.Mob_ReadyForOrder), target.getName()));
        }

        @Override
        public Image icon() {
            return null;
        }
    }
}
