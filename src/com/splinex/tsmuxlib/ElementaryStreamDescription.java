package com.splinex.tsmuxlib;

public class ElementaryStreamDescription {
	public static final byte H264 = 27;
	byte streamType;
	short packetID;
	
	public ElementaryStreamDescription(short packetID) {
		this(packetID, ElementaryStreamDescription.H264);
	}
	
	public ElementaryStreamDescription(short packetID, byte streamType) {
		this.packetID = packetID;
		this.streamType = streamType;
	}
	
	public short getPacketID() {
		return packetID;
	}
}
