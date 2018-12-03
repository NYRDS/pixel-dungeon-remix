package com.watabou.pixeldungeon.windows;

import com.nyrds.android.util.GuiProperties;
import com.nyrds.android.util.ModdingMode;
import com.nyrds.pixeldungeon.items.accessories.Accessory;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.support.IIapCallback;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.SystemText;
import com.watabou.noosa.Text;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.PixelDungeon;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.scenes.PixelScene;
import com.watabou.pixeldungeon.sprites.HeroSpriteDef;
import com.watabou.pixeldungeon.ui.RedButton;
import com.watabou.pixeldungeon.ui.TextButton;
import com.watabou.pixeldungeon.ui.Window;

public class WndHatInfo extends Window {

	private static final int WIDTH         = 110;
	private static final int HEIGHT        = 160;
	private static final int BUTTON_HEIGHT = 16;

	public WndHatInfo(final String accessory, String price, final Window parent ) {
		final Accessory item = Accessory.getByName(accessory);

		EventCollector.logScene(getClass().getCanonicalName()+":"+item.getClass().getSimpleName());

		// Title
		Text tfTitle = PixelScene.createMultiline(item.name(), GuiProperties.mediumTitleFontSize());
		tfTitle.hardlight(TITLE_COLOR);
		tfTitle.maxWidth(WIDTH - GAP);
		tfTitle.x = (WIDTH - tfTitle.width())/2;
		tfTitle.y = GAP;
		add(tfTitle);

		//Pricetag
		SystemText priceTag = new SystemText(price,GuiProperties.mediumTitleFontSize(),false);

		priceTag.hardlight(0xFFFF00);
		priceTag.maxWidth(WIDTH - GAP);
		priceTag.x = (WIDTH - priceTag.width()) / 2;
		priceTag.y = tfTitle.bottom() + GAP;
		add(priceTag);

		//Preview Image
		Image preview = (HeroSpriteDef.createHeroSpriteDef(Dungeon.hero, item)).avatar();
		preview.setPos(WIDTH / 2 - preview.width(), priceTag.bottom() + GAP);
		preview.setScale(2, 2);
		add(preview);

		//Text
		String hatText = Accessory.getByName(accessory).desc();

		Text info = PixelScene.createMultiline(hatText, GuiProperties.regularFontSize());

		info.hardlight(0xFFFFFF);

		info.y = preview.bottom() + GAP;
		info.maxWidth(WIDTH - GAP);
		info.x = (WIDTH - info.width()) / 2 ;

		add(info);

		//Button
		String buttonText = Game.getVar(R.string.WndHats_BuyButton);
		if(item.haveIt()) {
			buttonText = Game.getVar(R.string.WndHats_BackButton);
		}

		TextButton rb = new RedButton(buttonText) {
			@Override
			protected void onClick() {
				super.onClick();

				if(item.haveIt()) {
					onBackPressed();
					return;
				}

				Game.instance().runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								EventCollector.logEvent("PurchaseClick",item.name());
								PixelDungeon.instance().iap.doPurchase(accessory, new IIapCallback() {
									@Override
									public void onPurchaseOk() {
										item.ownIt(true);
										item.equip();
										Dungeon.hero.updateLook();
										onBackPressed();
										parent.hide();
										if(!Game.isPaused()) {
											GameScene.show(new WndHats());
										}
									}
								});
							}
						}
				);
			}
		};

		rb.enable(!ModdingMode.useRetroHeroSprites);

		if(!item.haveIt() && price == null) {
			rb.enable(false);
		}

		rb.setRect(WIDTH / 4, info.bottom() + GAP, WIDTH / 2, BUTTON_HEIGHT);
		add(rb);

		//Resizing window
		int h = Math.min(HEIGHT - GAP, (int) rb.bottom());
		resize( WIDTH,  h);
	}
}