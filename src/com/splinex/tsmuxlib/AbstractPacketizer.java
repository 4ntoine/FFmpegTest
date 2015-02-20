package com.splinex.tsmuxlib;

import java.util.LinkedList;

abstract class AbstractPacketizer<T extends AbstractPacketMetadata> implements Packetizer<T> {
	
	LinkedList<byte[]> packets = new LinkedList<byte[]>();
	
	abstract byte[] wrap(byte[] payload, T metadata);
	
	public void add(byte[] payload, T metadata) {
		byte[] packet = wrap(payload, metadata);
		assert(packet != null);
		packets.add(packet);
	}
	
	public byte[] getNextPacket() {
		return packets.remove();
	}

}
