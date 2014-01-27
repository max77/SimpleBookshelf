package com.max77.SimpleBookshelf.network;

import com.octo.android.robospice.request.springandroid.SpringAndroidSpiceRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import roboguice.util.temp.Ln;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public abstract class RequestBase<T> extends SpringAndroidSpiceRequest<T> {
	private static long sRequestCounter;
	private static ArrayList<ClientHttpRequestInterceptor> sInterceptors;
	private Class<T> mResponseClass;

	public RequestBase(Class<T> responseClass) {
		super(responseClass);
		mResponseClass = responseClass;
	}

	@Override
	public T loadDataFromNetwork() throws Exception {
		getRestTemplate().setInterceptors(sInterceptors);
		return loadDataFromNetworkInternal();
	}

	protected abstract T loadDataFromNetworkInternal() throws Exception;

	private static class HttpResponseCopy implements ClientHttpResponse {
		private ByteArrayInputStream bais;
		private ClientHttpResponse response;

		private HttpResponseCopy(ClientHttpResponse source, byte[] body) {
			bais = new ByteArrayInputStream(body);
			response = source;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			try {
				bais.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return bais;
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}
	}

	static {
		sInterceptors = new ArrayList<ClientHttpRequestInterceptor>();
		sInterceptors.add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(final HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
				sRequestCounter++;

				if (Ln.isDebugEnabled()) {
					Ln.d(">>> REQUEST (" + sRequestCounter + ")");

					if (request.getURI() != null)
						Ln.d("RQ METHOD:\n" + request.getMethod().toString());

					if (request.getURI() != null)
						Ln.d("RQ URI:\n" + request.getURI().toString());

					if (request.getHeaders() != null)
						Ln.d("RQ HEADERS:\n" + request.getHeaders().toString());

					if (body != null) {
						String str = new String(body);
						Ln.d("RQ BODY:\n" + "{" + str + "}");
					}

					Ln.d("<<< REQUEST (" + sRequestCounter + ")");
				}

				ClientHttpResponse response = execution.execute(request, body);

				if (Ln.isDebugEnabled()) {
					Ln.d(">>> RESPONSE (" + sRequestCounter + ")");
					Ln.d("RESP STATUS:\n" + response.getStatusText() + "(" + response.getStatusCode().value() + ")");

					if (response.getHeaders() != null)
						Ln.d("RESP HEADERS:\n" + response.getHeaders().toString());
				}


				ClientHttpResponse result;
				try {
					InputStream responseBody = response.getBody();
					if (responseBody != null) {
						byte[] bytes = FileCopyUtils.copyToByteArray(responseBody);

						// Это костыль !!! Сервер отдает неправильный Content-Type и ломается парсер
						if (bytes.length >= 4)
							response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
						else
							bytes = null;

						if (Ln.isDebugEnabled()) {
							String str = new String(bytes);
							Ln.d("RESP BODY:\n" + "{" + str.replaceAll("><", ">\n<") + "}");
						}

						result = new HttpResponseCopy(response, bytes);
					} else
						result = response;
				} catch (Exception e) {
					result = response;
				}

				if (Ln.isDebugEnabled())
					Ln.d("<<< RESPONSE (" + sRequestCounter + ")");

				return result;
			}
		});

		sRequestCounter = 0;
	}
}
