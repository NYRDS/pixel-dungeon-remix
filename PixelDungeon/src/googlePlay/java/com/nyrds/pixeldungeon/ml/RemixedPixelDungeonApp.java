package com.nyrds.pixeldungeon.ml;

import android.annotation.SuppressLint;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.FirebaseApp;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import io.fabric.sdk.android.Fabric;

public class RemixedPixelDungeonApp extends MultiDexApplication {

	@SuppressLint("StaticFieldLeak")
	static Context instanceContext;

	@Override
	public void onCreate() {
		super.onCreate();

		instanceContext = getApplicationContext();
		FirebaseApp.initializeApp(this);

		Fabric.with(this, new Crashlytics());

		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable ignore) {
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	static public Context getContext() {
		return instanceContext;
	}
}
