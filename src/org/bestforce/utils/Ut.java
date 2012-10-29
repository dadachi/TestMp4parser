package org.bestforce.utils;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

//public class Ut implements OpenStreetMapConstants, OpenStreetMapViewConstants {
public class Ut {

	private static File getDir(final Context mCtx, final String aPref, final String aDefaultDirName, final String aFolderName) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCtx);
		final String dirName = pref.getString(aPref, aDefaultDirName)+"/"+aFolderName+"/";

		final File dir = new File(dirName.replace("//", "/").replace("//", "/"));
		if(!dir.exists()){
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
				dir.mkdirs();
			}
		}

		return dir;
	}


	public static File getTestMp4ParserVideosDir(final Context mCtx) {
		return getDir(mCtx, "pref_dir_videos", "/sdcard/TestMp4Parser/Videos/", "");
	}
	
}
