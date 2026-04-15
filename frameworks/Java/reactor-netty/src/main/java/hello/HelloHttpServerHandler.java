package hello;

import static hello.Constants.JSON_CLHEADER_VALUE;
import static hello.Constants.PLAINTEXT_CLHEADER_VALUE;
import static hello.Constants.SERVER_NAME;
import static hello.Constants.STATIC_PLAINTEXT;
import static hello.Constants.newMsg;
import static hello.JsonUtils.acquireJsonStream;
import static hello.JsonUtils.releaseJsonStream;
import static hello.JsonUtils.serializeMsg;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.SERVER;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.jsoniter.output.JsonStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.FastThreadLocal;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class HelloHttpServerHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

	private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		}
	};

	private volatile CharSequence date = new AsciiString(FORMAT.get().format(new Date()));

	HelloHttpServerHandler(ScheduledExecutorService service) {
		service.scheduleWithFixedDelay(new Runnable() {
			private final DateFormat format = FORMAT.get();

			@Override
			public void run() {
				date = new AsciiString(format.format(new Date()));
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
		return switch (request.uri()) {
			case "/plaintext" -> makeResponse(response, Unpooled.wrappedBuffer(STATIC_PLAINTEXT), TEXT_PLAIN, PLAINTEXT_CLHEADER_VALUE);
			case "/json" -> {
				var stream = acquireJsonStream();
				try {
					yield makeResponse(response, Unpooled.wrappedBuffer(serializeMsg(newMsg(), stream)), APPLICATION_JSON, JSON_CLHEADER_VALUE);
				} finally {
					releaseJsonStream(stream);
				}
			}
			default -> response.sendNotFound();
		};
	}

	private Publisher<Void> makeResponse(HttpServerResponse response, ByteBuf buf, CharSequence contentType, CharSequence contentLength) {
		return response.header(CONTENT_TYPE, contentType)
				.header(SERVER, SERVER_NAME)
				.header(DATE, date)
				.header(CONTENT_LENGTH, contentLength)
				.sendObject(buf);
	}
}
