package hello;

import io.netty.channel.ChannelOption;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import reactor.netty.http.HttpResources;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

public class HelloHttpServer {

	static {
		ResourceLeakDetector.setLevel(Level.DISABLED);
	}

	public static void main(String[] args) {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}

		HttpServer server =
				HttpServer.create()
						.port(port)
						.option(ChannelOption.SO_BACKLOG, 8192)
						.option(ChannelOption.SO_REUSEADDR, true)
						.option(UnixChannelOption.SO_REUSEPORT, true)
						.childOption(ChannelOption.SO_REUSEADDR, true)
						.httpRequestDecoder(spec -> spec.maxInitialLineLength(4096)
								.maxHeaderSize(8192)
								.maxChunkSize(8192)
								.validateHeaders(false))
						.doOnBound(disposableServer -> System.out.printf("Http started. Listening on: %s%n", disposableServer.address()))
						.handle(new HelloHttpServerHandler(HttpResources.get().onServer(LoopResources.hasNativeSupport()).next()));

		server.bindNow()
				.onDispose()
				.block();
	}
}
