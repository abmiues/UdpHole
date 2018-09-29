package com.abmiues;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;


public class UdpServer extends Thread {

	
	int  MaxByteLength=512;//定义最大数据报字节数
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		if(!_instance.isAlive())
			super.start();
	}
	private static UdpServer _instance;
	public static UdpServer Instance()
	{
		if (_instance==null) 
			_instance=new UdpServer();
		return _instance;
		
	}
	
	DatagramChannel udpChannel;
	Selector selector;
	private ByteBuffer _buff=ByteBuffer.allocate(1024);//接受数据用的byte数组，系统自带的类，只能用这个接收和发送
	private ByteArray _recvBuff=new ByteArray(32*1024);//自己定义的byte数组类，因为需要自定义数据交互协议，像是加密标记，报文长度等等
	@Override
	public void run() 
	{
		try {
			selector=Selector.open();
			udpChannel=DatagramChannel.open();
			udpChannel.configureBlocking(false);//设置非阻塞
			udpChannel.socket().bind(new InetSocketAddress(4622));//绑定端口
			udpChannel.register(selector, SelectionKey.OP_READ);//绑定select
			while (selector.select()>0) {
				Iterator<SelectionKey> iterator=selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key=iterator.next();
					iterator.remove();
					if(key.isReadable())
					{
						DatagramChannel sc=(DatagramChannel) key.channel();
						String uid="";
						if(key.attachment()!=null)
							uid=(String) key.attachment();//获取该通道的uid,如果uid==""，表示还没有绑定uid
						_buff.clear();
						
						int size = 0;
						try{
							SocketAddress address=sc.receive(_buff);
							String[] ipHost=address.toString().replace("/", "").split(":");
							String clientIp=ipHost[0];
							String clientHost=ipHost[1];
							_buff.flip();
							
							while (_buff.hasRemaining()) {
								if(_recvBuff.IsFull())//缓存满了，动态扩展
									_recvBuff.AutoExpand(true);
								_recvBuff.WriteBytes(_buff.array(),_buff.limit());
								_buff.position(_buff.limit());
							}
							_buff.clear();
						}
						catch(IOException e) {
							System.out.println("客户端强制关闭连接");
							size=-1;
						}
						recv(_recvBuff,sc,uid);//传入ByteArray数据和sc,recv中对_recvBuff进行校验，主要是粘包处理
						key.interestOps(SelectionKey.OP_READ);//重新设置该通道为接受数据
						
						if(size==-1)//如果客户端socket断开，会触发读操作，返回size=-1，这是需关闭的通道
						{
							/*if(!uid.equals("")&_socketList.containsKey(uid))//判断改通道是否已绑定uid
								_socketList.remove(uid);//移除uid与通道的绑定
							System.out.println("remove uid:"+uid);
							sc.close();//关闭socketChannel
							key.cancel();//关闭通道
							if(key.channel()!=null)//如果通道没有关闭成功
							{
								key.channel().close();//手动调用通道关闭方法
							}
							continue;//跳过本次循环
*/						}
						
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("服务器启动");
	}
	
	/**
	 * 处理接收到的byteArray,如需解压，解密等均在这里处理，此处仅对粘包做了处理
	 * @param buff
	 * @param socket
	 */
	private void recv(ByteArray recvBuff,DatagramChannel socket,String uid)
	{
		recvBuff.GetLen();
		int av =recvBuff.GetAvailable();
		while (av>=4) {//这里用循环，是因为可能接收到了多条数据，需要逐条处理
			int packagelen=recvBuff.ReadInt();
			if(av>=packagelen)//如果收到的数据大于包头记录的包体长度，说明数据已经接受完毕
			{
				String data=recvBuff.ReadStringUInt();//ReadStringUInt里面做了编码转换
				System.out.println("收到udp数据："+data);
				//_threadPool.SubmitTask(data, socket,uid);//提交任务
				av=recvBuff.GetAvailable();
			}
			else//buff中数据不足，恢复指针位置，进行下次recv
			{
				recvBuff.Offset(-4);
				break;
			}
		}
	}
	
	
	
}
