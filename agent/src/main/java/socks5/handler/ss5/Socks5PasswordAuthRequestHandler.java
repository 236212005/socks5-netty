package socks5.handler.ss5;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import lombok.extern.slf4j.Slf4j;
import socks5.auth.PasswordAuth;
import socks5.handler.ProxyChannelTrafficShapingHandler;

@Slf4j
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    //TODO CHANGE TO USE DATABASE
    private final PasswordAuth passwordAuth;

    public Socks5PasswordAuthRequestHandler(PasswordAuth passwordAuth) {
        this.passwordAuth = passwordAuth;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        log.debug("用户名密码 : " + msg.username() + "," + msg.password());
        ProxyChannelTrafficShapingHandler.username(ctx, msg.username());
        boolean authFlag = passwordAuth.auth(msg.username(), msg.password());
        Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(
                authFlag ? Socks5PasswordAuthStatus.SUCCESS : Socks5PasswordAuthStatus.FAILURE);
        ChannelFuture future = ctx.writeAndFlush(passwordAuthResponse);
        if (!authFlag) {
            //发送鉴权失败消息，完成后关闭channel
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
