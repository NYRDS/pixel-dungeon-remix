package com.nyrds.android.util;

import android.content.Context;

import com.nyrds.pixeldungeon.ml.RemixedPixelDungeonApp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.annotation.NonNull;

public class FileSystem {

	static public File getInternalStorageFile(String fileName) {
		File storageDir = getContext().getFilesDir();
		return new File(storageDir, fileName);
	}

	static public String[] listInternalStorage() {
		File storageDir = getContext().getFilesDir();
		return storageDir.list();
	}

	@NonNull
	static public File[] listExternalStorage() {

		File storageDir = getContext().getExternalFilesDir(null);
		if (storageDir != null) {
			File[] ret = storageDir.listFiles();
			if(ret != null) {
				return ret;
			}
		}

		return new File[0];
	}

	static public OutputStream getOutputStream(String filename) throws FileNotFoundException {
		File dir = new File(filename).getParentFile();
		if (dir != null && !dir.exists()) {
			dir.mkdirs();
		}

		return new FileOutputStream(FileSystem.getInternalStorageFile(filename));
	}

	static public InputStream getInputStream(String filename) throws FileNotFoundException {
		return new FileInputStream(FileSystem.getInternalStorageFile(filename));
	}

	static public String getInternalStorageFileName(String fileName) {
		return getInternalStorageFile(fileName).getAbsolutePath();
	}

	static public File getExternalStorageFile(String fileName) {
		File storageDir = getContext().getExternalFilesDir(null);
		return new File(storageDir, fileName);
	}

	static public String getExternalStorageFileName(String fname) {
		return getExternalStorageFile(fname).getAbsolutePath();
	}

	static public File getFile(String fname) {
		return getInternalStorageFile(fname);
	}

	static public void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}

	static public void copyStream(InputStream in, OutputStream out) {
		try {
			byte[] buffer = new byte[4096];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();

			out.flush();
			out.close();

		} catch (Exception e) {
			throw new TrackedRuntimeException(e);
		}
	}

	static public void copyFile(String inputFile, OutputStream out) {
		try {
			InputStream in = new FileInputStream(inputFile);

			copyStream(in, out);
		} catch (Exception e) {
			throw new TrackedRuntimeException(e);
		}
	}

	static public void copyFile(String inputFile, String outputFile) {
		try {
			File dir = new File(outputFile).getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}

			copyFile(inputFile, new FileOutputStream(outputFile));
		} catch (Exception e) {
			throw new TrackedRuntimeException(e);
		}
	}

	public static void zipFolderTo(OutputStream out, File srcFolder, int depth, FileFilter filter) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(out);
		addFolderToZip(srcFolder,srcFolder,depth, zip, filter);

		zip.flush();
		zip.close();
	}

	private static void addFolderToZip(File rootFolder, File srcFolder, int depth,
	                                   ZipOutputStream zip, FileFilter filter) throws IOException {

		for (File file : srcFolder.listFiles(filter)) {

			if (file.isFile()) {
				addFileToZip(rootFolder, file, zip);
				continue;
			}

			if(depth > 0 && file.isDirectory()) {
				zip.putNextEntry(new ZipEntry(getRelativePath(file,rootFolder)));
				addFolderToZip(rootFolder, srcFolder, depth-1, zip, filter);
				zip.closeEntry();
			}
		}
	}

	private static void addFileToZip(File rootFolder, File file, ZipOutputStream zip) throws IOException {
			byte[] buf = new byte[4096];
			int len;
			FileInputStream in = new FileInputStream(file);
			zip.putNextEntry(new ZipEntry(getRelativePath(file,rootFolder)));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
			zip.closeEntry();
		}

	public static String getRelativePath(File file, File folder) {
		String filePath = file.getAbsolutePath();
		String folderPath = folder.getAbsolutePath();
		if (filePath.startsWith(folderPath)) {
			return filePath.substring(folderPath.length() + 1);
		} else {
			return null;
		}
	}

	private static Context getContext() {
		return RemixedPixelDungeonApp.getContext();
	}
}
