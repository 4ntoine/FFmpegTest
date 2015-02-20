package com.splinex.tsmuxlib;

public interface Packetizer<T extends AbstractPacketMetadata> {

	void add(byte[] payload, T metadata);

	byte[] getNextPacket();

}