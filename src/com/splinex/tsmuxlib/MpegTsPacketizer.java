package com.splinex.tsmuxlib;

import java.util.Arrays;
import java.util.LinkedList;

public class MpegTsPacketizer implements Packetizer<MpegTsPacketMetadata> {

    public static final int PACKET_LENGTH = 188;
    public static final int PACKET_HEADER_LENGTH = 4;
    public static final byte SYNC_BYTE = 71;
    public static final long MPEG_TS_FREQUENCY = 27000000;
    public static final int TIMESTAMP_FREQUENCY = 90000;

    private LinkedList<byte[]> packets = new LinkedList<byte[]>();
    private int[] continuityCounters = new int[8192];

    private short pcrPID;
    private int frequency;
    private int minimalFrameDuration;

    public MpegTsPacketizer() {
        this.pcrPID = 0x1fff; // no PCRs
    }

    public MpegTsPacketizer(short pcrPID, int frequency,
                            int minimalFrameDuration) {
        this.pcrPID = pcrPID;
        this.frequency = frequency;
        this.minimalFrameDuration = minimalFrameDuration;
    }

    public void add(byte[] payload, MpegTsPacketMetadata metadata) {
        int insertionPoint = PACKET_HEADER_LENGTH;
        byte[] pcrBuf = null;
        if (pcrPID == metadata.PID && payload[8] != 0) // has PTS
            pcrBuf = payloadTimestampToPcr(payload, 9);
        int seqNumber = 0;
        long bitCount = 0;
        for (int pos = 0; pos < payload.length; pos += PACKET_LENGTH
                - insertionPoint) {
            byte[] packet = new byte[PACKET_LENGTH];
            packet[0] = SYNC_BYTE;
            packet[1] = (byte) ((pos == 0 ? 64 : 0)
                    | (metadata.isHighPriority ? 32 : 0) | ((metadata.PID >> 8) & 0x1f));
            packet[2] = (byte) metadata.PID;
            int adaptationFieldCtrl = 16;
            insertionPoint = PACKET_HEADER_LENGTH;
            if (pcrBuf != null && seqNumber++ % frequency == 0) {
                adaptationFieldCtrl = 48;
                packet[4] = (byte) 7; // 1 flag byte and 6 PCR bytes
                packet[5] = (byte) 16; // PCR
                final int bitOffset = (6 + 6) * 8;
                adjustPcr(
                        pcrBuf,
                        (int) (((bitCount + bitOffset) * MPEG_TS_FREQUENCY) / (payload.length
                                * 8L
                                * TIMESTAMP_FREQUENCY
                                / minimalFrameDuration * 2)));
                bitCount = -bitOffset;
                System.arraycopy(pcrBuf, 0, packet, 6, 6);
                insertionPoint = 5 + (packet[4] & 0xff);
            }
            if (payload.length - pos < PACKET_LENGTH - insertionPoint) {
                adaptationFieldCtrl = 48;
                if (insertionPoint == PACKET_HEADER_LENGTH)
                    insertionPoint += 2; // length and flag
                int stuffingLength = (PACKET_LENGTH - insertionPoint - (payload.length - pos));
                if (stuffingLength < 0 && packet[4] == 0) {
                    --insertionPoint;
                    ++stuffingLength;
                }
                assert (stuffingLength >= 0);
                packet[4] = (byte) ((packet[4] & 0xff) + stuffingLength + (packet[4] == 0
                        && insertionPoint == PACKET_HEADER_LENGTH + 2 ? 1 : 0));
                insertionPoint = 5 + (packet[4] & 0xff);
                if (Utils.DEBUG) {
                    System.err.println("packet[4] == " + (0xff & packet[4]));
                    System.err.println("insertionPoint == " + insertionPoint);
                    System.err.println("stuffingLength == " + stuffingLength);
                    System.err.println("payload.length - pos == "
                            + (payload.length - pos));
                }
                assert (PACKET_LENGTH - insertionPoint == payload.length - pos);
                Arrays.fill(packet, insertionPoint - stuffingLength,
                        insertionPoint, (byte) -1);
                assert (packet[4] == (byte) 0 || insertionPoint
                        - stuffingLength > 5);
            }
            packet[3] = (byte) (adaptationFieldCtrl | (continuityCounters[metadata.PID]++ & 0xf));
            System.arraycopy(payload, pos, packet, insertionPoint,
                    PACKET_LENGTH - insertionPoint);
            packets.add(packet);
            bitCount += (PACKET_LENGTH << 3);
        }
    }

    private static byte[] payloadTimestampToPcr(byte[] payload, int offset) {
        if (Utils.DEBUG)
            System.err.printf("PTS 0x%02x|%02x|%02x|%02x|%02x\n", payload[offset],
                    payload[offset + 1], payload[offset + 2], payload[offset + 3],
                    payload[offset + 4]);
        byte[] pcr = new byte[6];
        // 32..25
        pcr[0] = (byte) (((payload[offset] & 0xe) << 4) | ((payload[offset + 1] & 0xf8) >>> 3));
        // 24..17
        pcr[1] = (byte) (((payload[offset + 1] & 7) << 5) | ((payload[offset + 2] & 0xff) >>> 3));
        // 16..9
        pcr[2] = (byte) (((payload[offset + 2] & 6) << 5) | ((payload[offset + 3] & 0xff) >>> 2));
        // 8..1
        pcr[3] = (byte) (((payload[offset + 3] & 3) << 6) | ((payload[offset + 4] & 0xff) >>> 2));
        // 0
        pcr[4] = (byte) (((payload[offset + 4] & 2) << 6) | 0x7e);
        if (Utils.DEBUG)
            System.err.printf("PCR 0x%02x|%02x|%02x|%02x|%02x\n", pcr[0], pcr[1],
                    pcr[2], pcr[3], pcr[4]);
        return pcr;
    }

    private void adjustPcr(byte[] pcr, int ticks) {
        assert (ticks >= 0);
        long pcrValueHigh = (Utils.uint32ToLong(pcr, 0) << 1)
                + ((pcr[4] & 0x80) >> 7);
        int pcrValueLow = pcr[5] & 0xff;
        assert (pcrValueLow >= 0);
        long ticksHigh = ticks / 300;
        long ticksLow = ticks - 300 * ticksHigh;
        pcrValueHigh += ticksHigh;
        pcrValueLow += ticksLow;
        long pcrValue = (pcrValueHigh << 9) | pcrValueLow;
        Utils.longToPcr(pcrValue, pcr, 0);
    }

    public byte[] getNextPacket() {
        if (packets.isEmpty())
            return null;
        return packets.remove();
    }

}
