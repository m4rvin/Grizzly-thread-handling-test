package it.uniroma2.sdcc.file_transfer;

import it.uniroma2.sdcc.file_transfer.ClientStatus.STATE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.memory.HeapBuffer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

public class GrizzlyServer 
{
	private static Logger logger = Logger.getLogger("GrizzlyServer");
	
	private final String hostname;
	private final int port;
	
	 
	
	public GrizzlyServer(String hostname, int port)
	{
		logger.setLevel(Level.OFF);
		//FIXME
		
		
		this.hostname = hostname;
		this.port = port;
	}
	
	private static class BufferToFileFilter extends BaseFilter
	{
		private static final String dst_folder_path = "/home/m4rvin/testSDCC/chunks/";
		
		private Map<String, ClientStatus> clients_status;

		//private String path;
		private AsynchronousFileChannel async_file_ch;
		//private FileChannel sync_file_ch;
		
		
		private String hexEncode(byte[] aInput, int from, int to)
		{
			StringBuilder result = new StringBuilder();
			char[] digits = {'0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f'};
			for (int idx = from; idx < aInput.length && idx<to; ++idx) 
			{
				byte b = aInput[idx];
				result.append(digits[ (b&0xf0) >> 4 ]);
				result.append(digits[ b&0x0f]);
			}
			return result.toString();
		}
		
		public BufferToFileFilter()
		{
			logger.info("BufferToFileFilter()");
			this.clients_status = new ConcurrentHashMap<String, ClientStatus>();
		}
		
		private String getClientId(FilterChainContext ctx)
		{
			return ctx.getConnection().getPeerAddress().toString();
		}
		
		@Override
		public NextAction handleAccept(FilterChainContext ctx)
				throws IOException {
			
			String client_id = getClientId(ctx);
			logger.info("client host = " + client_id +  " connected");
			this.clients_status.put(client_id, new ClientStatus());
			
			//  System.out.println("Thread id = "+ Thread.currentThread().getId() + " ctx = " + ctx.toString());
			
			/*StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			for(StackTraceElement stack_element : stack)
				System.out.println(stack_element);*/
			
			/*ReadResult result = ctx.read();
			
			Buffer buff = (Buffer) result.getMessage();
			//logger.info("client accepted filename = "+result);
			String dst_path = buff.toStringContent();
			logger.info("client accepted filename = "+dst_path);
			
			
			this.sync_file_ch = FileChannel.open(
					Paths.get(dst_path), 
					StandardOpenOption.APPEND, 
					StandardOpenOption.CREATE
					);
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			/*Buffer buff = ctx.getMessage();
			logger.info("client accepted filename = "+buff.toStringContent());
			*/
			return super.handleAccept(ctx);
		}
		
		@Override
		public NextAction handleConnect(FilterChainContext ctx)
				throws IOException {
			
			logger.info("client connected");
			
			return super.handleConnect(ctx);
		}
		
		
		@Override
		public NextAction handleClose(FilterChainContext ctx)
				throws IOException {
			//logger.info("client " + getClientId(ctx) + " has close connection!");
			//  System.out.println("close() Thread id = "+ Thread.currentThread().getId() + " ctx = " + ctx.toString());
			

			return super.handleClose(ctx);
		}
		
		private NextAction handleFileMetadata(FilterChainContext ctx, String client_id) 
				throws IOException
		{
			
			Buffer buff = ctx.getMessage();
			ClientStatus c_status = this.clients_status.get(client_id);
			
			//explicit buffer position
			Integer buff_size = buff.getInt(buff.position());
			logger.info("metadata size = " + buff_size);
			
			
			//explicit offset
			ByteArrayInputStream byte_array = new ByteArrayInputStream(buff.array(), buff.arrayOffset() + buff.position() +4, buff_size);
			ObjectInputStream stream = new ObjectInputStream(byte_array);
			try {
				FileMetadata metadata = (FileMetadata) stream.readObject();
				logger.info("receive metadata = " + metadata.toString());
				System.err.println("buffer retrieved by the server from clientID (containing message size and a serialized JavaObject: FileMetadata)" + client_id + " = " + buff.capacity() + "\n It's a request for file " + metadata.filename);

				/*buff.position(buff.limit());
				buff.clear();
				*/
				//  System.out.println("pos="+ buff.position() + " buff = " + hexEncode(buff.array(), 0, 217));
				c_status.channel = FileChannel.open(
						Paths.get(dst_folder_path + metadata.filename), 
						StandardOpenOption.APPEND, 
						StandardOpenOption.CREATE
						);
				
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
			
			ctx.write(HeapBuffer.wrap(new String("ok").getBytes()));
			c_status.status = STATE.TRANSFER;
			return ctx.getStopAction();
		}
		
		private NextAction handleFileTransfer(FilterChainContext ctx, String client_id) 
				throws IOException
		{
			Buffer buff = ctx.getMessage();
			ClientStatus c_status = this.clients_status.get(client_id);
			c_status.channel.write(buff.toByteBuffer());
			//this.sync_file_ch.write(buff.toByteBuffer());
			return ctx.getStopAction();
		}
		
		@Override
		public NextAction handleRead(final FilterChainContext ctx)
				throws IOException {
			
			//logger.info("handleRead()");	
			String client_id = getClientId(ctx);
			ClientStatus c_status = this.clients_status.get(client_id);
			if(c_status != null)
			{
				switch (c_status.status) {
				case READY:
					return handleFileMetadata(ctx, client_id);
				case TRANSFER:
					return handleFileTransfer(ctx, client_id);
				default:
					break;
				}
			}

			return ctx.getStopAction();
		}

	}
	

	public void start()
	{
		// Create a FilterChain using FilterChainBuilder
		FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		//FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateful();
		
		// Add TransportFilter, which is responsible
		// for reading and writing data to the connection
		filterChainBuilder.add(new TransportFilter());

		// EchoFilter is responsible for echoing received messages
		filterChainBuilder.add(new BufferToFileFilter());

		// Create TCP transport
		
				TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
		//builder.setWorkerThreadPoolConfig(ThreadPoolConfig.defaultConfig());
		//builder.getWorkerThreadPoolConfig().setCorePoolSize(2);
		final TCPNIOTransport transport = builder.build();
		transport.setProcessor(filterChainBuilder.build());

		//transport.
		//logger.info("thread pool size = "+ builder.getWorkerThreadPoolConfig().getCorePoolSize());

		try {
			// binding transport to start listen on certain host and port
			transport.bind(hostname, port);

		
			// start the transport
			transport.start();

			//logger.info("Press any key to stop the server...");
			//System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*finally {
			logger.info("Stopping transport...");
			// stop the transport
			try {
				transport.shutdownNow();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.info("Stopped transport...");
		}*/
	}
}


