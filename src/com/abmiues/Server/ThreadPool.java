package com.abmiues.Server;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
	private static int _threadPoolSize=3;//指定线程池线程数量为3
	private ExecutorService fixedThreadPool;//定长线程池，超出数量会排队
	
	public ThreadPool()
	{
		fixedThreadPool= Executors.newFixedThreadPool(_threadPoolSize);
	}
	
	/**
	 * 添加任务
	 * @param data 任务数据
	 * @param socket socket通道，数据处理完后可直接用这个返回数据
	 * @param uid 通道上绑定的uid
	 */
	public void SubmitTask(String data,SocketChannel socket,String uid)
	{
		System.out.println("recv:"+data);
		synchronized (TaskPool.Instance()) {//锁住对象池，防止子线程对对象池进行操作
			fixedThreadPool.execute(TaskPool.Instance().Pop(data, socket, uid));//执行任务
		}
	}
	
	

}
