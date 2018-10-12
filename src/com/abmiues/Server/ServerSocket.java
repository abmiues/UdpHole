package com.abmiues.Server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ServerSocket extends Thread{
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		if(!_instance.isAlive())
			super.start();
	}

	@Override
	public void run() {
		init();
	}
	private static ServerSocket _instance;
	public static ServerSocket Instance()
	{
		if (_instance==null) 
			_instance=new ServerSocket();
		return _instance;
		
	}
	
	private ThreadPool _threadPool;
	private HashMap<String,SocketChannel> _socketList;//保存SocketChannel到Map中，方便根据uid获取SocketChannel并发送数据
	private Selector selector;
	//定于接受数据的缓冲区大小
	private ByteBuffer _buff=ByteBuffer.allocate(32*1024);//接受数据用的byte数组，系统自带的类，只能用这个接收和发送
	private ByteArray _recvBuff=new ByteArray(32*1024);//自己定义的byte数组类，因为需要自定义数据交互协议，像是加密标记，报文长度等等
	//定于发送数据的缓冲区大小
	private ByteArray _sendBuff=new ByteArray(32*1024);
    
	public void init()
	{
		_socketList=new HashMap<String,SocketChannel>();//创建
		_threadPool=new ThreadPool();
		try {
			selector=Selector.open();//open 方式创建一个用于检测所有Channel的selector
			ServerSocketChannel serverSocket=ServerSocketChannel.open();//打开一个未绑定的ServerSocketChannel实例
			serverSocket.socket().bind(new InetSocketAddress(8050));//绑定端口
			serverSocket.configureBlocking(false);//设置非阻塞模式
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);//把serverSocket注册到指定的selector，设置为accept表示是接收连接
			while(true)
			{
				int selCount = selector.select();
				if(selCount>0)//如果有网络请求，select()返回大于零，否则select()会阻塞
				{
					Iterator<SelectionKey> it=selector.selectedKeys().iterator();
					while(it.hasNext())//遍历网络请求的集合
					{
						SelectionKey key=it.next();
						it.remove();//从selector中移除自身，不然下次请求也会留在selector种
						//selector.selectedKeys().remove(key);//从selector中移除自身，不然下次请求也会留在selector中
						if(key.isAcceptable())//接受socket连接的操作
						{
							System.out.println("new Connect...");
							ServerSocketChannel ssc=(ServerSocketChannel)key.channel();//将通道强转为ServerSocketChannel
							SocketChannel sc=ssc.accept();//调用接受连接的方法，获取服务端与客户端连接的SocketChannel,其实就是调用了ServerSocket.accept()
							sc.configureBlocking(false);//设置非阻塞
							sc.register(selector, SelectionKey.OP_READ);//把SocketChannel注册到selector中，设置为接受数据
							key.interestOps(SelectionKey.OP_ACCEPT);//重新设置该通道状态为接收连接。
						}
						//接受数据接收操作，如果这里用else if 则表示只能接受一种模式。
						//若interestOps()，register()或里设置对多种模式，需改成if，接受另一种模式消息,一般不注册接受多种模式
						else if(key.isReadable())
						{
							
							SocketChannel sc=(SocketChannel) key.channel();//获取对应的SocketChannel
							String uid="";
							if(key.attachment()!=null)
								uid=(String) key.attachment();//获取该通道的uid,如果uid==""，表示还没有绑定uid
							int size = 0;
							try{
								while((size=sc.read(_buff))>0)//读取数据，存入buff,由于buff定义32K，因此一般不用担心会有第二次，如数据超过32k，_recvBuff会自动扩充，继续缓存数据
								{
									if(_recvBuff.IsFull())//缓存满了，动态扩展
										_recvBuff.AutoExpand(true);
									_buff.flip();//初始化buff的读取位置limit和读取索引position
									_recvBuff.WriteBytes(_buff.array(),_buff.limit());
									//content+=charset.decode(buff);//转换为string
									_buff.clear();//必须调用clear方法，否则read无法将数据读取出来存入buff，导致一直有数据未取出，selector中一直有可读取消息的通道
								}
							}
							catch(IOException e) {
								System.out.println("客户端强制关闭连接");
								size=-1;
							}
							recv(_recvBuff,sc,uid);//传入ByteArray数据和sc,recv中对_recvBuff进行校验，主要是粘包处理
							key.interestOps(SelectionKey.OP_READ);//重新设置该通道为接受数据
							
							if(size==-1)//如果客户端socket断开，会触发读操作，返回size=-1，这是需关闭的通道
							{
								if(!uid.equals("")&_socketList.containsKey(uid))//判断改通道是否已绑定uid
									_socketList.remove(uid);//移除uid与通道的绑定
								System.out.println("remove uid:"+uid);
								sc.close();//关闭socketChannel
								key.cancel();//关闭通道
								if(key.channel()!=null)//如果通道没有关闭成功
								{
									key.channel().close();//手动调用通道关闭方法
								}
								continue;//跳过本次循环
							}
							
						}
					}
				}
				//非阻塞的优势就是这里可以做其他操作
			}
			
		} catch (IOException e) {
			System.out.println("ServerSocketChannel通道初始化失败");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * 绑定uid和socket
	 * @param ChannelID 通道id，
	 * @param socket 该用户的SocketChannel
	 */
	public void SetSocketID(SocketChannel socket,String uid)//绑定用户的uid
	{
		socket.keyFor(selector).attach(uid);
		_socketList.put(uid, socket);
	}
	
	/**
	 * 处理接收到的byteArray,如需解压，解密等均在这里处理，此处仅对粘包做了处理
	 * @param buff
	 * @param socket
	 */
	private void recv(ByteArray recvBuff,SocketChannel socket,String uid)
	{
		recvBuff.GetLen();
		int av =recvBuff.GetAvailable();
		while (av>=4) {//这里用循环，是因为可能接收到了多条数据，需要逐条处理
			int packagelen=recvBuff.ReadInt();
			if(av>=packagelen)//如果收到的数据大于包头记录的包体长度，说明数据已经接受完毕
			{
				String data=recvBuff.ReadStringUInt();//ReadStringUInt里面做了编码转换
				_threadPool.SubmitTask(data, socket,uid);//提交任务
				av=recvBuff.GetAvailable();
			}
			else//buff中数据不足，恢复指针位置，进行下次recv
			{
				recvBuff.Offset(-4);
				break;
			}
		}
	}
	
	
	public void pushAll(String msg)//推送给所有连接上的客户端
	{
		for (Map.Entry<String, SocketChannel> entry : _socketList.entrySet()) {  
			String uid=entry.getKey();
			Send(entry.getValue(),msg,uid);
		}  
	}
	public void push(String data,String uid)
	{
		if(_socketList.containsKey(uid))
			Send(_socketList.get(uid),data,uid);
		else 
			System.out.println("uid不存在:"+uid);
	}
	
	public void Send(SocketChannel socket,String data,String uid)
	{
		try {
		_sendBuff.Clear();
		_sendBuff.WriteInt(1);//随便写入一个整形占前4个字节，用于存放包内容长度
		_sendBuff.WriteStringUInt(data.getBytes("UTF-8"));
		_sendBuff.WriteLen(_sendBuff.GetLen()-4);//写入包内容长度。
		socket.write(ByteBuffer.wrap(_sendBuff.GetBuff(), 0, _sendBuff.GetLen()));//发送数据
		} catch (UnsupportedEncodingException e) {
			System.out.println("不支持的编码"+data);
		} catch(IOException e) {
			_socketList.remove(uid);//移除通道号与通道的绑定
			System.out.println("发送失败，uid:"+uid+"消息内容："+data);
			try {
				socket.close();
			} catch (IOException e1) {
				System.out.println("关闭失败");
			}
		}
	}

}
