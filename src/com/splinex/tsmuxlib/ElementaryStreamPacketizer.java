package com.splinex.tsmuxlib;

public class ElementaryStreamPacketizer extends
        AbstractPacketizer<ElementaryStreamPacketMetadata> {

    private static final byte MINIMAL_HEADER_LENGTH = 9;

    @Override
    byte[] wrap(byte[] payload, ElementaryStreamPacketMetadata metadata) {
        byte headerDataLength = 0;
        boolean hasDTS = metadata.decodingTimestamp != null;
        boolean hasPTS = metadata.presentationTimestamp != null;
        if (hasDTS && !hasPTS)
            throw new IllegalArgumentException();
        boolean hasBothTimestamps = hasDTS && hasPTS;
        if (hasBothTimestamps)
            headerDataLength = 10;
        else if (hasPTS)
            headerDataLength = 5;
        byte[] packet = new byte[MINIMAL_HEADER_LENGTH + headerDataLength
                + payload.length];
        if (Utils.DEBUG)
            System.err.println("data_index + pes_header_size should be "
                    + packet.length);
        packet[2] = 1; // start code prefix
        packet[3] = metadata.streamID;
        int sizeField = (packet.length > 0xffff) ? 0 : payload.length
                + headerDataLength + 3;
        packet[4] = (byte) (sizeField >> 8);
        packet[5] = (byte) sizeField;
        if (Utils.DEBUG)
            System.err.println("total_size is " + sizeField);
        packet[6] = (byte) (0x80 | (metadata.isHighPriority ? 8 : 0)
                | (metadata.isCopyrighted ? 2 : 0) | (metadata.isOriginal ? 1
                : 0));
        assert (packet[6] != 0);
        packet[7] = (byte) (((hasPTS ? 2 : 0) | (hasDTS ? 1 : 0)) << 6);
        packet[8] = headerDataLength;
        long pts = 0;
        if (hasPTS)
            pts = metadata.presentationTimestamp + 310 * 90;
        if (hasBothTimestamps) {
            packet[9] = (byte) (49 | (pts >> 29));
            packet[10] = (byte) (0xff & (pts >> 21));
            packet[11] = (byte) (1 | (((pts >> 15) & 0x7f) << 1));
            packet[12] = (byte) (0xff & (pts >> 7));
            packet[13] = (byte) (1 | ((pts & 0x7f) << 1));
            packet[14] = (byte) (17 | (metadata.decodingTimestamp >> 29));
            packet[15] = (byte) (0xff & (metadata.decodingTimestamp >> 21));
            packet[16] = (byte) (1 | (((metadata.decodingTimestamp >> 15) & 0x7f) << 1));
            packet[17] = (byte) (0xff & (metadata.decodingTimestamp >> 7));
            packet[18] = (byte) (1 | ((metadata.decodingTimestamp & 0x7f) << 1));
            System.arraycopy(payload, 0, packet, 19, payload.length);
        } else if (hasPTS) {
            if (Utils.DEBUG)
                System.err.println("pt = " + metadata.presentationTimestamp);
            packet[9] = (byte) (33 | (metadata.presentationTimestamp >> 29));
            packet[10] = (byte) (0xff & (metadata.presentationTimestamp >> 21));
            packet[11] = (byte) (1 | (((metadata.presentationTimestamp >> 15) & 0x7f) << 1));
            packet[12] = (byte) (0xff & (metadata.presentationTimestamp >> 7));
            packet[13] = (byte) (1 | ((metadata.presentationTimestamp & 0x7f) << 1));
            System.arraycopy(payload, 0, packet, 14, payload.length);
        } else {
            System.arraycopy(payload, 0, packet, 9, payload.length);
        }
        return packet;
    }
}
