package pri.liyang.chatroom.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Scanner;
import java.util.UUID;

/**
 * @Author: LiYang
 * @Date: 2020/8/7 19:51
 * @Description: 聊天室客户端
 */
public class ChatRoomClient {

    //客户端的通道，可用来发送消息
    public ChannelFuture channelFuture = null;

    //客户端的id
    public String id = UUID.randomUUID().toString();

    //客户端自己输入的昵称
    public String name;

    /**
     * 启动聊天客户端
     * @param host 服务器IP
     * @param port 服务器端口号
     */
    public void bootstrapClient(String host, int port) {

        //客户端的线程组
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //客户端启动类
        Bootstrap boot = new Bootstrap();

        try {
            channelFuture = boot

                    //处理线程组分组
                    .group(workerGroup)

                    //设置客户端通道类型
                    .channel(NioSocketChannel.class)

                    //处理器
                    .handler(new ChannelInitializer() {

                        //初始化客户端channel
                        protected void initChannel(Channel channel) throws Exception {

                            //获得pipeline
                            ChannelPipeline pipeline = channel.pipeline();

                            //加入字符串编解码器
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new StringDecoder());

                            //加入信息处理器
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {

                                //读到服务端消息时
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                    //直接打印服务端消息
                                    System.out.println(msg);
                                }
                            });
                        }
                    })

                    //连接服务端IP和端口
                    .connect(host, port)

                    //阻塞住
                    .sync();

            //扫描器
            Scanner scanner = new Scanner(System.in);

            //先根据提示，输入自己的聊天昵称
            System.out.print("请输入您的昵称：");
            name = scanner.next();

            //组织加入的消息
            String joinMessage = "join" + "~&~" + id + "~#~" + name;

            //将加入的消息发送给服务端
            channelFuture.channel().writeAndFlush(joinMessage);

            //一直不停地聊天
            while (true) {

                //获得客户端聊天的输入消息
                String message = scanner.next();

                //如果输入退群关键词
                if ("_bye_".equals(message)) {

                    //组织退群的消息
                    String exitMessage = "exit" + "~&~" + id;

                    //将退群的消息发送给服务端
                    channelFuture.channel().writeAndFlush(exitMessage);

                    //提示用户已经成功推出群聊
                    System.out.println("你已退出群聊");

                    //终止循环，退出程序
                    break;

                //如果不是退群关键词
                } else {

                    //组织发出聊天的消息
                    String wordsMessage = "words" +  "~&~" + id + "~#~" + message;

                    //将发出聊天的消息发送给服务器
                    channelFuture.channel().writeAndFlush(wordsMessage);
                }
            }

            //有了上面的while(true)，这里也就不用阻塞了
            //channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            //优雅地关闭客户端线程组
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 启动一个客户端实例
     * @param args
     */
    public static void main(String[] args) {
        new ChatRoomClient().bootstrapClient("localhost", 12345);
    }

}
