package socks5.log;

import lombok.extern.slf4j.Slf4j;
import socks5.handler.ProxyChannelTrafficShapingHandler;
import io.netty.channel.ChannelHandlerContext;

import java.net.*;
import java.util.Enumeration;

@Slf4j
public class ProxyFlowLog4j implements ProxyFlowLog {

    public void log(ChannelHandlerContext ctx) {
        ProxyChannelTrafficShapingHandler trafficShapingHandler = ProxyChannelTrafficShapingHandler.get(ctx);
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        long readByte = trafficShapingHandler.trafficCounter().cumulativeReadBytes();
        long writeByte = trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();

//        logger.info("======================" + System.lineSeparator() +
//                        "用户：{}" + System.lineSeparator() +
//                        "开始时间：{}" + System.lineSeparator() +
//                        "结束时间：{}" + System.lineSeparator() +
//                        "本地监听：{}:{}" + System.lineSeparator() +
//                        "远端信息：{}:{}" + System.lineSeparator() +
//                        "读取字节：{}" + System.lineSeparator() +
//                        "写入字节：{}" + System.lineSeparator() +
//                        "服务器IO字节数：{}" + System.lineSeparator(),
//                trafficShapingHandler.getUsername(),
//                new SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(new Timestamp(trafficShapingHandler.getBeginTime())),
//                new SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(new Timestamp(trafficShapingHandler.getEndTime())),
//                getLocalAddress(),
//                localAddress.getPort(),
//                remoteAddress.getAddress().getHostAddress(),
//                remoteAddress.getPort(),
//                readByte,
//                writeByte,
//                (readByte + writeByte));
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
