package com.nyrds.android.util;

import com.nyrds.android.RemixedDungeonApp;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.watabou.pixeldungeon.RemixedDungeon;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.var;

public class ModdingMode {
	public static final String REMIXED = "Remixed";
	public static final String NO_FILE = "___no_file";

	private static final Set<String> trustedMods = new HashSet<>();

	public static boolean useRetroHeroSprites = false;

	private static final Set<String> pathsChecked = new HashSet<>();
	private static final Map<String, Boolean> assetsExistenceCache = new HashMap<>();

	private static final Map<String, String> resourcesRemap = new HashMap<>();

	static {
		trustedMods.add("Maze");
		trustedMods.add("Conundrum");

		resourcesRemap.put("spellsIcons/elemental(new).png", "spellsIcons/elemental_all.png");
	}

	@NotNull
	static private String mActiveMod = REMIXED;

	static private ArrayList<String> mActiveModsList;

	static private boolean mTextRenderingMode = false;

	public static void selectMod(String mod) {
		try {
			assetsExistenceCache.clear();

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
			return RemixedDungeon.versionCode;
		}

		JSONObject version = JsonHelper.tryReadJsonFromAssets("version.json");
		return version.optInt("version");
	}

	public static String activeMod() {
		return mActiveMod;
	}

	public static String getSoundById(String id) {

		String candidate = id + ".ogg";

		if(ModdingMode.isResourceExistInMod(candidate)) {
			return candidate;
		}

		candidate = id + ".mp3";

		if(ModdingMode.isResourceExistInMod(candidate)) {
			return candidate;
		}

		candidate = id + ".ogg";

		if(ModdingMode.isAssetExist(candidate)) {
			return candidate;
		}

		candidate = id + ".mp3";
		if(ModdingMode.isAssetExist(candidate)) {
			return candidate;
		}

		if(id.contains(".mp3")) {
			return getSoundById(id.replace(".mp3",""));
		}

		if(id.contains(".ogg")) {
			return getSoundById(id.replace(".ogg",""));
		}

		return Utils.EMPTY_STRING;
	}

	public static boolean isSoundExists(String id) {
		String resourceId = "sound/"+id;
		String foundId = getSoundById(resourceId);
		GLog.debug("sound: %s -> %s", id, foundId);
		return !foundId.isEmpty();
	}

	public static boolean isAssetExist(String resName) {
		Boolean isExist = assetsExistenceCache.get(resName);

		if(isExist != null) {
			return isExist;
		}

		InputStream str;
		try {
			str = RemixedDungeonApp.getContext().getAssets().open(resName);
			str.close();
			assetsExistenceCache.put(resName, true);
			return true;
		} catch (IOException e) {
			assetsExistenceCache.put(resName, false);
			return false;
		}
	}

	public static boolean inMod() {
		return !mActiveMod.equals(REMIXED);
	}

	public static boolean isResourceExists(String resName) {
		return isAssetExist(resName) || isResourceExistInMod(resName);
	}

	public static boolean isResourceExistInMod(String resName) {
		if (!mActiveMod.equals(REMIXED)) {
			return FileSystem.getExternalStorageFile(mActiveMod + "/" + resName).exists();
		}
		return false;
	}

	@NotNull
	public static List<String> listResources(String path, FilenameFilter filter) {
        pathsChecked.clear();

	    var list = _listResources(path, filter);

		for (int i = 0;i<list.size();++i) {
			list.set(i,list.get(i).replaceFirst(path+"/",""));
		}

		return list;
	}

	@SneakyThrows
	@NotNull
	private static List<String> _listResources(String path, FilenameFilter filter) {
		if(pathsChecked.contains(path)) {
			return new ArrayList<>();
		}

		pathsChecked.add(path);

		Set<String> resList = new HashSet<>();

		String[] fullList = RemixedDungeonApp.getContext().getAssets().list(path);
		collectResources(path, filter, resList, fullList);

		if(inMod()) {
			String resourcesPath = mActiveMod + "/" + path;
			if(isResourceExistInMod(path)) {
				String[] modList = FileSystem.getExternalStorageFile(resourcesPath).list();
				collectResources(path, filter, resList, modList);
			}
		}

		return Arrays.asList(resList.toArray(new String[0]));
	}

	private static void collectResources(String path, FilenameFilter filter, Set<String> resList, String[] fullList) {
		if(fullList==null) {
			return;
		}
		for(String resource : fullList) {
			if(filter.accept(null, resource)) {
				resList.add(path+"/"+resource);
			} else {
				resList.addAll(_listResources(path+"/"+resource,filter));
			}
		}
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

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(resName)))) {
			String line = reader.readLine();

			while (line != null) {
				resource.append(line);
				line = reader.readLine();
			}
		} catch (IOException e) {
			EventCollector.logException(e, resName);
		}

		return resource.toString();
	}

	public static @NotNull InputStream getInputStream(String resName) {
		try {
			if (!mActiveMod.equals(REMIXED) && isModdingAllowed(resName)) {
				File file = FileSystem.getExternalStorageFile(mActiveMod + "/" + resName);
				if (file.exists()) {
					return new FileInputStream(file);
				}
			}

			if(resourcesRemap.containsKey(resName)) {
				resName = resourcesRemap.get(resName);
			}

			return RemixedDungeonApp.getContext().getAssets().open(resName);
		} catch (IOException e) {
			throw new ModError("Missing file: "+resName + " in: " + activeMod() + " " + activeModVersion(),e);
		}
	}

	private static boolean isModdingAllowed(@NotNull String resName) {

		if(resName.startsWith("scripts/services")) {
			return false;
		}

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

	public static RuntimeException modException(Exception e) {
		return new ModError(mActiveMod,e);
	}

	public static RuntimeException modException(String s, JSONException e) {
		return new ModError(mActiveMod + ":" + s, e);
	}
}
