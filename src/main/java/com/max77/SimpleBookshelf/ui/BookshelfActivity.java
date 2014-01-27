package com.max77.SimpleBookshelf.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.max77.SimpleBookshelf.BookshelfApplication;
import com.max77.SimpleBookshelf.DownloadEngine;
import com.max77.SimpleBookshelf.R;
import com.max77.SimpleBookshelf.model.Bookshelf;
import com.max77.SimpleBookshelf.network.BookshelfRequest;
import com.octo.android.robospice.JacksonSpringAndroidSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.octo.android.robospice.spicelist.SpiceListView;
import com.octo.android.robospice.spicelist.okhttp.OkHttpBitmapSpiceManager;
import roboguice.util.temp.Ln;

/**
 * Экран, отображающий список книг
 */

public class BookshelfActivity extends Activity {
	private static final String KEY_FIRST_RUN = "firstRun12309198";
	private static final String CACHE_KEY_BOOKS = "books123912";
	private static final long UPDATE_PERIOD = 500;
	private SpiceListView lvBookshelf;
	private TextView tvMessage;
	private Button btnRetry;
	private ProgressBar pbLoading;
	// SpiceManager для загрузки/парсинга JSON-списка книг
	private SpiceManager mBookshelfSpiceManager = new SpiceManager(JacksonSpringAndroidSpiceService.class);
	// SpiceManager для загрузки иконок
	private OkHttpBitmapSpiceManager mIconSpiceManager = new OkHttpBitmapSpiceManager();
	// Флаг первого запуска или запуска после закрытия по Back
	private boolean isFirstRun = true;
	private DownloadEngine mDownloadEngine;
	// Handler для периодического обновления состояния загружаемых книг
	private Handler mPeriodicUpdateHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookshelf);

		if (savedInstanceState != null)
			isFirstRun = savedInstanceState.getBoolean(KEY_FIRST_RUN);

		lvBookshelf = (SpiceListView) findViewById(R.id.lvBookshelf);
		tvMessage = (TextView) findViewById(R.id.tvMessage);
		btnRetry = (Button) findViewById(R.id.btnRetry);
		pbLoading = (ProgressBar) findViewById(R.id.pbLoading);

		mDownloadEngine = ((BookshelfApplication) getApplication()).getDownloadEngine();
	}

	@Override
	protected void onStart() {
		super.onStart();

		mBookshelfSpiceManager.start(this);
		mIconSpiceManager.start(this);

		// Если запустились первый раз или после закрытия, принудительно чистим кэш
		if (isFirstRun)
			clearCache();
		else
			retrieveBooks();
	}

	@Override
	protected void onStop() {
		stopPeriodicUpdate();
		mBookshelfSpiceManager.shouldStop();
		mIconSpiceManager.shouldStop();
		mDownloadEngine.saveDownloads();

		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(KEY_FIRST_RUN, isFirstRun);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Получение списка книг. Результат кэшируется на 1 час.
	 */
	private void retrieveBooks() {
		showProgress();

		BookshelfRequest rq = new BookshelfRequest();
		mBookshelfSpiceManager.execute(rq, CACHE_KEY_BOOKS, DurationInMillis.ONE_HOUR, new BookshelfListener());
	}

	/**
	 * Очистка кэша JSON-списка книг. Картинки не трогаем.
	 */
	private void clearCache() {
		AsyncTask<Void, Void, Void> clearTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					mBookshelfSpiceManager.removeAllDataFromCache().get();
				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				retrieveBooks();
			}
		};

		clearTask.execute();
	}

	private void showMessage(String message) {
		if (message != null)
			tvMessage.setText(message);
		pbLoading.setVisibility(View.GONE);
		lvBookshelf.setVisibility(View.GONE);
		tvMessage.setVisibility(View.VISIBLE);
		btnRetry.setVisibility(View.VISIBLE);
	}

	private void showList(Bookshelf bookshelf) {
		pbLoading.setVisibility(View.VISIBLE);
		lvBookshelf.setVisibility(View.VISIBLE);
		tvMessage.setVisibility(View.GONE);
		btnRetry.setVisibility(View.GONE);

		BookshelfListAdapter adapter = new BookshelfListAdapter(this, mIconSpiceManager, bookshelf, mDownloadEngine);
		adapter.setItemActionListener(new BookItemActionListener());
		lvBookshelf.setAdapter(adapter);
	}

	private void showProgress() {
		pbLoading.setVisibility(View.VISIBLE);
		lvBookshelf.setVisibility(View.GONE);
		tvMessage.setVisibility(View.GONE);
		btnRetry.setVisibility(View.GONE);
	}

	/**
	 * Запускает периодическое обновление списка.
	 * Обновление прекращается принудительно (см. {@link #startPeriodicUpdate()} или когда не остается выполняющихся загрузок
	 */
	private void startPeriodicUpdate() {
		stopPeriodicUpdate();

		Ln.i("List update started");

		mPeriodicUpdateHandler = new Handler();
		mPeriodicUpdateHandler.post(new Runnable() {
			@Override
			public void run() {
				Ln.i("List update");

				mDownloadEngine.updateDownloadState(null);
				lvBookshelf.getAdapter().notifyDataSetChanged();

				if (mDownloadEngine.hasRunningDownloads())
					mPeriodicUpdateHandler.postDelayed(this, UPDATE_PERIOD);
				else
					Ln.i("List update stopped");
			}
		});
	}

	/**
	 * Останавливает периодическое обновление списка
	 */
	private void stopPeriodicUpdate() {
		if (mPeriodicUpdateHandler != null) {
			mPeriodicUpdateHandler.removeCallbacksAndMessages(null);
			Ln.i("List update stopped");
		}
	}

	private class BookshelfListener implements RequestListener<Bookshelf> {
		@Override
		public void onRequestFailure(SpiceException e) {
			showMessage(getString(R.string.error));
		}

		@Override
		public void onRequestSuccess(Bookshelf bookshelf) {
			showList(bookshelf);
			startPeriodicUpdate();
		}
	}

	private class BookItemActionListener implements BookView.ActionListener {
		@Override
		public void onDownloadBook(BookView view) {
			String url = view.getData().mBook.getBookFileUrl();
			mDownloadEngine.addDownload(url);
			mDownloadEngine.updateDownloadState(url);
			startPeriodicUpdate();
		}

		@Override
		public void onReadBook(BookView view) {
			String url = view.getData().mBook.getBookFileUrl();
			String path = mDownloadEngine.getDownloadInfo(url).getSavedFilePath();

			Intent intent = new Intent(BookshelfActivity.this, BookViewActivity.class);
			intent.putExtra(BookViewActivity.EXTRA_BOOK_PATH, path);
			startActivity(intent);
		}
	}
}
