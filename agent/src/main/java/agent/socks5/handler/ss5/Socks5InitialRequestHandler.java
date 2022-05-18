package agent.socks5.handler.ss5;

import agent.socks5.ProxyAgent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private final ProxyAgent proxyAgent;

    public Socks5InitialRequestHandler(ProxyAgent proxyAgent) {
        this.proxyAgent = proxyAgent;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        log.debug("初始化ss5连接，远端地址:{}", ctx.channel().remoteAddress());
        if (msg.decoderResult().isFailure()) {
            log.debug("远端链接使用了无法解析协议版本：{}", msg.version());
            ctx.fireChannelRead(msg);
        } else {
            log.debug("socks5版本号:{}", msg.version());
            if (msg.version().equals(SocksVersion.SOCKS5)) {
                ctx.writeAndFlush(new DefaultSocks5InitialResponse(proxyAgent.isAuth() ?
                        Socks5AuthMethod.PASSWORD : Socks5AuthMethod.NO_AUTH));
            }
        }
    }

}
