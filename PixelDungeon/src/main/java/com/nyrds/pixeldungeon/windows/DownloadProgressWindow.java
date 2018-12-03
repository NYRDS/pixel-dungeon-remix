package com.nyrds.pixeldungeon.windows;

import com.nyrds.android.util.DownloadStateListener;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndMessage;

import androidx.annotation.NonNull;

/**
 * Created by mike on 14.04.2018.
 * This file is part of Remixed Pixel Dungeon.
 */
public class DownloadProgressWindow implements DownloadStateListener {
    private WndMessage        progress;
    private String            prefix;
    private IDownloadComplete onComplete;

    public DownloadProgressWindow(@NonNull String prefix, @NonNull IDownloadComplete onComplete) {
        this.prefix = prefix;
        this.onComplete = onComplete;
    }

    @Override
    public void DownloadProgress(final String file, final Integer bytes) {
        Game.pushUiTask(new Runnable() {

            @Override
            public void run() {
                if (progress == null) {
                    progress = new WndMessage("");
                    Game.scene().add(progress);
                }
                if (progress.getParent() == Game.scene()) {
                    progress.setText(Utils.format("%s  %4.2fMb", prefix, bytes / 1024f / 1024f));
                }
            }
        });
    }

    @Override
    public void DownloadComplete(final String file, final Boolean result) {
        Game.pushUiTask(new Runnable() {

            @Override
            public void run() {
                if (progress != null) {
                    progress.hide();
                    progress = null;
                }

                onComplete.DownloadComplete(file, result);
            }
        });
    }
}
