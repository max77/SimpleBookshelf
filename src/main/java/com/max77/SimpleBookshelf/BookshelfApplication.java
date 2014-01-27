package com.max77.SimpleBookshelf;

import android.app.Application;

/**
 * Расширение стандартного Application для хранения DownloadEngine
 */
public class BookshelfApplication extends Application {
	private DownloadEngine mDownloadEngine;

	@Override
	public void onCreate() {
		super.onCreate();

		mDownloadEngine = new DownloadEngine(this);
		mDownloadEngine.restoreDownloads();
	}

	public DownloadEngine getDownloadEngine() {
		return mDownloadEngine;
	}
}
