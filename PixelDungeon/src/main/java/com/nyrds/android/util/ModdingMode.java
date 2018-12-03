package com.nyrds.android.util;

import android.content.Context;

import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.RemixedPixelDungeonApp;
import com.watabou.pixeldungeon.PixelDungeon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

public class ModdingMode {
	public static final String REMIXED = "Remixed";

	private static final Set<String> trustedMods = new HashSet<>();

	public static boolean useRetroHeroSprites = false;

	static {
		trustedMods.add("Maze");
		trustedMods.add("Conundrum");
	}

	@NonNull
	static private String mActiveMod = REMIXED;

	static private boolean mTextRenderingMode = false;

	public static void selectMod(String mod) {
		try {
			File modPath = FileSystem.getExternalStorageFile(mod);
			if ((modPath.exists() && modPath.isDirectory()) || mod.equals(ModdingMode.REMIXED)) {
				mActiveMod = mod;
			}

			if(!mod.equals(ModdingMode.REMIXED)) {
				useRetroHeroSprites = !isResourceExistInMod("hero_modern");
			}
		} catch (Exception e) {
			EventCollector.logException(e);
			mActiveMod = ModdingMode.REMIXED;
		}
	}

	public static int activeModVersion() {
		if (mActiveMod.equals(ModdingMode.REMIXED)) {
			return PixelDungeon.versionCode;
		}

		JSONObject version = JsonHelper.tryReadJsonFromAssets("version.json");
		return version.optInt("version");
	}

	public static boolean useRetroHero() {
		if (mActiveMod.equals(ModdingMode.REMIXED)) {
			return false;
		}

		JSONObject version = JsonHelper.tryReadJsonFromAssets("version.json");
		return !version.optBoolean("modernHeroSprites", true);
	}

	public static String activeMod() {
		return mActiveMod;
	}

	public static boolean isAssetExist(String resName) {
		InputStream str;
		try {
			str = getContext().getAssets().open(resName);
			str.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean inMod() {
		return !mActiveMod.equals(REMIXED);
	}

	public static boolean isResourceExistInMod(String resName) {
		if (!mActiveMod.equals(REMIXED)) {
			return FileSystem.getExternalStorageFile(mActiveMod + "/" + resName).exists();
		}
		return false;
	}

	public static boolean isResourceExist(String resName) {
		if (isResourceExistInMod(resName)) {
			return true;
		} else {
			return isAssetExist(resName);
		}
	}

	public static File getFile(String resName) {
		if (!mActiveMod.equals(REMIXED)) {
			return FileSystem.getExternalStorageFile(mActiveMod + "/" + resName);
		}
		return null;
	}

	public static String getResource(String resName) {

		StringBuilder resource = new StringBuilder();

		BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(resName)));

		try {
			String line = reader.readLine();

			while (line != null) {
				resource.append(line);
				line = reader.readLine();
			}
			reader.close();

		} catch (IOException e) {
			EventCollector.logException(e, resName);
		}

		return resource.toString();
	}

	public static InputStream getInputStream(String resName) {
		try {
			if (!mActiveMod.equals(REMIXED) && isModdingAllowed(resName)) {
				File file = FileSystem.getExternalStorageFile(mActiveMod + "/" + resName);
				if (file.exists()) {
					return new FileInputStream(file);
				}
			}
			return getContext().getAssets().open(resName);
		} catch (IOException e) {
			throw new TrackedRuntimeException(e);
		}
	}

	private static boolean isModdingAllowed(String resName) {
		return trustedMod() || !(resName.contains("accessories") || resName.contains("banners"));
	}

	private static boolean trustedMod() {
		return trustedMods.contains(mActiveMod);
	}

	public static void setClassicTextRenderingMode(boolean val) {
		mTextRenderingMode = val;
	}

	public static boolean getClassicTextRenderingMode() {
		return mTextRenderingMode;
	}

	public static boolean isHalloweenEvent() {

		Calendar now = new GregorianCalendar();
		Calendar halloween = new GregorianCalendar();
		halloween.set(Calendar.MONTH, Calendar.OCTOBER);
		halloween.set(Calendar.DAY_OF_MONTH, 31);

		long milisPerDay = (1000 * 60 * 60 * 24);

		long nowMilis = now.getTimeInMillis() / milisPerDay;
		long hallMilis = halloween.getTimeInMillis() / milisPerDay;

		long daysDiff;

		if (nowMilis > hallMilis) {
			daysDiff = (nowMilis - hallMilis);
		} else {
			daysDiff = (hallMilis - nowMilis);
		}

		return daysDiff < 14;
	}

	private static Context getContext() {
		return RemixedPixelDungeonApp.getContext();
	}

	public static RuntimeException modException(Exception e) {
		if(inMod()) {
			return new ModError(mActiveMod,e);
		}
		return new TrackedRuntimeException(e);
	}

	public static RuntimeException modException(String s, JSONException e) {
		if(inMod()) {
			return new ModError(mActiveMod + ":" + s, e);
		}
		return new TrackedRuntimeException(s,e);
	}
}
