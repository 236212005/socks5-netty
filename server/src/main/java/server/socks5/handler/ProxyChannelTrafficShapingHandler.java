package server.socks5.handler;

import lombok.experimental.Accessors;
import server.socks5.log.ProxyFlowLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

@Accessors(chain = true)
public class ProxyChannelTrafficShapingHandler extends ChannelTrafficShapingHandler {

    public static final String PROXY_TRAFFIC = "ProxyChannelTrafficShapingHandler";
    private final ProxyFlowLog proxyFlowLog;
    private long beginTime;
    private long endTime;
    private String username = "anonymous";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public static void username(ChannelHandlerContext ctx, String username) {
        get(ctx).username = username;
    }

    public static ProxyChannelTrafficShapingHandler get(ChannelHandlerContext ctx) {
        return (ProxyChannelTrafficShapingHandler) ctx.pipeline().get(PROXY_TRAFFIC);
    }

    public ProxyChannelTrafficShapingHandler(long checkInterval, ProxyFlowLog proxyFlowLog) {
        super(checkInterval);
        this.proxyFlowLog = proxyFlowLog;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        beginTime = System.currentTimeMillis();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        endTime = System.currentTimeMillis();
        proxyFlowLog.log(ctx);
        super.channelInactive(ctx);
    }
}
