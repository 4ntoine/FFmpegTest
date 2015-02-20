package com.splinex.tsmuxlib.test;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.splinex.tsmuxlib.ElementaryStreamDescription;
import com.splinex.tsmuxlib.ElementaryStreamPacketMetadata;
import com.splinex.tsmuxlib.ElementaryStreamPacketizer;
import com.splinex.tsmuxlib.MpegTsMultiplexer;
import com.splinex.tsmuxlib.MpegTsPacketMetadata;
import com.splinex.tsmuxlib.MpegTsPacketizer;
import com.splinex.tsmuxlib.Utils;

public class Main {

	private static final short[] PACKET_IDENTIFIERS = { 34, 35 };
	private static final byte BASE_ID_FOR_VIDEO_STREAMS = (byte) 0xe0;

	private static byte[] MDAT = { 'm', 'd', 'a', 't' };
	private static byte[] AVCC = { 'a', 'v', 'c', 'C' };
	private static byte[] STTS = { 's', 't', 't', 's' };

	private static void quit(int exitCode) {
		try {
			System.exit(exitCode);
		} catch (SecurityException e) {
		}
	}

	private static void findBox(InputStream input, byte[] name)
			throws IOException {
		int c;
		while ((c = input.read()) >= 0) {
			if (c == name[0] && input.read() == name[1]
					&& input.read() == name[2] && input.read() == name[3])
				break;
		}
		if (c < 0) {
			System.err.println("Box \"" + (new String(name, "US-ASCII"))
					+ "\" not found!");
			throw new EOFException();
		}
	}

	private static int avcConfigurationsParsed;

	private static void readToStreamStart(InputStream input) throws IOException {
		findBox(input, MDAT);
		byte[] buf = new byte[8];
		if (input.read(buf) >= 8)
			return;
		System.err
				.println("No streams have been found in one of the input files!");
		throw new EOFException();
	}

	private static byte[] getNextNalUnit(InputStream input) throws IOException {
		if (avcConfigurationsParsed < 2) {
			++avcConfigurationsParsed;
			return getParameterSets(input);
		}
		byte[] buffer = new byte[4];
		if (input.read(buffer) < 4)
			return null;
		long size = Utils.uint32ToLong(buffer, 0);
		buffer = new byte[4 + (int) size];
		buffer[3] = 1; // prefix
		if (input.read(buffer, 4, (int) size) < size)
			return null;
		return buffer;
	}

