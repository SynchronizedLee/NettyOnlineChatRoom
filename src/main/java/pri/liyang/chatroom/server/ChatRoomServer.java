package pri.liyang.chatroom.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: LiYang
 * @Date: 2020/8/7 19:50
 * @Description: 聊天室服务端
 */
public class ChatRoomServer {

    //channel的组，用来装所有的聊天室客户端
    public ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //客户端id和聊天昵称映射Map
    public Map<String, String> nameMap = new ConcurrentHashMap<String, String>();

    //用于转化时间
    public final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 启动聊天服务器
     * @param port 服务器端口号
     */
    public void bootstrapServer(int port) {
        //接收客户端连接的线程
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        //处理读写事件的线程
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);

        //服务器启动类
        ServerBootstrap sBoot = new ServerBootstrap();

        try {
            ChannelFuture channelFuture = sBoot

                    //连接和读写线程组分组
                    .group(bossGroup, workerGroup)

                    //设置服务端通道类型
                    .channel(NioServerSocketChannel.class)

                    //子处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        //初始化服务端channel
                        protected void initChannel(SocketChannel channel) throws Exception {

                            //获得pipeline
                            ChannelPipeline pipeline = channel.pipeline();

                            //加入字符串编解码器
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new StringDecoder());

                            //加入信息处理器
                            pipeline.addLast(new SimpleChannelInboundHandler() {

                                //有客户端加入时
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    //将新来的客户端加入
                                    clients.add(ctx.channel());
                                }

                                //读到客户端的消息时
                                protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {

                                    //将消息转化为String
                                    String message = (String) obj;

                                    //分解消息类型和消息
                                    String[] parse = message.split("~&~");

                                    //获取消息类型
                                    String messageType = parse[0];

                                    //获取消息内容
                                    String[] messageContent = parse[1].split("~#~");

                                    //回应消息
                                    String backMessage = sdf.format(new Date());

                                    //如果是聊天内容
                                    if ("words".equals(messageType)) {

                                        //获取id
                                        String id = messageContent[0];

                                        //获取聊天内容
                                        String words = messageContent[1];

                                        //获取昵称
                                        String name = nameMap.get(id);

                                        //组织群发回客户端的聊天消息
                                        backMessage += "【" + name + "】" + words;

                                    //如果是有客户端加入的消息
                                    } else if ("join".equals(messageType)) {

                                        //获取id和昵称
                                        String id = messageContent[0];
                                        String name = messageContent[1];

                                        //将新的客户端的id和昵称加入
                                        nameMap.put(id, name);

                                        //组织群发回客户端的加入消息
                                        backMessage += "【系统消息】" + name +  " 加入了群聊";

                                    //如果是客户端退出群聊的消息
                                    } else if ("exit".equals(messageType)) {
                                        //获取id和昵称
                                        String id = messageContent[0];
                                        String name = nameMap.get(id);

                                        //移除客户端
                                        clients.remove(ctx.channel());
                                        //移除映射Map
                                        nameMap.remove(id);

                                        //组织群发回客户端的退群消息
                                        backMessage += "【系统消息】" + name +  " 退出了群聊";
                                    }

                                    //服务端打印群发消息
                                    System.out.println(backMessage);

                                    //服务端群发消息给所有客户端
                                    clients.writeAndFlush(backMessage);
                                }
                            });
                        }
                    })

                    //服务端绑定接口
                    .bind(port)

                    //阻塞住
                    .sync();

            System.out.println("服务器已启动，将记录所有聊天过程");
            System.out.println("============================================");

            //服务端关闭前阻塞
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            //优雅地关闭所有线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 启动服务端实例
     * @param args
     */
    public static void main(String[] args) {
        new ChatRoomServer().bootstrapServer(12345);
    }

}
