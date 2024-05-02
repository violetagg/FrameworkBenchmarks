package hello;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.SERVER;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.jsoniter.output.JsonStream;
import com.jsoniter.output.JsonStreamPool;
import com.jsoniter.spi.JsonException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
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

	private static Message newMsg() {
		return new Message("Hello, World!");
	}

	private static byte[] serializeMsg(Message obj) {
		JsonStream stream = JsonStreamPool.borrowJsonStream();
		try {
			stream.reset(null);
			stream.writeVal(Message.class, obj);
			return Arrays.copyOfRange(stream.buffer().data(), 0, stream.buffer().tail());
		} catch (IOException e) {
			throw new JsonException(e);
		} finally {
			JsonStreamPool.returnJsonStream(stream);
		}
	}

	private static int jsonLen() {
		return serializeMsg(newMsg()).length;
	}

	private static final byte[] STATIC_PLAINTEXT = "Hello, World!".getBytes(CharsetUtil.UTF_8);
	private static final int STATIC_PLAINTEXT_LEN = STATIC_PLAINTEXT.length;

	private static final CharSequence PLAINTEXT_CL_HEADER_VALUE = AsciiString.cached(String.valueOf(STATIC_PLAINTEXT_LEN));
	private static final int JSON_LEN = jsonLen();
	private static final CharSequence JSON_CL_HEADER_VALUE = AsciiString.cached(String.valueOf(JSON_LEN));
	private static final CharSequence SERVER_NAME = AsciiString.cached("Reactor Netty");

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
		String uri = request.uri();
		switch (uri) {
			case "/plaintext":
				return makeResponse(response, Unpooled.wrappedBuffer(STATIC_PLAINTEXT), TEXT_PLAIN, PLAINTEXT_CL_HEADER_VALUE);
			case "/json":
				return makeResponse(response, Unpooled.wrappedBuffer(serializeMsg(newMsg())), APPLICATION_JSON, JSON_CL_HEADER_VALUE);
		}
		return response.sendNotFound();
	}

	private Publisher<Void> makeResponse(HttpServerResponse response, ByteBuf buf, CharSequence contentType, CharSequence contentLength) {
		return response.header(CONTENT_TYPE, contentType)
				.header(SERVER, SERVER_NAME)
				.header(DATE, date)
				.header(CONTENT_LENGTH, contentLength)
				.sendObject(buf);
	}
}
