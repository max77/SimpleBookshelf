package com.max77.SimpleBookshelf.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;

/**
 * Java-представление коллекции книг
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bookshelf {

	@JsonProperty("books")
	private ArrayList<Book> mBooks;

	public ArrayList<Book> getBooks() {
		return mBooks;
	}

	public void setBooks(ArrayList<Book> books) {
		mBooks = books;
	}

	/**
	 * Проверяем валидность полученной коллекции, выкидывая мусор, если нужно.
	 */
	public void validate() {
		int i = 0;
		while (i < mBooks.size() && mBooks.get(i).isValid())
			i++;

		if (i != mBooks.size()) {
			ArrayList<Book> filtered = new ArrayList<Book>();
			for (Book book : mBooks)
				if (book.isValid())
					filtered.add(book);

			mBooks = filtered;
		}
	}
}
