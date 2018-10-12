package com.abmiues.Server;
import java.nio.channels.SocketChannel;
import java.util.Stack;

//任务对象池
public class TaskPool{
	private static TaskPool _instance;
	public static TaskPool Instance()
	{
		if(_instance==null)
			_instance=new TaskPool();
		return _instance;
	}
	
	private static int _handlerPoolSize=200;//指定任务的对象池为200
	private static int _expendSize=10;//指定对象池取完时扩充的数量
	private Stack<MsgHandler> _handlerPool;//对象池，设置为静态变量，因为子线程要用到
	
	public TaskPool()
	{
		if(_handlerPool==null)
		{
			_handlerPool=new Stack<MsgHandler>();
			for (int i = 0; i < _handlerPoolSize; i++) {
				_handlerPool.push(new MsgHandler());//构建初始数量为_handlerPoolSize的对象池
			}
		}
	}
	
	public MsgHandler Pop(String data,SocketChannel socket,String uid)
	{
		if(_handlerPool.empty())//如果对象池空了
			ExpendObjectPool();//扩充对象池
		MsgHandler handler=_handlerPool.pop();//从对象池中取出一个
		handler.init(data, socket, uid);//配置数据
		return handler;
	}
	
	public void Push(MsgHandler handler)
	{
		_handlerPool.push(handler);
	}
	
	private void ExpendObjectPool()
	{
		for (int i = 0; i < _expendSize; i++) {
			_handlerPool.push(new MsgHandler());//扩充对象池
		}
	}
	
}


