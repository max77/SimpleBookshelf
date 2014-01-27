package com.max77.SimpleBookshelf.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSetter;

/**
 * Java-представление элемента коллекции книг
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {

	@JsonProperty("id")
	private Integer mId;

	@JsonProperty("title")
	private String mTitle;

	@JsonProperty("content")
	private String mContent;

	@JsonProperty("image")
	private String mImageUrl;

	/** Имя файла для кэширования. См. {@link #setImageUrl(String)} */
	private String mImageFileName;

	@JsonProperty("file")
	private String mBookFileUrl;


	public Integer getId() {
		return mId;
	}

	public void setId(Integer id) {
		mId = id;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}

	public String getContent() {
		return mContent;
	}

	public void setContent(String content) {
		mContent = content;
	}

	public String getImageUrl() {
		return mImageUrl;
	}

	@JsonSetter("image")
	public void setImageUrl(String imageUrl) {
		mImageUrl = imageUrl;

		// Оставляем в имени файла только буквы/цифры и для надежности добавляем hashCode исходного URL'а
		mImageFileName = mImageUrl != null ? mImageUrl.replaceAll("[^A-Za-z0-9]", "") + String.valueOf(mImageUrl.hashCode()) : null;
	}

	public String getImageFileName() {
		return mImageFileName;
	}

	public String getBookFileUrl() {
		return mBookFileUrl;
	}

	public void setBookFileUrl(String bookFileUrl) {
		mBookFileUrl = bookFileUrl;
	}

	public boolean isValid() {
		return mId != null && mTitle != null && mBookFileUrl != null;
	}
}
