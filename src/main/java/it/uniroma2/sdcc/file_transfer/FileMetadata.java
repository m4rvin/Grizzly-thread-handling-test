package it.uniroma2.sdcc.file_transfer;

import java.io.Serializable;

public class FileMetadata implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8209508583389357605L;
	public String filename;
//	public Long size_in_bytes;
	@Override
	public String toString() {
		//return "FileMetadata [filename=" + filename + ", size_in_bytes="
				//+ size_in_bytes + "]";
		return "FileMetadata [filename=" + filename +"]";
	}
	
	
	
	
	
}
