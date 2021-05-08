package com.nyrds.pixeldungeon.levels.objects;

import androidx.annotation.Keep;

import com.nyrds.Packable;
import com.nyrds.pixeldungeon.mechanics.CommonActions;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.noosa.StringsManager;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Blindness;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.windows.WndMessage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mike on 01.07.2016.
 */
public class Sign extends LevelObject {

	private static final String TEXT = "text";

	@Packable
	private String text;

	@Keep
	public Sign(){
		super(Level.INVALID_CELL);
	}

	public Sign(int pos, String text) {
		super(pos);
		this.text = text;
	}

	@Override
	void setupFromJson(Level level, JSONObject obj) throws JSONException {
		text = StringsManager.maybeId(obj.getString(TEXT));
	}

	@Override
	public boolean interact(Char hero) {
		if(!hero.getHeroClass().forbidden(CommonActions.AC_READ)) {
			if (hero.hasBuff(Blindness.class )) {
				GLog.w(Game.getVar(R.string.Codex_Blinded));
			} else {
				GameScene.show(new WndMessage(text));
			}
		}
		return super.interact(hero);
	}

	@Override
	public void burn() {
		remove();
		level().set(getPos(),Terrain.EMBERS);
		GameScene.discoverTile(getPos());
	}

	@Override
	public String desc() {
		return level().tileDesc(Terrain.SIGN);
	}

	@Override
	public String name() {
		return level().tileName(Terrain.SIGN);
	}

	@Override
	public int image() { //TODO use variety
		return 16;
	}
}
