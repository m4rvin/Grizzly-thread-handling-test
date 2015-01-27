package it.uniroma2.sdcc.file_transfer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class Client 
{

	public static void transferTo(String hostname, 
			int port, String src_filename, FileMetadata metadata) throws IOException
	{
		SocketAddress sad = new InetSocketAddress(hostname, port);
		SocketChannel sc = SocketChannel.open();
		sc.connect(sad);
		sc.configureBlocking(true);
		FileChannel fc = new FileInputStream(src_filename).getChannel();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutput out = new ObjectOutputStream(bos);
	    out.writeObject(metadata);
		
	    int object_size = bos.size();
	    System.out.println("obj size = " + object_size);
		ByteBuffer buff = ByteBuffer.allocate(object_size + 4); 
		buff.putInt(object_size);
		buff.put(bos.toByteArray());
		buff.position(0);
		
		System.out.println("message length = " + buff.remaining());
		int written = 0;
		while(buff.hasRemaining())
			written += sc.write(buff);
		System.err.println("written bytes from clientID " + sc.getLocalAddress() + " =" + written + " (==size of the serialized JavaObject + the FileMetadata object), asking permission to write file: " + metadata.filename);
		
		buff.clear();
		
		System.out.println("client waiting for server ack");
		sc.read(buff);
		System.out.println("receive ack from server " + buff.toString());
	
		long start = System.currentTimeMillis();
		long curnset = 0;
		curnset =  fc.transferTo(0, fc.size(), sc);
		System.out.println("total bytes transferred--"+curnset+" and time taken in MS--"+(System.currentTimeMillis() - start));
		fc.close();
		sc.close();
	}

	
}