	private static void readParameterSets(InputStream input,
			ArrayList<Byte> result, int count) throws IOException {
		for (int i = 0; i < count; i++) {
			byte[] buffer = new byte[2];
			if (input.read(buffer) < 2)
				throw new EOFException();
			int len = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);
			result.ensureCapacity(result.size() + len + 4);
			for (int j = 0; j < 3; j++)
				result.add((byte) 0);
			result.add((byte) 1); // prefix
			buffer = new byte[len];
			if (input.read(buffer) < len)
				throw new EOFException();
			for (int j = 0; j < len; j++)
				result.add(buffer[j]);
		}
	}

	private static byte[] getParameterSets(InputStream input)
			throws IOException {
		findBox(input, AVCC);
		byte[] buffer = new byte[6];
		if (input.read(buffer) < 6)
			throw new EOFException();
		int spsCount = buffer[5] & 0x1f;
		ArrayList<Byte> result = new ArrayList<Byte>();
		readParameterSets(input, result, spsCount);
		int ppsCount = input.read();
		if (ppsCount < 0)
			throw new EOFException();
		readParameterSets(input, result, ppsCount);
		buffer = new byte[result.size()];
		for (int i = 0; i < buffer.length; i++)
			buffer[i] = result.get(i);
		readToStreamStart(input);
		return buffer;
	}
	
	private static FileInputStream inputTs[] = new FileInputStream[2];

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: java -cp . "
					+ "com.splinex.tsmuxlib.test.Main"
					+ " INPUT1.mp4 INPUT2.mp4 OUTPUT.ts");
			quit(3);
			return;
		}
		FileInputStream input1 = null, input2 = null;
		FileOutputStream output = null;
		MpegTsMultiplexer multiplexer = null;
		try {
			try {
				input1 = new FileInputStream(args[0]);
				inputTs[0] = new FileInputStream(args[0]);
				input2 = new FileInputStream(args[1]);
				inputTs[1] = new FileInputStream(args[1]);
			} catch (Exception e) {
				System.err.println("At least one of the input files could not"
						+ " be opened!");
				quit(1);
				return;
			}
			try {
				output = new FileOutputStream(args[2]);
			} catch (Exception e) {
				System.err.println("Cannot open the output file!");
				quit(1);
				return;
			}
			byte[] nalu1 = null, nalu2 = null;
			ElementaryStreamPacketizer esPacketizer1 = new ElementaryStreamPacketizer();
			ElementaryStreamPacketizer esPacketizer2 = new ElementaryStreamPacketizer();
			MpegTsPacketizer tsPacketizer = new MpegTsPacketizer(
					MpegTsMultiplexer.PROGRAM_CLOCK_REFERENCE_PACKET_ID, 64,
					3795);
			ElementaryStreamDescription esDescription1 = new ElementaryStreamDescription(
					PACKET_IDENTIFIERS[0]), esDescription2 = new ElementaryStreamDescription(
					PACKET_IDENTIFIERS[1]);
			ArrayList<ElementaryStreamDescription> esDescriptions = new ArrayList<ElementaryStreamDescription>();
			esDescriptions.add(esDescription1);
			esDescriptions.add(esDescription2);
			multiplexer = new MpegTsMultiplexer(esDescriptions);
			byte[] buffer = new byte[2 * MpegTsPacketizer.PACKET_LENGTH];
			int c = multiplexer.read(buffer);
			assert (c == buffer.length);
			assert (buffer[5] == 0); // PAT
			output.write(buffer);
			while (((((nalu1 = getNextNalUnit(input1)) != null) ? 1 : 0) | (((nalu2 = getNextNalUnit(input2)) != null) ? 1
					: 0)) != 0) {
				if (nalu1 != null) {
					packetizeNalUnit(nalu1, esPacketizer1, tsPacketizer,
							esDescription1, 0);
				}
				if (nalu2 != null) {
					packetizeNalUnit(nalu2, esPacketizer2, tsPacketizer,
							esDescription2, 1);
				}
				byte[] tsPacket;
				int i = 0;
				while ((tsPacket = tsPacketizer.getNextPacket()) != null) {
					i += multiplexer.add(tsPacket);
				}
				byte[] buf = new byte[MpegTsPacketizer.PACKET_LENGTH * i];
				c = multiplexer.read(buf);
				assert (c == buf.length);
				output.write(buf);
			}
		} catch (IOException e) {
			System.err.println("I/O error!");
		} finally {
			try {
				input1.close();
			} catch (IOException e) {
				System.err
						.println("I/O error while closing the first input file!");
			}
			try {
				input2.close();
			} catch (IOException e) {
				System.err
						.println("I/O error while closing the second input file!");
			}
			try {
				if (output != null)
					output.close();
			} catch (IOException e) {
				System.err.println("I/O error while closing the output file!");
			}
			if (multiplexer != null)
				multiplexer.close();
		}
	}
	
	private static long[] currentTimestampCount = new long[2],
			sameDuration = new long[2], duration = new long[2],
			previousTimestamp = new long[2];
	
	private static long getNextTimestamp(InputStream input, int i) throws IOException {
		if (currentTimestampCount[i]++ == 0) {
			findBox(input, STTS);
			byte[] buf = new byte[8];
			int c = input.read(buf);
			assert(c == 8);
		}
		if (sameDuration[i]-- != 0) {
			assert(duration[i] > 0);
			long timestamp = previousTimestamp[i];
			previousTimestamp[i] += duration[i];
			System.err.printf("timestamp:%d - %d\n", i, timestamp);
			return timestamp;
		}
		byte[] buffer = new byte[8];
		int c = input.read(buffer);
		assert(c == 8);
		sameDuration[i] = Utils.uint32ToLong(buffer, 0);
		duration[i] = Utils.uint32ToLong(buffer, 4);
		assert(duration[i] > 0);
		long timestamp = previousTimestamp[i];
		System.err.printf("timestamp:%d - %d\n", i, timestamp);
		previousTimestamp[i] += duration[i];
		return timestamp;
	}

	private static void packetizeNalUnit(byte[] nalu1,
			ElementaryStreamPacketizer esPacketizer,
			MpegTsPacketizer tsPacketizer,
			ElementaryStreamDescription esDescription, int i) throws IOException {
		assert (nalu1 != null);
		// System.err.println(nalu1);
		esPacketizer
				.add(nalu1,
						new ElementaryStreamPacketMetadata(
								(byte) (BASE_ID_FOR_VIDEO_STREAMS + (esDescription
										.getPacketID() == PACKET_IDENTIFIERS[0] ? 0
										: 1)), false, false, true,
										getNextTimestamp(inputTs[i], i), null));
		tsPacketizer.add(esPacketizer.getNextPacket(),
				new MpegTsPacketMetadata(false, esDescription.getPacketID()));
	}
}
