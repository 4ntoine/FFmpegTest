package com.splinex.tsmuxlib;

public class MpegTsPacketMetadata extends AbstractPacketMetadata {
	boolean isHighPriority;
	short PID;
	boolean isPCR;
	long basePCR;
	int extensionPCR;
	
	public MpegTsPacketMetadata(boolean isHighPriority, short PID) {
		this.isHighPriority = isHighPriority;
		this.PID = PID;
		this.isPCR = false;
	}
	
	public MpegTsPacketMetadata(short PID, long basePCR, int extensionPCR) {
		this.isHighPriority = false;
		this.PID = PID;
		this.basePCR = basePCR;
		this.extensionPCR = extensionPCR;
	}
}
