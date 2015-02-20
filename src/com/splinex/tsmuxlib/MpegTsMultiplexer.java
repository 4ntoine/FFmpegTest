package com.splinex.tsmuxlib;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.CRC32;

public class MpegTsMultiplexer extends java.io.InputStream {

	private ConcurrentLinkedQueue<byte[]> packets;
	private int currentReadOffset;
	private Object readLock, writeLock;
	private CRC32 crc32;

	public static final short PROGRAM_MAP_PACKET_ID = 0x1000;
	public static final short PROGRAM_CLOCK_REFERENCE_PACKET_ID = 34;
	public static final short PROGRAM_NUMBER = 1;

	private List<ElementaryStreamDescription> descriptions;

	public MpegTsMultiplexer(List<ElementaryStreamDescription> descriptions) {
		this.readLock = new Object();
		this.writeLock = new Object();
		this.crc32 = new CRC32();
		this.packets = new ConcurrentLinkedQueue<byte[]>();
		this.addProgramAssociationTable();
		this.descriptions = descriptions;
		this.addProgramMapTable(descriptions);
	}

	private void waitForPackets() {
		while (packets.isEmpty())
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	private void advance() {
		packets.poll();
		currentReadOffset = 0;
	}

	@Override
	public int read() {
		synchronized (readLock) {
			waitForPackets();
			int result = packets.peek()[currentReadOffset++] & 0xff;
			if (currentReadOffset == MpegTsPacketizer.PACKET_LENGTH)
				advance();
			return result;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) {
		synchronized (readLock) {
			waitForPackets();
			int r;
			for (r = 0; r < len;) {
				if (packets.isEmpty())
					return r;
				int availableLength = Math.min(len - r,
						MpegTsPacketizer.PACKET_LENGTH - currentReadOffset);
				System.arraycopy(packets.peek(), currentReadOffset, b, off,
						availableLength);
				r += availableLength;
				off += availableLength;
				if (r == len)
					currentReadOffset += availableLength;
				else
					advance();
			}
			return r;
		}
	}

	@Override
	public int read(byte[] buffer) {
		return read(buffer, 0, buffer.length);
	}

	private int packetCount = 0;
	private boolean isAdding;

	public int add(byte[] packet) {
		synchronized (writeLock) {
			assert(packet.length == MpegTsPacketizer.PACKET_LENGTH);
			packets.add(packet);
			if (!isAdding && ((++packetCount & 0x7f) == 0)) {
				isAdding = true;
				addProgramAssociationTable();
				addProgramMapTable(descriptions);
				isAdding = false;
				return 3;
			}
			return 1;
		}
	}

	private final byte reflect(byte i) {
		return (byte) (((i & 1) << 7) | ((i & 2) << 5) | ((i & 4) << 3)
				| ((i & 8) << 1) | ((i & 16) >>> 1) | ((i & 32) >>> 3)
				| ((i & 64) >>> 5) | ((i & 0x80) >>> 7));
	}

	final byte reflect2(final byte i) {
		BitSet reflected = new BitSet(8);
		for (int j = 0; j < 8; j++) {
			reflected.set(j, ((i >>> j) & 1) != 0);
		}
		byte result = 0;
		for (int j = 0; j < 8; j++) {
			result |= (reflected.get(j) ? 1 : 0) << (7 - j);
		}
		return result;
	}

	private final int reflect(final int i) {
		byte[] buf = new byte[4];
		buf[0] = reflect((byte) i);
		buf[1] = reflect((byte) ((i & 0xff00) >>> 8));
		buf[2] = reflect((byte) ((i & 0xff0000) >>> 16));
		buf[3] = reflect((byte) ((i & 0xff000000) >>> 24));
		return (buf[3] & 0xff) | ((buf[2] & 0xff) << 8)
				| ((buf[1] & 0xff) << 16) | ((buf[0] & 0xff) << 24);
	}

	private final byte[] reflect(byte[] buf, int offset, int len) {
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++) {
			result[i] = reflect(buf[offset + i]);
		}
		return result;
	}

	final String toBitString(final byte i) {
		StringBuilder s = new StringBuilder();
		for (int j = 7; j >= 0; j--) {
			s.append((i & (1 << j)) != 0 ? "1" : "0");
		}
		return s.toString();
	}

	private void setCRC32(byte[] buffer, int offset, int length) {
		crc32.reset();
		byte[] bits = reflect(buffer, offset, length);
		crc32.update(bits);
		int crc = reflect(~(int) crc32.getValue());
		int c = offset + length;
		// System.err.printf("crc = 0x%x\n", crc);
		buffer[c] = (byte) ((crc & 0xff000000) >>> 24);
		buffer[c + 1] = (byte) ((crc & 0xff0000) >>> 16);
		buffer[c + 2] = (byte) ((crc & 0xff00) >>> 8);
		buffer[c + 3] = (byte) crc;
		crc32.reset();
		bits = reflect(buffer, offset, length + 4);
		crc32.update(bits);
		// System.err.printf("Section CRC = 0x%x\n",
		//		reflect(~(int) crc32.getValue()));
		assert (reflect(~(int) crc32.getValue()) == 0);
	}

	void crc32SanityCheck() {
		byte[] buffer = { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39 };
		crc32.reset();
		byte[] bits = reflect(buffer, 0, buffer.length);
		crc32.update(bits);
		int crc = reflect(~(int) crc32.getValue());
		// System.err.printf("CRC of \"123456789\" is 0x%x\n", crc);
		assert (crc == 0x376e6e7);
	}

	private MpegTsPacketizer programAssociationTablePacketizer = new MpegTsPacketizer();

	public void addProgramAssociationTable() {
		// crc32SanityCheck();
		// for (int i = 0; i < 256; i++)
		//	assert (reflect((byte) i) == reflect2((byte) i));
		byte[] packet = new byte[MpegTsPacketizer.PACKET_LENGTH
				- MpegTsPacketizer.PACKET_HEADER_LENGTH];
		// packet[0] = (byte) 0; // pointer
		// packet[1] = (byte) 0; // table ID
		packet[2] = (byte) 0xB0;
		packet[3] = (byte) 13;
		// packet[4] = (byte) 0;
		packet[5] = (byte) 1; // ID of this transport stream
		packet[6] = (byte) 0xC1;
		// packet[7] = (byte) 0; // section number
		// packet[8] = (byte) 0; // the last section number
		packet[10] = (byte) PROGRAM_NUMBER;
		packet[11] = (byte) (0xE0 | (PROGRAM_MAP_PACKET_ID >>> 8));
		packet[12] = (byte) PROGRAM_MAP_PACKET_ID; // program map PID
		setCRC32(packet, 1, 12);
		Arrays.fill(packet, 17, MpegTsPacketizer.PACKET_LENGTH
				- MpegTsPacketizer.PACKET_HEADER_LENGTH, (byte) -1);
		programAssociationTablePacketizer.add(packet, new MpegTsPacketMetadata(
				false, (short) 0));
		byte[] tsPacket = programAssociationTablePacketizer.getNextPacket();
		assert (tsPacket != null);
		this.add(tsPacket);
	}

	private MpegTsPacketizer programMapTablePacketizer = new MpegTsPacketizer();

	public void addProgramMapTable(
			List<ElementaryStreamDescription> descriptions) {
		byte[] packet = new byte[MpegTsPacketizer.PACKET_LENGTH
				- MpegTsPacketizer.PACKET_HEADER_LENGTH];
		// packet[0] = 0; // pointer
		packet[1] = (byte) 2; // table ID
		packet[4] = (byte) (PROGRAM_NUMBER >>> 8);
		packet[5] = (byte) PROGRAM_NUMBER;
		packet[6] = (byte) 0xC1;
		// packet[7] = (byte) 0; // section number
		// packet[8] = (byte) 0; // the last section number
		packet[9] = (byte) (0xe0 | ((PROGRAM_CLOCK_REFERENCE_PACKET_ID & 0xffff) >> 8));
		packet[10] = (byte) PROGRAM_CLOCK_REFERENCE_PACKET_ID;
		packet[11] = (byte) 0xf0;
		packet[12] = (byte) 0; // no additional descriptors
		int k = 12;
		for (int i = 0; i < descriptions.size(); i++) {
			ElementaryStreamDescription description = descriptions.get(i);
			packet[++k] = (byte) description.streamType;
			packet[++k] = (byte) (0xe0 | (description.packetID >>> 8));
			packet[++k] = (byte) (description.packetID & 0xff);
			int descriptorsTotalSize = fillDescriptors(packet, k + 3,
					description);
			packet[++k] = (byte) (0xf0 | (descriptorsTotalSize >>> 8));
			packet[++k] = (byte) (descriptorsTotalSize & 0xff);
			k += descriptorsTotalSize;
		}
		assert (k < 1021);
		packet[2] = (byte) (0xB0 | (k + 1) >>> 8);
		packet[3] = (byte) (k + 1); // length of the table after this field
		setCRC32(packet, 1, k);
		Arrays.fill(packet, k + 5, MpegTsPacketizer.PACKET_LENGTH
				- MpegTsPacketizer.PACKET_HEADER_LENGTH, (byte) -1);
		programMapTablePacketizer.add(packet, new MpegTsPacketMetadata(false,
				PROGRAM_MAP_PACKET_ID));
		this.add(programMapTablePacketizer.getNextPacket());
	}

	private static int fillDescriptors(byte[] buffer, int offset,
			ElementaryStreamDescription description) {
		return 0; // TODO
	}

	@Override
	public void close() {
	}

}
