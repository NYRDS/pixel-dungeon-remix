package com.nyrds.android.util;

import com.nyrds.pixeldungeon.ml.EventCollector;
import com.watabou.pixeldungeon.utils.GLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzip {

	private static final int    BUFFER_SIZE = 4096;

	private static void ensureDir(String dir) {
		File f = new File(dir);

		if (!f.exists() || !f.isDirectory()) {
			f.mkdirs();
		}
	}

	static public boolean unzip(InputStream fin, String tgtDir) {
		ensureDir(tgtDir);
		try {
			ZipInputStream zin = new ZipInputStream(fin);
			ZipEntry ze;

			byte data[] = new byte[BUFFER_SIZE];

			while ((ze = zin.getNextEntry()) != null) {
				GLog.debug( "Unzipping " + ze.getName());

				if (ze.isDirectory()) {
					ensureDir(tgtDir + "/" + ze.getName());
				} else {

					FileOutputStream fout = new FileOutputStream(tgtDir + "/" + ze.getName());

					int bytesRead;
					while ((bytesRead = zin.read(data)) != -1) {
						fout.write(data, 0, bytesRead);
					}

					zin.closeEntry();
					fout.close();
				}
			}
			zin.close();
		} catch (Exception e) {
			EventCollector.logException(e);
			return false;
		}

		return true;
	}

	static public boolean unzip(String zipFile, String tgtDir) {
		try {
			return unzip(new FileInputStream(zipFile), tgtDir);
		} catch (FileNotFoundException e) {
			return false;
		}
	}
}