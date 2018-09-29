package com.abmiues;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.abmiues.ByteArray;

public class UdpClient {
	   
	int  MaxByteLength=512;//定义最大数据报字节数
    private String sendStr = "SendString";
    private String netAddress = "127.0.0.1";
    InetAddress address;
    private final int SendPort = 4622;
    private final int LocalPort=8030;
    ByteArray sendbuf=new ByteArray(32*1024);
	ByteArray _recvBuff=new ByteArray(32*1024);
   
    private DatagramSocket datagramSocket;
    private DatagramPacket _recvPacket;
    private DatagramPacket _sendPack;
   
    public static void main(String[]args) throws IOException{
    	UdpClient sd=new UdpClient();
		sd.init();
		//sd.init2();
	}
    public void init(){
        try {
        	
        	address=InetAddress.getByName(netAddress);
        	//_sendPack=new DatagramPacket(null, 0, address, PORT_NUM);
        	_recvPacket=new DatagramPacket(new byte[1024],1024);
            datagramSocket=new DatagramSocket(LocalPort);
            new ClientThread().start();
            Send("haha");
            /*** 接收数据***/
            byte[] receBuf = new byte[1024];
            DatagramPacket recePacket = new DatagramPacket(receBuf, receBuf.length);
            datagramSocket.receive(recePacket);
           
            String receStr = new String(recePacket.getData(), 0 , recePacket.getLength());
            System.out.println("Client Rece Ack:" + receStr);
            System.out.println(recePacket.getPort());
           
           
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
    
    
    public void Send(String data)
	{
		try {
			sendbuf.Clear();
			sendbuf.WriteInt(1);//随便写入一个整形占前4个字节，用于存放包内容长度
			sendbuf.WriteStringUInt(data.getBytes("UTF-8"));//写入包内容
			sendbuf.WriteLen(sendbuf.GetLen()-4);//写入包内容长度。
			_sendPack=new DatagramPacket(sendbuf.GetBuff(),0,sendbuf.GetLen(), address, SendPort);
			//_sendPack.setData(sendbuf.GetBuff(),0,sendbuf.GetLen());
			//datagramPacket = new DatagramPacket(sendbuf.GetBuff(),0,sendbuf.GetLen(),  address, PORT_NUM);
			datagramSocket.send(_sendPack);
		} catch (UnsupportedEncodingException e1) {
			System.out.println("不支持的编码："+data);
		}
		catch (IOException e) {
			System.out.println("发送失败："+data);
		}
	}
  //定义读取服务器数据的线程
  	private class ClientThread extends Thread{
  		public void run(){
  			int length=0;
            try {
				datagramSocket.receive(_recvPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           
            String receStr = new String(_recvPacket.getData(), 0 , _recvPacket.getLength());
            System.out.println("Client Rece Ack:" + receStr);
            System.out.println(_recvPacket.getPort());
  			/*try{
  				while((length=datagramSocket.receive(_recvPacket).getLength()>0){
  					if(_recvBuff.IsFull())//缓存满了，动态扩展
  						_recvBuff.AutoExpand(true);
  					_recvBuff.WriteBytes(_buff,length);
  					recv(_recvBuff);
  				}
  			}
  			catch(IOException e) {
  				System.out.println("服务端强制关闭连接");
  				try {
  					outPut.close();
  					inPut.close();
  					socket.close();
  				} catch (IOException e1) {
  					System.out.println("socket关闭失败");
  				}
  				this.interrupt();
  				return;
  			}*/
  		}
  	}
}
