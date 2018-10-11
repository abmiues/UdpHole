package com.abmiues;

import java.io.IOException;

public class Test {
	public boolean wait=false;
	public Object lock=new Object();
	public static void main(String[]args) throws IOException{
		Test tt=new Test();
		tt.new subClass().start();
		tt.doWhile();
	}
	public void doWhile()
	{
		synchronized (lock) {
			while (!wait) {
				try {
					System.out.println("wait");
					lock.wait();
					System.out.println("finishwait");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	class subClass extends Thread
	{
		public void run(){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			synchronized (lock){
				wait=true;
				//lock.notify();
			}
    	}
	}
}
