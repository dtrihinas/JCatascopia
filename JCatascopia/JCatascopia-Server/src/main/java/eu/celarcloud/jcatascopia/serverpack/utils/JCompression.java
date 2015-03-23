package eu.celarcloud.jcatascopia.serverpack.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JCompression {
	
	public static byte[] encode(String s) throws IOException {
		if (s == null || s.length() == 0) 
			return null;
		
		ByteArrayOutputStream c = new ByteArrayOutputStream(s.length());
		GZIPOutputStream gzip = new GZIPOutputStream(c);
		gzip.write(s.getBytes());
		gzip.close();
		
		return c.toByteArray();
	}
	
	public static String decode(byte[] s) {
		if (s == null || s.length == 0) 
			return null;
		
        String line, out = "";
		try {
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(s));
			BufferedReader b = new BufferedReader(new InputStreamReader(gis));
	        while ((line=b.readLine())!= null)
	          out += line;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
        return out;
	}
}
