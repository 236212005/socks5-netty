package agent.socks5.handler;

import io.netty.channel.ChannelHandlerContext;

public interface ChannelListener {

    void inActive(ChannelHandlerContext ctx);

    void active(ChannelHandlerContext ctx);
}
