package agent.socks5;

import cn.hutool.core.util.StrUtil;
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
import lombok.extern.slf4j.Slf4j;
import agent.socks5.auth.PasswordAuth;
import agent.socks5.auth.PropertiesPasswordAuth;
import agent.socks5.handler.ProxyChannelTrafficShapingHandler;
import agent.socks5.handler.ProxyIdleHandler;
import agent.socks5.handler.ss5.Socks5CommandRequestHandler;
import agent.socks5.handler.ss5.Socks5InitialRequestHandler;
import agent.socks5.handler.ss5.Socks5PasswordAuthRequestHandler;
import agent.socks5.log.ProxyFlowLog;
import agent.socks5.log.ProxyFlowLog4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProxyAgent {

    private static final ProxyAgent instance = new ProxyAgent();
    private boolean isAuth;

    private ProxyAgent() {
    }

    public static ProxyAgent getInstance() {
        return instance;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public static void main(String[] args) throws Exception {
        log.info("\r\nUsage: \r\n" +
                "-l: Open agent.socks5 logging use true, else use false, default is false.\r\n" +
                "-p: Specify a port which server listened, default is 11080.\r\n" +
                "-a: If need an identification to access the server. If need plz use true, else use false, default is false.");
        Map<String, String> argMap = buildParams(args);
        int exitCode = 0;
        try {
            int port = Integer.parseInt(argMap.get("-p") == null ? "11080" : argMap.get("-p"));
            boolean auth = Boolean.parseBoolean(argMap.get("-a") == null ? "false" : argMap.get("-a"));
            boolean logging = Boolean.parseBoolean(argMap.get("-l") == null ? "false" : argMap.get("-l"));
            ProxyAgent.getInstance().start(logging, auth, port);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    private static Map<String, String> buildParams(String[] args) {
        Map<String, String> resultMap = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (StrUtil.isNotBlank(args[i]) && args[i].startsWith("-")) {
                resultMap.put(args[i], args[i + 1]);
                i++;
            }
        }
        return resultMap;
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
                            ch.pipeline().addLast(new Socks5InitialRequestHandler(ProxyAgent.this));
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
