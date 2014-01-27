package com.max77.SimpleBookshelf;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import org.apache.commons.lang3.ArrayUtils;
import roboguice.util.temp.Ln;

import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный класс, "обертка" вокруг менеджера загрузок
 */
public class DownloadEngine {

	private static final String PREFS = "downloadengineprefs";
	private Context mContext;
	private DownloadManager mDownloadManager;
	private Map<String, Long> mUrlToDownloadIdMap;
	private Map<Long, DownloadInfo> mDownloadIdToDownloadInfoMap;

	public DownloadEngine(Context context) {
		mContext = context;
		mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
		mUrlToDownloadIdMap = new HashMap<String, Long>();
		mDownloadIdToDownloadInfoMap = new HashMap<Long, DownloadInfo>();
	}

	/**
	 * Сохранение информации о загрузках в настройки
	 */
	public void saveDownloads() {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		for (String key : mUrlToDownloadIdMap.keySet())
			editor.putLong(key, mUrlToDownloadIdMap.get(key));

		editor.commit();
	}

	/**
	 * Восстановление информации о загрузках из настроек
	 *
	 * @return Количество загруженных настроек
	 */
	public int restoreDownloads() {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		Map<String, ?> map = prefs.getAll();

		mUrlToDownloadIdMap = new HashMap<String, Long>();
		for (String url : map.keySet())
			mUrlToDownloadIdMap.put(url, (Long) map.get(url));

		return mUrlToDownloadIdMap.size();
	}

	/**
	 * Добавление URL в очередь на загрузку
	 *
	 * @param url HTTP URL загружаемого файла
	 */
	public void addDownload(String url) {
		long id = mDownloadManager.enqueue(new DownloadManager.Request(Uri.parse(url)));
		mUrlToDownloadIdMap.put(url, id);
		mDownloadIdToDownloadInfoMap.put(id, new DownloadInfo(DownloadManager.STATUS_PENDING, 0, -1, null));
	}

	/**
	 * Обновление информации о текущих загрузках c заданными id
	 *
	 * @param ids Массив идентификаторов
	 */
	private void updateDownloadStates(long... ids) {
		// Создаем запрос с фильтром по ID ранее добавленных загрузок
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(ids);

		try {
			// Обновляем информацию
			Cursor c = mDownloadManager.query(query);
			int idIdx = c.getColumnIndex(DownloadManager.COLUMN_ID);
			int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int sizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
			int currSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
			int pathIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);

			while (c.moveToNext()) {
				long id = c.getLong(idIdx);
				int status = c.getInt(statusIdx);
				long size = c.getLong(sizeIdx);
				long currSize = c.getLong(currSizeIdx);

				String path = (status == DownloadManager.STATUS_SUCCESSFUL ? c.getString(pathIdx) : null);
				mDownloadIdToDownloadInfoMap.put(id, new DownloadInfo(status, size, currSize, path));
			}

			c.close();
		} catch (Exception e) {
			Ln.e(e);
		}
	}

	/**
	 * Обновление информации о текущих загрузках
	 *
	 * @param url HTTP URL загружаемого файла или null для обновления ВСЕХ загрузок
	 */
	public void updateDownloadState(String url) {
		long[] ids;

		if (url != null)
			ids = new long[]{mUrlToDownloadIdMap.get(url)};
		else
			ids = ArrayUtils.toPrimitive(mUrlToDownloadIdMap.values().toArray(new Long[0]));

		updateDownloadStates(ids);
	}

	/**
	 * Получение информации о загрузке
	 *
	 * @param url HTTP URL HTTP URL загружаемого файла
	 * @return {@link com.max77.SimpleBookshelf.DownloadEngine.DownloadInfo}
	 */
	public DownloadInfo getDownloadInfo(String url) {
		Long id = mUrlToDownloadIdMap.get(url);

		return id != null ? mDownloadIdToDownloadInfoMap.get(id) : null;
	}

	/**
	 * Проверка на наличие выполняющихся загрузок
	 *
	 * @return true - есть загрузки в процессе, false - нет
	 */
	public boolean hasRunningDownloads() {
		for (DownloadInfo info : mDownloadIdToDownloadInfoMap.values())
			if (info.getStatus() == DownloadManager.STATUS_RUNNING || info.getStatus() == DownloadManager.STATUS_PENDING)
				return true;

		return false;
	}

	/**
	 * Удаление загрузки
	 *
	 * @param url HTTP URL загружаемого файла
	 * @return true, если все ОК, иначе - false
	 */
	public boolean removeDownload(String url) {
		Long id = mUrlToDownloadIdMap.remove(url);

		if (id != null) {
			mDownloadIdToDownloadInfoMap.remove(id);

			return mDownloadManager.remove(id) == 1;
		}

		return false;
	}

	/**
	 * Интерфейс для уведомления об обновлениях состояния загрузок
	 */
	public interface OnDownloadsUpdatedListener {
		void onDownloadsUpdated();
	}

	/**
	 * Информация о загружаемом файле
	 */
	public static class DownloadInfo {
		private int mStatus;
		private long mSize;
		private long mCurrentSize;
		private String mSavedFilePath;

		DownloadInfo(int status, long size, long currentSize, String savedFilePath) {
			mStatus = status;
			mSize = size;
			mCurrentSize = currentSize;
			mSavedFilePath = savedFilePath;
		}

		public int getStatus() {
			return mStatus;
		}

		public long getSize() {
			return mSize;
		}

		public long getCurrentSize() {
			return mCurrentSize;
		}

		public String getSavedFilePath() {
			return mSavedFilePath;
		}
	}
}
