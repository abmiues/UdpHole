package com.abmiues;

public class UnPackData {
	int  _pieceSize=512;//定义每个数据报最大长度
	int  _dataSize=0;//数据报数量
	int  _pieceId=0;//当前数据存到第几个片段
	int  _pos=0;//当前数据报长度
	byte[][] _bufs;//数据报数组
	
	public UnPackData(int dataSize)
	{
		_bufs=new byte[dataSize][_pieceSize];//定义数据包数组
		_pos=0;
		_pieceId=0;
		_dataSize=dataSize;
	}
	public void AutoExpand(boolean needCopyOldData)
	{
		AutoExpand(needCopyOldData,0);
	}
	public void AutoExpand(boolean needCopyOldData,int targetSize)
	{
		int newSize;
		if(targetSize==0)
		{
			newSize=_dataSize*2;
		}
		else
		{
			newSize=targetSize;
		}
		if(newSize<=_dataSize)
			return;
		byte[][] newbuf=new byte[newSize][_pieceSize];//新建数据数组
		if(needCopyOldData)//如果需要拷贝数据
		{
			for(int i=0;i<_dataSize;i++)//遍历每个数据报,开始拷贝数据报
			{
				
				System.arraycopy(_bufs[i], 0, newbuf[i], 0, i==_dataSize-1?_pos:_pieceSize);
			}
		}
		_dataSize=newSize;
		_bufs=newbuf;
	}
	
}
