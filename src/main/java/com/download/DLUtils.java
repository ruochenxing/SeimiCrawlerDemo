package com.download;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class DLUtils {

	/**
	 * 根据contentEncoding对in解码
	 * @param in
	 * @param contentEncoding
	 */
	public static InputStream getInputStream(InputStream in, String contentEncoding) throws IOException {
		if("gzip".equalsIgnoreCase(contentEncoding)) {
			in = new GZIPInputStream(in);
		} else if("deflate".equalsIgnoreCase(contentEncoding)) {
			in = new DeflaterInputStream(in);
		}
		return in;
	}

	public static String readFull(InputStream in, String charset) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
		try {
			byte[] buf = new byte[512];
			int len;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
		} finally {
			in.close();
			out.close();
		}
		return out.toString(charset);
	}
	/**
	 * 获取一个模拟chrome请求的连接
	 * */
	public static HttpURLConnection getChromeURLConnection(String uri, boolean keep, Properties reqProps)
			throws MalformedURLException, IOException {
		URL url = new URL(uri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(10000);
		connection.setUseCaches(false);
		connection.setReadTimeout(10000);
		connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
		connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6,ja;q=0.4");
		connection.setRequestProperty("Cache-Control", "max-age=0");
		if(keep)
			connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Cookie", "");
		connection.setRequestProperty("Host", url.getHost());
		connection.setRequestProperty("Referer", "http://" + url.getHost() + "/");
		connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36");
		
		for (Entry<Object, Object> elem : reqProps.entrySet()) {
			connection.setRequestProperty(String.valueOf(elem.getKey()), String.valueOf(elem.getValue()));
		}
		
		return connection;
	}

	public static void close(Closeable closeable) {
		try {
			if (closeable != null)
				closeable.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getNameForDisposition(String disposition) {
		//"Content-Disposition", "attachment; filename=\""+new String( name.getBytes("utf-8"), "ISO8859-1" )+"\"");
		String content = disposition;
		if(content != null) {
			int pos = content.toLowerCase().indexOf("filename=") + 9;
			content = content.substring(pos);

			pos = content.indexOf('=');
			if(pos > 0) {
				for (int i = pos; 0 < i; i--) {
					if(content.charAt(i) == ' ' || content.charAt(i) == ';') {
						pos = i;
						break;
					}
				}
				return content.substring(0, pos).trim();
			}
			return content.trim();
		}
		return null;
	}

	/**
	 * 检查文件是否存在，若存在则重命名
	 * @param filepath
	 * @return
	 */
	public static String checkFileAndRename(String filepath) {
		File file = new File(filepath);
		if(file.exists()) {
			int i = 1;
			File newFile;
			do {
				newFile = new File(file.getParent() + "/" + getNameNoExt(file.getName()) + (i++) + "." + getFileExt(file.getName()));
			} while (newFile.exists());
			return newFile.getPath();
		}
		return filepath;
	}

	public static String getNameNoExt(String name) {
		return name.substring(0, name.lastIndexOf('.'));
	}

	public static String getFileExt(String name) {
		return name.substring(name.lastIndexOf('.')+1);
	}

	public static void makeEmptyFile(String file) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			fw.flush();
		} finally {
			close(fw);
		}
	}
	
	public static void main(String[] args) throws IOException {
//		System.out.println(getNameForDisposition("attachment; filename=abc.jpg ;type=png"));
//		makeEmptyFile("d:\\aa.txt");
//		System.out.println(getLengthStr((long) (1024*1024*8.2658)));
		System.out.println( "[:*?<>|]".replaceAll("[:*?<>|]", "_"));
	}

	public static void close(HttpURLConnection conn) {
		if(conn != null)
			conn.disconnect();
	}

	public static String getLengthStr(long bytes) {
		if(bytes > 1024*1024*1024) {
			return String.format("%.2fGb", ((double)bytes / 1024  / 1024 / 1024));
		}
		if(bytes > 1024*1024) {
			return String.format("%.2fMb", ((double)bytes / 1024 / 1024));
		}
		return String.format("%.2fKb", ((double)bytes / 1024));
	}

	public static String getPercentStr(long downloaded, long length) {
		return String.format("%.2f%%", (double) downloaded / length * 100);
	}
}
