package server.socks5.log;

import server.socks5.log.ProxyFlowLog;
import lombok.extern.slf4j.Slf4j;
import server.socks5.handler.ProxyChannelTrafficShapingHandler;
import io.netty.channel.ChannelHandlerContext;

import java.net.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

@Slf4j
public class ProxyFlowLog4j implements ProxyFlowLog {

    public void log(ChannelHandlerContext ctx) {
        ProxyChannelTrafficShapingHandler trafficShapingHandler = ProxyChannelTrafficShapingHandler.get(ctx);
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        long readByte = trafficShapingHandler.trafficCounter().cumulativeReadBytes();
        long writeByte = trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();

        double opTime = (double) (trafficShapingHandler.getEndTime() - trafficShapingHandler.getBeginTime()) / 1000;
        log.info("用户：{}" +
                        "，开始时间：{}" +
                        "，结束时间：{}" +
                        "，代理时长：{}秒" +
                        "，本地监听：{}:{}" +
                        "，远端信息：{}:{}" +
                        "，读取字节：{}" +
                        "，写入字节：{}" +
                        "，服务器IO字节数：{}" +
                        "，IO处理速度：{}字节/秒",
                trafficShapingHandler.getUsername(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(new Timestamp(trafficShapingHandler.getBeginTime())),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(new Timestamp(trafficShapingHandler.getEndTime())),
                opTime,
                getLocalAddress(),
                localAddress.getPort(),
                remoteAddress.getAddress().getHostAddress(),
                remoteAddress.getPort(),
                readByte,
                writeByte,
                (readByte + writeByte),
                (readByte + writeByte) / opTime);
    }

    /**
     * 获取本机的IP
     *
     * @return Ip地址
     */
    private static String getLocalAddress() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.debug("Error when getting host ip address: <{}>.", e.getMessage());
        }
        return "127.0.0.1";
    }

}
