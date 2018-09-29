package com.abmiues;

import java.io.UnsupportedEncodingException;

public class ByteArray {

	private byte[] _buf;
	private int _size;
	private int _len;
	private int _pos;
	public ByteArray(int size)
	{
		_buf=new byte[size];
		_size=size;
		_len=0;
		_pos=0;
	}
	
	/**
	 * 自动调整buff大小
	 * @param needCopyOldData 调整后是否保留原始数据
	 */
	public void AutoExpand(boolean needCopyOldData)
	{
		AutoExpand(needCopyOldData,0);
	}
	/**
	 * 自动调整buff大小
	 * @param needCopyOldData 调整后是否保留原始数据
	 * @param targetSize 目标大小
	 */
	public void AutoExpand(boolean needCopyOldData,int targetSize)
	{
		int newSize;
		if(targetSize==0)
		{
			newSize=_size*2;
		}
		else
		{
			newSize=targetSize;
		}
		if(newSize<=_size)
			return;
		_size=newSize;
		byte[] newbuf=new byte[_size];
		if(needCopyOldData)
			System.arraycopy(_buf, 0, newbuf, 0, _len);
		_buf=newbuf;
	}
	/**
	 * 调整buff空间，移除用过的数据
	 */
	public void Shrink()
	{
		if(_pos>0)
		{
			int av=_len-_pos;
			System.arraycopy(_buf, _pos, _buf, 0, av);
			_pos=0;
			_len=av;
		}
	}
	
	public void Clear()
	{
		_pos=0;
		_len=0;
	}
	public int GetPos()
	{
		return _pos;
	}
	public int GetLen()
	{
		return _len;
	}
	public byte[] GetBuff()
	{
		return _buf;
	}
	public void SetLen(int len)
	{
		_len=len;
	}
	public int GetSize()
	{
		return _size;
	}
	public int GetFreeSize()
	{
		return _size-_len;
	}
	public void Offset(int offset)
	{
		_pos+=offset;
	}

	/**
	 * 返回剩余数据长度
	 * @return
	 */
    public int GetAvailable()
    {
        return _len - _pos;
    }

    
    /*
     * 读取整形, 大端
    */

    public int ReadInt()
    {
        //解包数据
    	int i=byteArrayToInt(_buf,_pos);
        _pos += 4;
        return i;
    }

    /**
     * 写入包总长度
     * @param i
     */
    public void WriteLen(int i)
    {
    	SetInt(0, i);
    }
    
    /**
     * 写入整形类型
     * @param i
     */
    public void WriteInt(int i)
    {
        SetInt(_len, i);
        _len += 4;
    }

    public void SetInt(int offset, int value)
    {
        byte[] b = intToByteArray(value);
        System.arraycopy(b, 0, _buf, offset, 4);
    }


    public boolean IsFull()
    {
        return _len >= _size;
    }

    public void WriteStringUInt(byte[] stringBytes)
    {
        WriteInt(stringBytes.length);
        WriteBytes(stringBytes);
    }


    public void WriteBytes(byte[] bytes)
    {
    	WriteBytes(bytes,bytes.length);
    }
    
    public void WriteBytes(byte[] bytes,int length)
    {
        System.arraycopy(bytes, 0, _buf, _len, length);
        _len += length;
    }

    public String ReadStringUInt()
    {
        int len = ReadInt();
        String ret;
		try {
			ret = new String(_buf,_pos,len,"UTF-8");
			_pos += len;
	        return ret;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.out.println("接收数据发现不支持的编码");//e.printStackTrace();
			return "";
		}
        
    }
    
   
    

    /**
     * int 转byte 大端
     * @param a
     * @return
     */
    
    public static byte[] intToByteArray(int a) {  
        return new byte[] {  
            (byte) (a & 0xFF)  ,
            (byte) ((a >> 8) & 0xFF),     
            (byte) ((a >> 16) & 0xFF),     
            (byte) ((a >> 24) & 0xFF), 
        };  
    } 
	
    public static int byteArrayToInt(byte[] b,int offset) {  
        return  (b[3+offset] & 0xFF) << 24 |  
                (b[2+offset] & 0xFF) << 16 |  
                (b[1+offset] & 0xFF) << 8 |  
                (b[0+offset] & 0xFF) ;  
    }  
	
	
	
	
	
	
	
	
	
	
 }

