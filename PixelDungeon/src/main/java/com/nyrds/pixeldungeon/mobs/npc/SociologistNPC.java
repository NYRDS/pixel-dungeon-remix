package com.nyrds.pixeldungeon.mobs.npc;

import android.Manifest;

import com.nyrds.android.util.DownloadStateListener;
import com.nyrds.android.util.DownloadTask;
import com.nyrds.android.util.FileSystem;
import com.nyrds.android.util.JsonHelper;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.windows.DownloadProgressWindow;
import com.nyrds.pixeldungeon.windows.WndSurvey;
import com.watabou.noosa.Game;
import com.watabou.noosa.InterstitialPoint;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.windows.WndError;
import com.watabou.pixeldungeon.windows.WndOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import androidx.annotation.Nullable;

/**
 * Created by mike on 09.03.2018.
 * This file is part of Remixed Pixel Dungeon.
 */

public class SociologistNPC extends ImmortalNPC implements DownloadStateListener.IDownloadComplete, InterstitialPoint {

    private static final String SURVEY_JSON = "survey.json";

    @Nullable
    private JSONObject survey;

    public boolean interact(Hero hero) {

        Game.scene().add(new WndOptions(this.name,
                Game.getVar(R.string.SociologistNPC_Hi),
                Game.getVar(R.string.Wnd_Button_Yes), Game.getVar(R.string.Wnd_Button_No)
        ) {

            @Override
            protected void onSelect(int index) {
                if (index == 0) {
                    String[] requiredPermissions = {Manifest.permission.INTERNET};
                    Game.instance().doPermissionsRequest(SociologistNPC.this, requiredPermissions);
                }
            }
        });
        return true;
    }


    @Override
    public void DownloadComplete(String file, final Boolean result) {
        Game.pushUiTask(new Runnable() {
            @Override
            public void run() {
                if (!result) {
                    reportError();
                } else {
                    try {
                        survey = JsonHelper.readJsonFromFile(FileSystem.getInternalStorageFile(SURVEY_JSON));

                        Game.scene().add(new WndSurvey(survey));

                    } catch (JSONException e) {
                        reportError();
                    }
                }
            }
        });
    }

    private void reportError() {
        Game.scene().add(new WndError(Game.getVar(R.string.SociologistNPC_DownloadError)));
    }

    @Override
    public void returnToWork(boolean result) {
        if (result) {
            File survey = FileSystem.getInternalStorageFile(SURVEY_JSON);
            survey.delete();
            String downloadTo = survey.getAbsolutePath();

            new DownloadTask(new DownloadProgressWindow("Downloading", this)).download("https://github.com/NYRDS/pixel-dungeon-remix-survey/raw/master/survey.json", downloadTo);
        } else {
            say(Game.getVar(R.string.SociologistNPC_InternetRequired));
        }
    }
}
