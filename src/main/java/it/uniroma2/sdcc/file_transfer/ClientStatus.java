package it.uniroma2.sdcc.file_transfer;

import java.nio.channels.FileChannel;


public class ClientStatus 
{
	public enum STATE 
	{
		READY, TRANSFER
	}
	public FileChannel channel;
	public STATE status;
	
	public ClientStatus()
	{
		status = STATE.READY;
	}
}
