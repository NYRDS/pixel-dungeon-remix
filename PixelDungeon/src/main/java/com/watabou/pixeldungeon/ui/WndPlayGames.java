package com.watabou.pixeldungeon.ui;

import com.nyrds.android.util.GuiProperties;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.support.Google.PlayGames;
import com.watabou.noosa.Game;
import com.watabou.noosa.Text;
import com.watabou.pixeldungeon.Preferences;
import com.watabou.pixeldungeon.scenes.PixelScene;
import com.watabou.pixeldungeon.windows.WndMessage;

/**
 * Created by mike on 14.05.2017.
 * This file is part of Remixed Pixel Dungeon.
 */

class WndPlayGames extends Window {

    private int y = GAP;

    public WndPlayGames() {

        boolean playGamesConnected = Game.instance().playGames.isConnected();
        resizeLimited(120);

        Text listTitle = PixelScene.createMultiline(Game.getVar(R.string.WndPlayGames_Title), GuiProperties.mediumTitleFontSize());
        listTitle.hardlight(TITLE_COLOR);
        listTitle.maxWidth(width - GAP * 2);
        listTitle.x = (width - listTitle.width()) / 2;
        listTitle.y = y;

        add(listTitle);

        y += listTitle.height() + GAP;

        CheckBox usePlayGames = new CheckBox(Game.getVar(R.string.WndPlayGames_Use), Preferences.INSTANCE.getBoolean(Preferences.KEY_USE_PLAY_GAMES, false)) {
            @Override
            public void checked(boolean value) {
                super.checked(value);

                if (value) {
                    Game.instance().playGames.connect();
                    Game.scene().add(new WndMessage(Game.getVar(R.string.WndPlayGames_Connecting)));
                } else {
                    Game.instance().playGames.disconnect();
                }

                hide();
            }
        };

        addButton(usePlayGames);

        if (!playGamesConnected) {
            resize(width, y);
            return;
        }

        addButton(new RedButton(Game.getVar(R.string.WndPlayGames_Show_Badges)) {
            @Override
            protected void onClick() {
                super.onClick();
                Game.instance().playGames.showBadges();
            }
        });

        addButton(new RedButton(Game.getVar(R.string.WndPlayGames_Show_Leaderboards)) {
            @Override
            protected void onClick() {
                super.onClick();
                Game.instance().playGames.showLeaderboard();
            }
        });

        addButton(new RedButton(Game.getVar(R.string.WndPlayGames_BackupProgress)) {
            @Override
            protected void onClick() {
                super.onClick();
                Game.instance().playGames.backupProgress(new ResultHandler());
            }
        });

        addButton(new RedButton(Game.getVar(R.string.WndPlayGames_RestoreProgress)) {
            @Override
            protected void onClick() {
                super.onClick();
                Game.instance().playGames.restoreProgress(new ResultHandler());
            }
        });

        resize(width, y);
    }

    private void addButton(TextButton btn) {
        btn.setRect(0, y, width, BUTTON_HEIGHT);
        add(btn);
        y += btn.height() + GAP;
    }

    private static class ResultHandler implements PlayGames.IResult {

        private WndMessage working;
        ResultHandler() {
            working = new WndMessage(Game.getVar(R.string.WndPlayGames_WorkInCloud));
            Game.scene().add(working);
        }
        @Override
        public void status(final boolean status) {
            Game.pushUiTask(new Runnable() {
                                       @Override
                                       public void run() {
                                           working.hide();
                                           String msg = Game.getVar(status ? R.string.WndPlayGames_Show_Ok : R.string.WndPlayGames_Show_Error );
                                           Game.scene().add(new WndMessage(msg));
                                       }
                                   }
            );
        }
    }
}
