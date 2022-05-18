package socks5;

import lombok.extern.slf4j.Slf4j;
import socks5.auth.PasswordAuth;
import socks5.auth.PropertiesPasswordAuth;
import socks5.handler.ChannelListener;
import socks5.handler.ProxyChannelTrafficShapingHandler;
import socks5.handler.ProxyIdleHandler;
import socks5.handler.ss5.Socks5CommandRequestHandler;
import socks5.handler.ss5.Socks5InitialRequestHandler;
import socks5.handler.ss5.Socks5PasswordAuthRequestHandler;
import socks5.log.ProxyFlowLog;
import socks5.log.ProxyFlowLog4j;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Properties;

@Slf4j
public class ProxyServer {

    private static final ProxyServer instance = new ProxyServer();
    private boolean isAuth;

    private ProxyServer() {
    }

    public static ProxyServer getInstance() {
        return instance;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public static void main(String[] args) throws Exception {
        int port = 11080;
        boolean auth = false;
        Properties properties = new Properties();
        try {
            properties.load(ProxyServer.class.getResourceAsStream("/config.properties"));
            port = Integer.parseInt(properties.getProperty("port"));
            auth = Boolean.parseBoolean(properties.getProperty("auth"));
        } catch (Exception e) {
            log.warn("load config.properties error, default port 11080, auth false!");
        }
        ProxyServer.getInstance().start(true, auth, port);
    }

    public void start(boolean logging, boolean isAuth, int port) throws Exception {
        log.info("Starting server with {} ,{} at port {}", logging ? "logging" : "no logging", isAuth ? "auth" : "no auth", port);
        this.isAuth = isAuth;
        ProxyFlowLog proxyFlowLog = new ProxyFlowLog4j();
        //TODO USE DATABASE
        PasswordAuth passwordAuth = new PropertiesPasswordAuth();
        EventLoopGroup boss = new NioEventLoopGroup(8);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //流量统计
                            ch.pipeline().addLast(
                                    ProxyChannelTrafficShapingHandler.PROXY_TRAFFIC,
                                    new ProxyChannelTrafficShapingHandler(3000, proxyFlowLog)
                            );
                            //channel超时处理
                            ch.pipeline().addLast(new IdleStateHandler(3, 30, 0));
                            ch.pipeline().addLast(new ProxyIdleHandler());
                            //netty日志
                            if (logging) {
                                ch.pipeline().addLast(new LoggingHandler());
                            }
                            //Socks5MessageByteBuf
                            ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestHandler(ProxyServer.this));
                            if (isAuth) {
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestHandler(passwordAuth));
                            }
                            //socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                            //Socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestHandler(new NioEventLoopGroup()));
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.debug("bind port : " + port);
            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
