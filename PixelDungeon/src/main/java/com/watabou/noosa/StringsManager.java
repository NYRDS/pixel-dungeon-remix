package com.watabou.noosa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import com.nyrds.android.util.ModdingMode;
import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.ml.BuildConfig;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.ml.RemixedPixelDungeonApp;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

/**
 * Created by mike on 08.03.2016.
 */
public class StringsManager {

	@SuppressLint("UseSparseArrays")
	@NonNull
	private static Map<Integer, String>   stringMap  = new HashMap<>();
	@SuppressLint("UseSparseArrays")
	@NonNull
	private static Map<Integer, String[]> stringsMap = new HashMap<>();

	private static Map<String, String>   sStringMap  = new HashMap<>();
	private static Map<String, String[]> sStringsMap = new HashMap<>();

	private static Map<String, Integer> keyToInt = new HashMap<>();;

	private static Set<String> nonModdable = new HashSet<>();

	static {
		addMappingForClass(R.string.class);
		addMappingForClass(R.array.class);

		nonModdable.add("easyModeAdUnitId");
		nonModdable.add("saveLoadAdUnitId");
		nonModdable.add("easyModeSmallScreenAdUnitId");
		nonModdable.add("iapKey");
		nonModdable.add("ownSignature");
		nonModdable.add("appodealRewardAdUnitId");
		nonModdable.add("admob_publisher_id");
		nonModdable.add("admob_app_id");
		nonModdable.add("fabric_api_key");
	}

	private static void addMappingForClass(Class<?> clazz) {
		for (Field f : clazz.getDeclaredFields()) {
			if (f.isSynthetic()) {
				continue;
			}
			int key;
			try {
				key = f.getInt(null);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new TrackedRuntimeException(e);
			}
			String name = f.getName();

			keyToInt.put(name, key);
		}
	}

	private static void clearModStrings() {
		stringMap.clear();
		stringsMap.clear();

		sStringMap.clear();
		sStringsMap.clear();
	}

	private static void parseStrings(String resource) {
		File jsonFile = ModdingMode.getFile(resource);
		if (jsonFile == null || !jsonFile.exists()) {
			return;
		}

		String line = "";

		try {
			InputStream fis = new FileInputStream(jsonFile);
			InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(isr);

			while ((line = br.readLine()) != null) {
				JSONArray entry = new JSONArray(line);

				String keyString = entry.getString(0);
				Integer key = keyToInt.get(keyString);

				if (entry.length() == 2) {
					String value = entry.getString(1);

					if (key != null) {
						stringMap.put(key, value);
					}

					sStringMap.put(keyString, value);
				}

				if (entry.length() > 2) {
					String[] values = new String[entry.length() - 1];
					for (int i = 1; i < entry.length(); i++) {
						values[i - 1] = entry.getString(i);
					}

					if (key != null) {
						stringsMap.put(key, values);
					}

					sStringsMap.put(keyString, values);
				}
			}
			br.close();
		} catch (IOException e) {
			throw new TrackedRuntimeException(e);
		} catch (JSONException e) {
			Game.toast("malformed json: [%s] in [%s] ignored ", line, resource);
		}
	}

	private static Locale userSelectedLocale;

	private static void ensureCorrectLocale() {
		if(userSelectedLocale==null) {
			return;
		}

		Configuration config = getContext().getResources().getConfiguration();

		if(!getContext().getResources().getConfiguration().locale.equals(userSelectedLocale)) {
			if(BuildConfig.DEBUG){
				GLog.i("Locale is messed up! Restoring");
			}
			config.locale = userSelectedLocale;
			getContext().getResources().updateConfiguration(config,
					getContext().getResources().getDisplayMetrics());
		}
	}

	public static void useLocale(Locale locale, String lang) {
		userSelectedLocale = locale;

		Configuration config = getContext().getResources().getConfiguration();

		GLog.i("context locale: %s -> %s", config.locale, locale);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			config.locale = locale;
		} else {
			config.setLocale(locale);
		}
		getContext().getResources().updateConfiguration(config,
				getContext().getResources().getDisplayMetrics());

		clearModStrings();

		String modStrings = Utils.format("strings_%s.json", lang);

		if (ModdingMode.isResourceExistInMod(modStrings)) {
			parseStrings(modStrings);
		} else if (ModdingMode.isResourceExistInMod("strings_en.json")) {
			parseStrings("strings_en.json");
		}

	}

	public static String getVar(int id) {
		if (stringMap.containsKey(id)) {
			return stringMap.get(id);
		}

		try {
			ensureCorrectLocale();
			return getContext().getResources().getString(id);
		} catch (Resources.NotFoundException notFound) {
			GLog.w("resource not found: %s", notFound.getMessage());
		}
		return "";
	}

	public static String[] getVars(int id) {

		if (stringsMap.containsKey(id)) {
			return stringsMap.get(id);
		}
		ensureCorrectLocale();
		return getContext().getResources().getStringArray(id);
	}

	public static String getVar(String id) {
		if(nonModdable.contains(id)) {
			return "";
		}

		if (sStringMap.containsKey(id)) {
			return sStringMap.get(id);
		}

		if(keyToInt.containsKey(id)) {
			return getVar(keyToInt.get(id));
		}

		return "";
	}


	public static String maybeId(String maybeId, int index) {
		String[] ret = getVars(maybeId);
		if (ret.length > index) {
			return ret[index];
		}
		return Utils.format("%s[%d]",maybeId,index);
	}

	public static String maybeId(String maybeId) {

		String ret = getVar(maybeId);
		if (ret.isEmpty()) {
			return maybeId;
		}
		return ret;
	}

	public static String[] getVars(String id) {
		if (sStringsMap.containsKey(id)) {
			return sStringsMap.get(id);
		}

		if(keyToInt.containsKey(id)) {
			return getVars(keyToInt.get(id));
		}

		return new String[0];
	}

	public static Context getContext() {
		return RemixedPixelDungeonApp.getContext();
	}
}
