package com.abmiues;

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

import com.abmiues.ByteArray;

/**
 * @author Administrator
 *
 */
public class Client {
	
	public Client() {
		//ack包写入msgid,ack包是0，作为特殊标记，可以不需要数据报id，包长度
		System.arraycopy(ByteArray.intToByteArray(1), 0, _ackbuf, 0, 4);
		
		//固定长度的包写入数据报长度
		System.arraycopy(_standSizebuf,8,ByteArray.intToByteArray(onceByteLength),0,4);
	}
	
	int msgid=2;//起始msgid为2，msgid=1的是ack专用的
	
	int  MaxByteLength=512;//定义最大数据报字节数
    private String sendStr = "SendString";
    private String netAddress = "127.0.0.1";
    InetAddress address;
    private final int SendPort = 8031;
    private final int LocalPort=8030;
    ByteArray _sendbuf=new ByteArray(32*1024);
	ByteArray _recvBuff=new ByteArray(32*1024);
   
    private DatagramSocket datagramSocket;
    private DatagramPacket _recvPacket;
    private DatagramPacket _sendPack;
    private DatagramPacket _ackPack;
   
    public static void main(String[]args) throws IOException{
    	Client sd=new Client();
    	sd.init();
		//sd.init2();
	}
    public void init(){
        try {
        	
        	address=InetAddress.getByName(netAddress);//设置接收方地址
        	_sendPack=new DatagramPacket(_ackbuf,12);//创建一个可复用的发送包
        	_ackPack=new DatagramPacket(_ackbuf,12);//创建一个可复用的响应包
        	_recvPacket=new DatagramPacket(_ackbuf,12);//创建一个可复用的接收包
        	//datagramSocket.setReuseAddress(true);//设置端口复用
            datagramSocket=new DatagramSocket(LocalPort);//创建socket，绑定端口
            new ClientThread().start();//开启接收线程
           
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭socket
            if(datagramSocket != null){
                datagramSocket.close();
            }
        }
    } 
    
    public void SetPackAddress(InetAddress address,int port) {
    	_sendPack.setAddress(address);
		_sendPack.setPort(port);
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
   			_sendbuf.WriteInt(1);//随便写入一个整形占前4个字节，用于存放包内容长度
   			_sendbuf.WriteStringUInt(data.getBytes("UTF-8"));//写入包内容
   			_sendbuf.WriteLen(_sendbuf.GetLen()-4);//写入包内容长度。
   			DoSend();
   			
   		} catch (UnsupportedEncodingException e1) {
   			System.out.println("不支持的编码："+data);
   		}
   	}
    
    int _maxReSendTimes=4;//最大重传次数
    int _reSendCount=0;//当前重传次数
    int _maxWaitTime=12;//最大超时
    int _reSendTime=3000;//定义重发时间
    int _sendTimes=0;//第几次发送数据
	int _sendPiece=-1;//本次发送数据报的编号
	int _ackId=-1;//收到的数据报编号
	int _dataRemain=0;//本次剩余数据量
	int _offset=0;
	byte[] _dynamicSizeBuf=new byte[512];//动态长度的发送buf，只有在发送当前数据的最后一条时才会使用
	byte[] _standSizebuf=new byte[512];//标准尺寸的发送buf
    static final int onceByteLength=500;//最大值512，剔除一个发送次数、剔除一个数据报序号和一个数据长度，剩500字节
    
    /**
     * 拆包，发送，超时重发，超时停止发送
     */
    private void DoSend() {
    	_sendTimes++;
    	System.arraycopy(_standSizebuf,0,ByteArray.intToByteArray(_sendTimes),0,4);//写入发送次数
    	System.arraycopy(_dynamicSizeBuf,0,ByteArray.intToByteArray(_sendTimes),0,4);//写入发送次数
    	
    	synchronized (datagramSocket) {
    		
    		_dataRemain=_sendbuf.GetLen();//计算要发送的数据量
    		while (_dataRemain>0)//如果剩余数据>0，发送 
    		{
        		if(_ackId==_sendPiece)//如果收到的序号和发送的序号相同，开始发送下一序号
        		{
        			_reSendCount=0;
        			_reSendTime=(int) Math.ceil(_reSendTime*0.9f);//如果成功接收，缩短等待时间
        			_sendPiece++;//自增，取下一份数据报
        			_offset=_sendPiece*onceByteLength;//数据取了多少
        			_dataRemain-=_offset;
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
            			_sendPack.setData(_dynamicSizeBuf,0,_dataRemain);
            		}
        		}
        		else {
        			_reSendTime*=2;//如果等待过后，依然没收到，延长等待时间，下面重新发送
        			if(_reSendTime>_maxWaitTime)
        				_reSendTime=_maxWaitTime;
        			if(_reSendCount>_maxReSendTimes)//超过了重发次数
        			{
        				_dataRemain=0;//剩余长度为0
        				throw new InternalError("data id:"+_sendTimes+"send out of time");//抛出超时异常
        			}
        			_reSendCount++;
    			}
        		try {
        			datagramSocket.send(_sendPack);//发送数据
        			
        			//短暂休眠，等待返回数据，用wait可以在收到数据的时候调用notify马上恢复线程，如果收到数据，就等待超时后重发，并延长等待时间
        			datagramSocket.wait(_reSendTime);
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
			datagramSocket.send(_ackPack);
		} catch (IOException e) {
			System.out.println("send ack fail address:"+address.toString()+" port:"+port);
			e.printStackTrace();
		}
    	    	
	}
    
    
    byte[] _recvbuf=new byte[512];
  //定义读取服务器数据的线程
  	private class ClientThread extends Thread{
  		public void run(){
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
            	synchronized(datagramSocket)
            	{
            		_ackId=ByteArray.byteArrayToInt(_recvbuf, 8);
            		System.out.println("收到ack");
            		datagramSocket.notify();//继续发送线程
            	}
            }
            else
            {
            	int packid=ByteArray.byteArrayToInt(_recvbuf, 4);
            	SendAck(_recvPacket.getAddress(), _recvPacket.getPort(), msgid, packid);
            	String receStr = new String(_recvbuf, 12 , ByteArray.byteArrayToInt(_recvbuf, 8));
            	System.out.println("recv data:" + receStr);
            }
            
  		}
  	}
}
