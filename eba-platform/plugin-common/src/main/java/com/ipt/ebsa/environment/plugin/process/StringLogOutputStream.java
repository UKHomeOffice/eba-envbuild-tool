package com.ipt.ebsa.environment.plugin.process;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author James Shepherd
 *
 */
public class StringLogOutputStream extends OutputStream {
	
	/** The logger where to log the written bytes. */
	private PrintStream printStream;

	/** The internal memory for the written bytes. */
	private StringBuilder buffer = new StringBuilder();

	/** stores all the bytes */
	private StringBuilder bufferAll = new StringBuilder();

	/**
	 * Creates a new log output stream which logs to the PrintWriter
	 *
	 * @param pw
	 *            the logger where to log the written bytes
	 */
	public StringLogOutputStream(PrintStream pw) {
		setPrintStream(pw);
	}

	/**
	 * Takes a byte, coverts it to a String and buffers it.
	 *
	 * @param b 
	 */
	public void write(int b) {
		byte[] bytes = new byte[1];
		bytes[0] = (byte) (b & 0xff);
		String oneChar = new String(bytes);
		buffer.append(oneChar);

		if (oneChar.equals("\n")) {
			flush();
		}
	}

	/**
	 * Flushes the output stream, adding stuff to the buffer for {@link #getOutput()}
	 */
	public void flush() {
		printStream.print(buffer.toString());
		bufferAll.append(buffer);
		buffer.delete(0, buffer.length());
	}

	/**
	 * @return current unbuffered output
	 */
	public String getOutput() {
		return bufferAll.toString();
	}

	public PrintStream getPrintStream() {
		return printStream;
	}

	public void setPrintStream(PrintStream printStream) {
		this.printStream = printStream;
	}
}
