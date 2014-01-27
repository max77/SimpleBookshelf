package com.max77.SimpleBookshelf.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.max77.SimpleBookshelf.DownloadEngine;
import com.max77.SimpleBookshelf.R;
import com.octo.android.robospice.spicelist.SpiceListItemView;
import org.apache.commons.lang3.StringUtils;

/**
 * Элемент списка книг
 */
public class BookView extends RelativeLayout implements SpiceListItemView<BookshelfListAdapter.BookItem> {

	private ImageView ivThumbnail;
	private ImageView ivContentMark;
	private TextView tvTitle;
	private Button btnAction;
	private ProgressBar pbDownload;
	private TextView tvMessage;
	private TextView tvContent;

	private BookshelfListAdapter.BookItem mBookItem;
	private DownloadEngine mDownloadEngine;
	private ActionListener mListener;

	public BookView(Context context, DownloadEngine downloadEngine) {
		super(context);

		mDownloadEngine = downloadEngine;

		LayoutInflater.from(context).inflate(R.layout.book, this);
		ivThumbnail = (ImageView) findViewById(R.id.ivThumbnail);
		ivContentMark = (ImageView) findViewById(R.id.ivContentMark);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		btnAction = (Button) findViewById(R.id.btnAction);
		pbDownload = (ProgressBar) findViewById(R.id.pbDownload);
		tvMessage = (TextView) findViewById(R.id.tvMessage);
		tvContent = (TextView) findViewById(R.id.tvContent);
	}

	@Override
	public BookshelfListAdapter.BookItem getData() {
		return mBookItem;
	}

	@Override
	public ImageView getImageView(int imageIndex) {
		return ivThumbnail;
	}

	@Override
	public int getImageViewCount() {
		return (mBookItem != null && StringUtils.isNotEmpty(mBookItem.mBook.getImageUrl())) ? 1 : 0;
	}

	@Override
	public void update(BookshelfListAdapter.BookItem bookItem) {
		mBookItem = bookItem;

		tvTitle.setText(mBookItem.mBook.getTitle());

		DownloadEngine.DownloadInfo info = mDownloadEngine.getDownloadInfo(mBookItem.mBook.getBookFileUrl());

		// Формируем UI в зависимости от текущего статуса загрузки
		int buttonText;
		OnClickListener action = null;
		boolean progressVisible = false;
		int msgText = -1;

		// Попыток загрузки еще не было
		if (info == null) {
			buttonText = R.string.download;
			action = new DownloadOnClickListener();
		}
		// Загрузка в процессе/завершена/...
		else {
			switch (info.getStatus()) {
				case DownloadManager.STATUS_PENDING:
					buttonText = R.string.pending;
					break;

				case DownloadManager.STATUS_RUNNING:
					buttonText = R.string.downloading;
					progressVisible = info.getCurrentSize() > 0 && info.getSize() > 0;
					break;

				case DownloadManager.STATUS_SUCCESSFUL:
					buttonText = R.string.read;
					action = new ReadOnClickListener();
					break;

				default:
					buttonText = R.string.download;
					msgText = R.string.error;
					action = new DownloadOnClickListener();
					break;
			}
		}

		btnAction.setText(buttonText);
		btnAction.setEnabled(action != null);
		btnAction.setOnClickListener(action);

		if (progressVisible) {
			pbDownload.setVisibility(VISIBLE);
			pbDownload.setProgress((int) (info.getCurrentSize() * pbDownload.getMax() / info.getSize()));
		} else {
			pbDownload.setVisibility(GONE);
			pbDownload.setProgress(0);
		}

		if (msgText != -1)
			tvMessage.setText(msgText);
		tvMessage.setVisibility(msgText != -1 ? VISIBLE : GONE);

		setContentShown(mBookItem.mBook.getContent(), mBookItem.isContentShown);
	}

	public void setListener(ActionListener listener) {
		mListener = listener;
	}

	/**
	 * Показывает/скрывает краткое содержание
	 *
	 * @param shown true - показать, false - скрыть
	 */
	public void setContentShown(String content, boolean shown) {
		if (StringUtils.isNotBlank(content)) {
			ivContentMark.setVisibility(VISIBLE);
			ivContentMark.setImageResource(shown ? R.drawable.shrink_content : R.drawable.expand_content);
			tvContent.setVisibility(shown ? VISIBLE : GONE);
			tvContent.setText(content);

			ivThumbnail.setOnClickListener(new ShowContentOnClickListener());
			mBookItem.isContentShown = shown;
		} else {
			ivContentMark.setVisibility(GONE);
			tvContent.setVisibility(GONE);

			ivThumbnail.setOnClickListener(null);
		}
	}

	public interface ActionListener {
		public void onDownloadBook(BookView view);

		public void onReadBook(BookView view);
	}

	private class ShowContentOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			setContentShown(mBookItem.mBook.getContent(), !mBookItem.isContentShown);
		}
	}

	private class DownloadOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (mListener != null)
				mListener.onDownloadBook(BookView.this);
		}
	}

	private class ReadOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (mListener != null)
				mListener.onReadBook(BookView.this);
		}
	}

}
