package com.nyrds.pixeldungeon.support;

import android.annotation.SuppressLint;
import android.graphics.Color;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;

public class AdMobBannerProvider implements  AdsUtilsCommon.IBannerProvider {
    private AdView adView;

    AdMobBannerProvider(){ }

    @SuppressLint("MissingPermission")
    @Override
    public void displayBanner() {
        adView = new AdView(Game.instance());
        adView.setAdUnitId(Game.getVar(R.string.easyModeAdUnitId));
        adView.setBackgroundColor(Color.TRANSPARENT);
        adView.setAdListener(new AdmobBannerListener());
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.loadAd(AdMob.makeAdRequest());
    }

    @Override
    public boolean isReady() {
        return true;
    }

    private class AdmobBannerListener extends AdListener {

        @Override
        public void onAdLoaded() {
            Ads.updateBanner(adView);
        }

        @Override
        public void onAdFailedToLoad(LoadAdError reason) {
            AdsUtilsCommon.bannerFailed(AdMobBannerProvider.this);
        }
    }
}
