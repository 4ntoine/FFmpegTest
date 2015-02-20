package com.splinex.tsmuxlib;


public class ElementaryStreamPacketMetadata extends AbstractPacketMetadata {
	
	byte streamID;
	boolean isHighPriority;
	boolean isCopyrighted;
	boolean isOriginal;
	Long presentationTimestamp;
	Long decodingTimestamp;
	
	public ElementaryStreamPacketMetadata(byte streamID,
			boolean isHighPriority, boolean isCopyrighted, boolean isOriginal,
			Long presentationTimestamp, Long decodingTimestamp) {
		this.streamID = streamID;
		this.isHighPriority = isHighPriority;
		this.isCopyrighted = isCopyrighted;
		this.isOriginal = isOriginal;
		this.presentationTimestamp = presentationTimestamp;
		this.decodingTimestamp = decodingTimestamp;
		
	}

}
