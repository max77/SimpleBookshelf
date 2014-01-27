package com.max77.SimpleBookshelf.ui;

import android.app.Activity;
import android.os.Bundle;
import com.anfengde.epub.core.value.Constants;
import com.anfengde.epub.ui.BookView;
import com.max77.SimpleBookshelf.R;

/**
 * Экран собсна книги
 */
public class BookViewActivity extends Activity {
	public static final String EXTRA_BOOK_PATH = "extra_book_path32-0230-";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookview);

		String path = getIntent().getStringExtra(EXTRA_BOOK_PATH);

		BookView bookView = (BookView) findViewById(R.id.bookView);
		bookView.setPath(Constants.CACHE_PAHT);
		bookView.initBook();
		bookView.openShelf();
	}
}