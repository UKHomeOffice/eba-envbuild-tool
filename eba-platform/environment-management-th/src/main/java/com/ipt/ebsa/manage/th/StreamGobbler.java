package com.ipt.ebsa.manage.th;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * safe way to gobble output - mostly copied from the internet
 * @author scowx
 *
 */
public class StreamGobbler extends Thread {
	private static String LS = System.getProperty("line.separator");
	private InputStream is = null;
	private StringBuffer s = null;
	private PrintStream logger = null;
    /**
     * Reads everything from is until empty. 
     * @param is
     * @param s
     */
    StreamGobbler(InputStream is, StringBuffer s) {
        this.is = is;
        this.s = s;
    }
    
    /**
     * Reads everything from is until empty. 
     * @param is
     * @param s
     */
    StreamGobbler(InputStream is, StringBuffer s, PrintStream logger) {
        this.is = is;
        this.s = s;
        this.logger = logger;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
				s.append(line + LS);
				System.out.println(line);
				if (logger != null) {
					logger.println(line);
				}
			}    
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }

	public StringBuffer getS() {
		return s;
	}

	public void setS(StringBuffer s) {
		this.s = s;
	}
    
}
