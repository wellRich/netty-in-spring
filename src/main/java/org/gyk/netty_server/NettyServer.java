package org.gyk.netty_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.gyk.netty_server.handler.SecondHandler;
import org.gyk.netty_server.util.BodyReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StopWatch;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@Slf4j
public class NettyServer {


    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("start app...");
        ConfigurableApplicationContext context = SpringApplication.run(NettyServer.class, args);
        //get bean
        SecondHandler handler = context.getBean(SecondHandler.class);
        int port = context.getEnvironment().getProperty("server.port", Integer.class, 9000);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(16);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("codec", new HttpServerCodec())
                                    .addLast("bb", new ChannelInboundHandlerAdapter() {
                                        final AttributeKey<BodyReader> KEY = AttributeKey.valueOf("IO");

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            if (msg instanceof HttpRequest) {
                                                HttpRequest request = (HttpRequest) msg;
                                                String contentType = request.headers().get("Content-Type");
                                                log.info("ct--->{}", contentType);
                                                int i = contentType.indexOf(BodyReader.BOUNDARY_FLAG);
                                                byte[] boundary = contentType.substring(i + BodyReader.BOUNDARY_FLAG.length()).getBytes(StandardCharsets.UTF_8);
                                                int boundaryPrefixLength = boundary.length + 2;
                                                BodyReader bodyReader = new BodyReader(boundaryPrefixLength);
                                                bodyReader.beginParse();
                                                bodyReader.setSkip(bodyReader.getBoundaryPrefixLength());
                                                ctx.channel().attr(KEY).set(bodyReader);
                                            } else if (msg instanceof LastHttpContent) {
                                                ctx.fireChannelRead(ctx.channel().attr(KEY).get());
                                            } else {
                                                HttpContent httpContent = (HttpContent) msg;
                                                BodyReader bodyReader = ctx.channel().attr(KEY).get();
                                                ByteBuf byteBuf = httpContent.content();
                                                if (!bodyReader.hasBeginParse()) {
                                                    byteBuf.readerIndex(bodyReader.getBoundaryPrefixLength());
                                                    bodyReader.beginParse();
                                                }
                                                handler.readBody(byteBuf, bodyReader);
                                            }
                                        }
                                    })
                                    .addLast("handler", handler);
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind().sync();
            stopWatch.stop();
            log.info("Netty Server started， Listening on {}, info={}", port, stopWatch.prettyPrint());
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
