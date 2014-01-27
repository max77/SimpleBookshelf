package com.max77.SimpleBookshelf.ui;

import android.content.Context;
import android.view.ViewGroup;
import com.max77.SimpleBookshelf.DownloadEngine;
import com.max77.SimpleBookshelf.model.Book;
import com.max77.SimpleBookshelf.model.Bookshelf;
import com.octo.android.robospice.request.okhttp.simple.OkHttpBitmapRequest;
import com.octo.android.robospice.spicelist.SpiceListItemView;
import com.octo.android.robospice.spicelist.okhttp.OkHttpBitmapSpiceManager;
import com.octo.android.robospice.spicelist.okhttp.OkHttpSpiceArrayAdapter;

import java.io.File;

/**
 * Адаптер списка, отображающего коллекцию книг
 */
public class BookshelfListAdapter extends OkHttpSpiceArrayAdapter<BookshelfListAdapter.BookItem> {

	private DownloadEngine mDownloadEngine;
	private BookView.ActionListener mItemActionListener;

	public BookshelfListAdapter(Context context, OkHttpBitmapSpiceManager bitmapSpiceManager, Bookshelf bookshelf, DownloadEngine downloadEngine) {
		super(context, bitmapSpiceManager);

		for (Book book : bookshelf.getBooks())
			add(new BookItem(book, false));

		mDownloadEngine = downloadEngine;
	}

	@Override
	public OkHttpBitmapRequest createRequest(BookItem item, int imageIndex, int requestImageWidth, int requestImageHeight) {
		File tempFile = new File(getContext().getCacheDir(), "BOOK_THUMB_" + item.mBook.getImageFileName());
		return new OkHttpBitmapRequest(item.mBook.getImageUrl(), requestImageWidth, requestImageHeight, tempFile);
	}

	@Override
	public SpiceListItemView<BookItem> createView(Context context, ViewGroup parent) {
		BookView view = new BookView(context, mDownloadEngine);
		view.setListener(mItemActionListener);

		return view;
	}

	public void setItemActionListener(BookView.ActionListener listener) {
		mItemActionListener = listener;
	}

	/**
	 * Обертка вокруг Book. Дополняет информацией о том, показано ли краткое содержание.
	 * Сделана, дабы не мешать модель и визуальное представление.
	 */
	class BookItem {
		Book mBook;
		boolean isContentShown;

		private BookItem(Book book, boolean isContentShown) {
			mBook = book;
			this.isContentShown = isContentShown;
		}
	}
}
