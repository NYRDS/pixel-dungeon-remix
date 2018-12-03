package com.nyrds.android.lua;

import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.ml.RemixedPixelDungeonApp;

import org.luaj.vm2.lib.jse.LuajavaLib;

/**
 * Created by mike on 01.11.2017.
 * This file is part of Remixed Pixel Dungeon.
 */
public class MultiDexLuajavaLib extends LuajavaLib {

	public MultiDexLuajavaLib() {
		super();
	}

	@Override
	protected Class classForName(String name) {
		ClassLoader classLoader = RemixedPixelDungeonApp.getContext().getClassLoader();

		try {

			Class clazz = Class.forName(name, true, classLoader);
			return clazz;
		} catch (ClassNotFoundException e) {
			throw new TrackedRuntimeException(classLoader.toString(), e);
		}
	}
}
