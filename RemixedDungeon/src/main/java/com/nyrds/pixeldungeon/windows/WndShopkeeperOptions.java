package com.nyrds.pixeldungeon.windows;

import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.utils.BuyItemSelector;
import com.nyrds.pixeldungeon.utils.SellItemSelector;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.bags.Bag;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndBag;
import com.watabou.pixeldungeon.windows.WndOptions;

public class WndShopkeeperOptions extends WndOptions {
    private final Char client;
    private final Bag backpack;

    private final Char shopkeeper;

    public WndShopkeeperOptions(Char shopkeeper, Char client) {
        super(Utils.capitalize(shopkeeper.getName()),
                Game.getVar(R.string.Shopkeeper_text),
                Game.getVar(R.string.Shopkeeper_SellPrompt),
                Game.getVar(R.string.Shopkeeper_BuyPrompt));
        this.client = client;
        this.backpack = shopkeeper.getBelongings().backpack;
        this.shopkeeper = shopkeeper;
    }

    @Override
    public void onSelect(int index) {
        switch (index) {
            case 0:
                GameScene.show(
                        new WndBag(client.getBelongings(),
                                    client.getBelongings().backpack,
                                    new SellItemSelector(shopkeeper),
                                    WndBag.Mode.FOR_SALE,
                                    Game.getVar(R.string.Shopkeeper_Sell)));
                break;
            case 1:
                GameScene.show(
                    new WndBag(shopkeeper.getBelongings(),
                                backpack,
                                new BuyItemSelector(shopkeeper),
                                WndBag.Mode.FOR_BUY,
                                Game.getVar(R.string.Shopkeeper_Buy)));
                break;
        }

    }
}
