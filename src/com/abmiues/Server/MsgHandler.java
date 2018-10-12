package com.abmiues.Server;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.json.JSONObject;

public class MsgHandler implements Runnable {
	SocketChannel _socket;
	String _data;
	String _uid;
	
	
	public void init (String data,SocketChannel socket,String uid) {
		_socket=socket;
		_data=data;
		_uid=uid;
	}
	
	@Override
	public void run() {
		JSONObject jsonObject;
		try{
		 jsonObject=new JSONObject(_data);
		}catch(Exception e)
		{
			System.out.println("不是可解析的json数据："+_data);
			return;
		}
		String func=jsonObject.getString("func");
		if(func.equals("connect"))//链接操作，保存通道
		{
			_uid=jsonObject.getString("uid");
			ServerSocket.Instance().SetSocketID(_socket, _uid);
			ServerSocket.Instance().Send(_socket, "{\"func\":\"connect\",\"data\":\"\"}", _uid);
			System.out.println("get Connect:"+_uid);
			try {
				System.out.println(_socket.getRemoteAddress().toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(func.equals("heartBeat"))//通道的心跳操作，掉线操作暂未实现
		{
			System.out.println("get HeartBeat:"+_uid);
			ServerSocket.Instance().Send(_socket, "{\"func\":\"heartbeat\",\"data\":\"\"}", _uid);
		}
		
		//执行结束，放回对象池。这个必须放在最后执行，且必须执行
		synchronized (TaskPool.Instance()) {
			TaskPool.Instance().Push(this);
		}
	}
}

