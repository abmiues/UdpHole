package com.abmiues.Client.UDP;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.abmiues.Client.ByteArray;

/**
 * @author Administrator
 *
 */
public class UdpClient {

	public UdpClient() {
		//ack包写入msgid,ack包是0，作为特殊标记，可以不需要数据报id，包长度
		System.arraycopy(ByteArray.intToByteArray(1), 0, _ackbuf, 0, 4);

		//固定长度的包写入数据报长度
		System.arraycopy(ByteArray.intToByteArray(onceByteLength),0,_standSizebuf,8,4);
	}

	int msgid=2;//起始msgid为2，msgid=1的是ack专用的

	int  MaxByteLength=512;//定义最大数据报字节数
	private String sendStr = "sendstr";
	private String netAddress = "127.0.0.1";
	InetAddress address;
	private final int SendPort = 8030;
	private final int LocalPort=8031;
	ByteArray _sendbuf=new ByteArray(32*1024);
	ByteArray _recvBuff=new ByteArray(32*1024);

	private DatagramSocket datagramSocket;
	private DatagramPacket _recvPacket;
	private DatagramPacket _sendPack;
	private DatagramPacket _ackPack;

	public static void main(String[]args) throws IOException{
		UdpClient sd=new UdpClient();
		sd.init();
		//sd.init2();
	}
	public void init(){
		try {

			address=InetAddress.getByName(netAddress);//设置接收方地址
			_sendPack=new DatagramPacket(_ackbuf,12);//创建一个可复用的发送包
			_ackPack=new DatagramPacket(_ackbuf,12);//创建一个可复用的响应包
			_recvPacket=new DatagramPacket(_recvbuf,_recvbuf.length);//创建一个可复用的接收包
			datagramSocket=new DatagramSocket(LocalPort);//创建socket，绑定端口
			datagramSocket.setReuseAddress(true);//设置端口复用
			new ClientThread().start();//开启接收线程
			/*SetPackAddress(address,SendPort);
			sendStr="";
			for(int i=0;i<2048;i++)
			{
				sendStr+=i+" ";
			}
			System.out.println(sendStr);
			Send(sendStr);
			*/
			//Thread.sleep(10000);
			//Send("bb");
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	} 
	
	
	public void Loop()
	{
		while (true) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 调用响应的发送方法用于发送数据包给请求连接的客户端，在net上打洞
	 * @param address 服务器给的请求方ip
	 * @param port 服务器给的请求方host
	 */
	public void  OpenHole(InetAddress address,int port) {
		SendAck(address,port,0,0);
	}
	
	public void  SendConnect(InetAddress address,int port) {
		SetPackAddress(address, port);
	}
	
	private void SetPackAddress(InetAddress address,int port) {
		_sendPack.setAddress(address);
		_sendPack.setPort(port);
		Send("connect");
	}

	public void Send(byte[] data,int length) {
		
	}
	public void Send(File file) {

	}

	/**
	 * 发送数据，将数据写入_sendbuf,调用DoSend读取_sendbuf,拆包并发送
	 * @param data
	 */
	public void Send(String data)
	{
		try {
			_sendbuf.Clear();
			//_sendbuf.WriteInt(1);//随便写入一个整形占前4个字节，用于存放包内容长度
			_sendbuf.WriteStringUInt(data.getBytes("UTF-8"));//写入包内容
			//_sendbuf.WriteLen(_sendbuf.GetLen()-4);//写入包内容长度。
			DoSend();

		} catch (UnsupportedEncodingException e1) {
			System.out.println("不支持的编码："+data);
		}
	}

	int _maxReSendTimes=4;//最大重传次数
	int _reSendCount=0;//当前重传次数
	int _maxWaitTime=10000;//最大超时
	int _reSendTime=3000;//定义重发时间
	int _msgid=2;//第几次发送数据,0是缺省值，1是ack特殊标记，所以需要从2开始
	int _sendPiece=-1;//本次发送数据报的编号
	int _ackId=-1;//收到的数据报编号
	int _dataRemain=0;//本次剩余数据量
	int _offset=0;
	int _dataLength=0;
	byte[] _dynamicSizeBuf=new byte[512];//动态长度的发送buf，只有在发送当前数据的最后一条时才会使用
	byte[] _standSizebuf=new byte[512];//标准尺寸的发送buf
	static final int onceByteLength=500;//最大值512，剔除一个发送次数、剔除一个数据报序号和一个数据长度，剩500字节
	/**
	 * 拆包，发送，超时重发，超时停止发送
	 * 单个数据报格式为 0-3：msgid,4-7:pieceid,8-11:length，12-512：data
	 * msgid 为数据id，pieceid为此数据拆片后的片段id，length为此片段长度，data为实际数据，也就是自定义的数据报有12字节的开头
	 */
	private void DoSend() {
		_msgid++;
		System.arraycopy(ByteArray.intToByteArray(_msgid),0,_standSizebuf,0,4);//写入发送次数
		System.arraycopy(ByteArray.intToByteArray(_msgid),0,_dynamicSizeBuf,0,4);//写入发送次数

		synchronized (_sendPack) {

			_dataLength=_sendbuf.GetLen();//计算要发送的数据量
			System.out.println("length:"+_dataLength);
			while (true)
			{
				if(_ackId==_sendPiece)//如果收到的序号和发送的序号相同，开始发送下一序号
				{
					_reSendCount=0;
					_reSendTime=(int) Math.ceil(_reSendTime*0.9f);//如果成功接收，缩短等待时间
					_sendPiece++;//自增，取下一份数据报
					_offset=_sendPiece*onceByteLength;//数据取了多少
					_dataRemain=_dataLength-_offset;
					if(_dataRemain<=0)//如果剩余数据<0，跳出 
					{
						System.out.println("已发送完成");
						break;
					}
					if(_dataRemain>onceByteLength)//如果大于最大单次发送数据量，按最大单次发送量发送计算,也就是500长度，用标准尺寸的buf
					{
						System.arraycopy(ByteArray.intToByteArray(_sendPiece),0,_standSizebuf,4,4);//写入数据报编号
						System.arraycopy(_sendbuf.GetBuff(), _offset, _standSizebuf, 12,onceByteLength);//写入数据
						_sendPack.setData(_standSizebuf);
					}
					else
					{
						System.arraycopy(ByteArray.intToByteArray(_sendPiece),0,_dynamicSizeBuf,4,4);//写入数据报编号
						System.arraycopy(ByteArray.intToByteArray(_dataRemain),0,_dynamicSizeBuf,8,4);//写入数据报长度
						System.arraycopy( _sendbuf.GetBuff(),_offset, _dynamicSizeBuf, 12,_dataRemain);//写入数据
						_sendPack.setData(_dynamicSizeBuf,0,_dataRemain+12);//+12 是数据报开头的12个长度
					}
				}
				else {
					System.out.println("重发次数 "+_reSendCount);
					_reSendTime*=2;//如果等待过后，依然没收到，延长等待时间，下面重新发送
					if(_reSendTime>_maxWaitTime)
						_reSendTime=_maxWaitTime;
					if(_reSendCount>_maxReSendTimes)//超过了重发次数
					{
						_dataRemain=0;//剩余长度为0
						System.out.println("超时断开");
						throw new InternalError("msg:"+_msgid+"send out of time");//抛出超时异常
					}
					_reSendCount++;
				}
				try {
					System.out.println("send:"+_sendPiece);
					datagramSocket.send(_sendPack);//发送数据
					//短暂休眠，等待返回数据，用wait可以在收到数据的时候调用notify马上恢复线程，如果收到数据，就等待超时后重发，并延长等待时间
					_sendPack.wait(_reSendTime);
					System.out.println("收到数据，开始下一次发送");
					//Thread.sleep(_reSendTime);//短暂休眠，等待返回数据
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}


	/**
	 * ack包就12字节，3个int。
	 * 0-4字节是msgid,和普通数据包一样，但固定值是0，用于区分普通包和ack包
	 * 后面两位是消息id和数据报id
	 */
	byte[] _ackbuf=new byte[12];
	private void SendAck(InetAddress address,int port,int msgId,int packid) {
		System.arraycopy(ByteArray.intToByteArray(msgId),0,_ackbuf,4,4);//写入响应的msgid
		System.arraycopy(ByteArray.intToByteArray(packid),0,_ackbuf,8,4);//写入包id
		_ackPack.setAddress(address);
		_ackPack.setPort(port);
		_ackPack.setData(_ackbuf);
		try {
			synchronized (datagramSocket) {
				datagramSocket.send(_ackPack);
			}
		} catch (IOException e) {
			System.out.println("send ack fail address:"+address.toString()+" port:"+port);
			e.printStackTrace();
		}

	}

	int _currentMsgId=0;
	byte[] _recvbuf=new byte[512];
	int _currentPackId=-1;//当前接受到的数据报id，防止重复接受相同数据报
	//定义读取服务器数据的线程
	private class ClientThread extends Thread{
		public void run(){
			while(true){
				try {
					datagramSocket.receive(_recvPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int length=_recvPacket.getLength();
				System.arraycopy(_recvPacket.getData(), 0, _recvbuf, 0,length);
				int msgid=ByteArray.byteArrayToInt(_recvbuf, 0);
				if(msgid==1)//如果是ack数据
				{
					synchronized(_sendPack)
					{
						_ackId=ByteArray.byteArrayToInt(_recvbuf, 8);
						System.out.println("收到ack");
						_sendPack.notify();//继续发送线程
					}
				}
				else if(msgid>0)
				{
					int packid=ByteArray.byteArrayToInt(_recvbuf, 4);//读取片段id

					if(_currentMsgId!=msgid)//如果msgid发生变化，重置packid
					{
						_currentMsgId=msgid;
						_currentPackId=-1;
						_recvBuff.Clear();
					}
					if(_currentPackId!=packid)//如果是未接收过的片段
					{
						int packLength=ByteArray.byteArrayToInt(_recvbuf, 8);
						if(packLength<=4)
						{
							_currentMsgId=0;
							System.out.println("收到数据量不足 msgid:" + msgid);
						}
						else
						{
							_recvBuff.WriteBytes(_recvbuf,12, packLength);//存入data
							//String receStr = new String(_recvbuf, 12 , ByteArray.byteArrayToInt(_recvbuf, 8));
							//System.out.println("recv data:" + receStr);
						}
						_currentPackId=packid;//记录当前片段已接收
						//System.out.println("length:"+ByteArray.byteArrayToInt(_recvBuff.GetBuff(), 0)+":"+(_recvBuff.GetLen()-4));
						SendAck(_recvPacket.getAddress(), _recvPacket.getPort(), msgid, packid);
						if(ByteArray.byteArrayToInt(_recvBuff.GetBuff(), 0)<=_recvBuff.GetLen()-4)//数据接收完毕
						{

							_currentPackId=-1;
							
							String ss=_recvBuff.ReadStringUInt();
							System.out.println(ss);
							//TODO 抛出数据
						}
					}
				}
				else {
					System.out.println("收到异常数据 msgid:" + msgid);
				}

			}
		}
	}
}
