package socks5.handler;

import socks5.log.ProxyFlowLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class ProxyChannelTrafficShapingHandler extends ChannelTrafficShapingHandler {

    public static final String PROXY_TRAFFIC = "ProxyChannelTrafficShapingHandler";

    private final ProxyFlowLog proxyFlowLog;

    public static ProxyChannelTrafficShapingHandler get(ChannelHandlerContext ctx) {
        return (ProxyChannelTrafficShapingHandler) ctx.pipeline().get(PROXY_TRAFFIC);
    }

    public ProxyChannelTrafficShapingHandler(long checkInterval, ProxyFlowLog proxyFlowLog) {
        super(checkInterval);
        this.proxyFlowLog = proxyFlowLog;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        proxyFlowLog.log(ctx);
        super.channelInactive(ctx);
    }
}
