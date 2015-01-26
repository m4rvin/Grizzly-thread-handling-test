package it.uniroma2.sdcc.file_transfer;

import java.io.IOException;

public class Main {

	private static int N_CLIENT = 10;
	
	
	public static void main(String[] args)
	{
		
		GrizzlyServer gserver = new GrizzlyServer("localhost", 9027);
		gserver.start();
		
		String src_folder_path = "/home/m4rvin/";
		String filename = "resteasytest.zip";
		
		long start = System.currentTimeMillis();
		Thread clients[] = new Thread[N_CLIENT];
		for(int i=0; i<N_CLIENT; i++)
		{
			clients[i] = buildClient(src_folder_path + filename, i+".zip");
			clients[i].start();
		}
		
		for(int i=0; i<N_CLIENT; i++)
		{
			try 
			{
				clients[i].join();
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
			
		System.out.println("transfer " + N_CLIENT + "files in " + (System.currentTimeMillis()-start) + " ms");
		
		try {
			System.out.println("Press any key to stop the server...");
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//server.start();
	}
	
	private static Thread buildClient(final String src_filepath, final String dst_filename)
	{
		return new Thread(new Runnable() {

			public void run() 
			{
				try 
				{
					Thread.sleep(1000);
					System.out.println("transfer start");
					FileMetadata metadata = new FileMetadata();
					metadata.filename = dst_filename;
					//metadata.size_in_bytes = 5377L;
					Client.transferTo(
							"localhost", 
							9027, 
							src_filepath,
							metadata);		
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		});
	}
}
