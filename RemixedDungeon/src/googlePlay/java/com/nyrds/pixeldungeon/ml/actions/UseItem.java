package com.nyrds.pixeldungeon.ml.actions;

import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.scenes.GameScene;

public class UseItem extends CharAction{

    public Item item;
    public String action;

    public UseItem(Item item, String action ) {
        this.item = item;
        this.action = action;
        dst = item.getOwner().getPos();
    }

    @Override
    public boolean act(Char hero) {
        item.execute(hero, action);

        if(GameScene.defaultCellSelector()) {
            hero.curAction = null;
            hero.next();
        } else {
            hero.curAction = null;
        }

        return false;
    }
}
