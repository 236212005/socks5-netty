package server.socks5.handler.ss5;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {
    EventLoopGroup bossGroup;

    public Socks5CommandRequestHandler(EventLoopGroup bossGroup) {
        this.bossGroup = bossGroup;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext clientChannelContext, DefaultSocks5CommandRequest msg) throws Exception {
        log.debug("socks5命令：{}，目标地址：{}:{}", msg.type().toString(), msg.dstAddr(), msg.dstPort());
        if (msg.type().equals(Socks5CommandType.CONNECT)) {
            log.trace("准备连接目标：{}:{}", msg.dstAddr(), msg.dstPort());
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(bossGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //ch.pipeline().addLast(new LoggingHandler());//in out
                            //将目标服务器信息转发给客户端
                            ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
                        }
                    });
            ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    log.trace("成功连接目标：{}:{}", msg.dstAddr(), msg.dstPort());
                    clientChannelContext.pipeline().addLast(new Client2DestHandler(future1));
                    Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                    clientChannelContext.writeAndFlush(commandResponse);
                } else {
                    log.trace("连接目标：{}:{}失败！", msg.dstAddr(), msg.dstPort());
                    Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                    clientChannelContext.writeAndFlush(commandResponse);
                }
            });
        } else {
            clientChannelContext.fireChannelRead(msg);
        }
    }

    /**
     * 将目标服务器信息转发给客户端
     *
     * @author huchengyi
     */
    private static class Dest2ClientHandler extends ChannelInboundHandlerAdapter {

        private final ChannelHandlerContext clientChannelContext;

        public Dest2ClientHandler(ChannelHandlerContext clientChannelContext) {
            this.clientChannelContext = clientChannelContext;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx2, Object destMsg) throws Exception {
            log.trace("将目标服务器{}信息转发给客户端{}",
                    ctx2.channel().remoteAddress().toString(),
                    clientChannelContext.channel().remoteAddress());
            clientChannelContext.writeAndFlush(destMsg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx2) throws Exception {
            log.trace("目标服务器{}断开连接", ctx2.channel().remoteAddress());
            clientChannelContext.channel().close();
        }
    }

    /**
     * 将客户端的消息转发给目标服务器端
     *
     * @author huchengyi
     */
    private static class Client2DestHandler extends ChannelInboundHandlerAdapter {

        private final ChannelFuture destChannelFuture;

        public Client2DestHandler(ChannelFuture destChannelFuture) {
            this.destChannelFuture = destChannelFuture;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.trace("将客户端{}的消息转发给目标服务器{}",
                    ctx.channel().remoteAddress().toString(),
                    destChannelFuture.channel().remoteAddress().toString());
            destChannelFuture.channel().writeAndFlush(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.trace("客户端{}断开连接", ctx.channel().remoteAddress());
            destChannelFuture.channel().close();
        }
    }
}
