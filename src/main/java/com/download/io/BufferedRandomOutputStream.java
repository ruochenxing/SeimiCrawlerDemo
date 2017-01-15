package com.download.io;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * 随机访问缓冲输出流
 */
public class BufferedRandomOutputStream extends BufferedOutputStream {

	private RandomAccessFile randomAccessFile;

	public BufferedRandomOutputStream(String file, String mode, int bufSize) throws FileNotFoundException {
		this(new RandomAccessFile(file, mode), bufSize);
	}

	public BufferedRandomOutputStream(RandomAccessFile randomAccessFile, int bufSize) throws FileNotFoundException {
		super(new BufferedOutputStream(new RandomOutputStream(randomAccessFile), bufSize));
		this.randomAccessFile = randomAccessFile;
	}
	
	public final FileDescriptor getFD() throws IOException {
		return randomAccessFile.getFD();
	}
	
	public final FileChannel getChannel() {
		return randomAccessFile.getChannel();
	}

	public void seek(long pos) throws IOException {
		flush();
		randomAccessFile.seek(pos);
	}
	
	public int skipBytes(int n) throws IOException {
		flush();
		return randomAccessFile.skipBytes(n);
	}
	
	public long getFilePointer() throws IOException {
		return randomAccessFile.getFilePointer();
	}
	
	public long length() throws IOException {
		flush();
		return randomAccessFile.length();
	}
	 
	public void setLength(int length) throws IOException {
		flush();
		randomAccessFile.setLength(length);
	}

	static class RandomOutputStream extends OutputStream {
		RandomAccessFile randomAccessFile;

		public RandomOutputStream(RandomAccessFile randomAccessFile) throws FileNotFoundException {
			this.randomAccessFile = randomAccessFile;
		}
		
		@Override
		public void write(int b) throws IOException {
			randomAccessFile.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			randomAccessFile.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			randomAccessFile.write(b, off, len);
		}
		
		@Override
		public void close() throws IOException {
			randomAccessFile.close();
		}
	}

}
