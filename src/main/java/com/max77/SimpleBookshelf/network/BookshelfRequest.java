package com.max77.SimpleBookshelf.network;

import com.max77.SimpleBookshelf.model.Bookshelf;
import roboguice.util.temp.Ln;

/**
 * Запрос на получение коллекции книг
 */
public class BookshelfRequest extends RequestBase<Bookshelf> {
	public static final String SERVER_URL = "http://test.sobolevdesign.ru";

	public BookshelfRequest() {
		super(Bookshelf.class);
	}

	@Override
	public Bookshelf loadDataFromNetworkInternal() throws Exception {
		Ln.i("Loading bookshelf...");

		Bookshelf bookshelf = getRestTemplate().getForObject(SERVER_URL, Bookshelf.class);
		bookshelf.validate();

		return bookshelf;
	}
}
