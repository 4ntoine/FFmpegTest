package com.splinex.streaming;

import com.splinex.streaming.settings.Settings;
import com.splinex.tsmuxlib.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TsStreamer extends AbstractStreamer {

    private static final short[] PACKET_IDENTIFIERS = {0x22, 0x23};
    private static final byte BASE_ID_FOR_VIDEO_STREAMS = (byte) 0xe0;
    public static final int MAX_QUEUE_SIZE_PER_STREAM = 150;
    public static final long CHUNK_SIZE = 3L * 1024 * 1024 * 1024;
    private int maxQueueSize;

    private byte[] HEADER;
    private byte[][] PPS = null;
    LinkedBlockingQueue<byte[]> packets;

    MpegTsMultiplexer multiplexer;
    private final int port;
    private int streamCount;
    private boolean closed = false;
    private ServerSocket serverSocket;
    private ArrayList<TsClient> clients = new ArrayList<TsClient>();

    ElementaryStreamPacketizer[] esPacketizer = new ElementaryStreamPacketizer[]{new ElementaryStreamPacketizer(), new ElementaryStreamPacketizer()};
    ElementaryStreamDescription[] esDescription = new ElementaryStreamDescription[]{
            new ElementaryStreamDescription(PACKET_IDENTIFIERS[0]),
            new ElementaryStreamDescription(PACKET_IDENTIFIERS[1])};
    MpegTsPacketizer tsPacketizer = new MpegTsPacketizer();

    private final String filename;
    private final Runnable writerLoop = new Runnable() {
        @Override
        public void run() {
            OutputStream os = null;
            int size = 0;
            long time = System.currentTimeMillis();
            long t2 = time;
            try {
                os = new BufferedOutputStream(new ChunkedFileOutputStream(filename, CHUNK_SIZE), 512 * 1024);
                while (!closed) {
                    byte[] data;
                    while ((data = packets.poll(100, TimeUnit.MILLISECONDS)) != null) {
                        size += data.length;
                        os.write(data);
                        long now = System.currentTimeMillis();
                        if (now - t2 > 1000) {
                            Log.d(String.format("PSize=%d Speed=%.2fMB/s", packets.size(), size / 1048.576 / (now - time)));
                            //size = 0;
                            t2 = now;

                        }
                    }
                }
            } catch (Exception e) {
                Log.e("Exception: ", e);
            }
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e("IOException while closing TS file");
                }
        }
    };

    private final Runnable serverLoop = new Runnable() {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (!closed) {
                    Socket client = serverSocket.accept();
                    if (true /*Settings.getInstance().isRecordEnabled()*/ ) {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                    } else {
                        TsClient tsClient = new TsClient(client, new Packet(HEADER, 0), 20 * streamCount);
                        for (int i = 0; i < streamCount; ++i)
                            if (PPS[i] != null)
                                tsClient.put(new Packet(PPS[i], 0));
                        clients.add(tsClient);
                    }
                }
            } catch (IOException e) {
                Log.e("IOException: ", e);
            }

        }
    };

    public TsStreamer(int port, int streamCount, String filename) {
        this.port = port;
        this.streamCount = streamCount;
        this.filename = filename;
        PPS = new byte[streamCount][];
        multiplexer = new MpegTsMultiplexer(Arrays.asList(esDescription).subList(0, streamCount));
        HEADER = new byte[2 * MpegTsPacketizer.PACKET_LENGTH];
        multiplexer.read(HEADER);
        if (filename == null)
            new Thread(serverLoop).start();
        else {
            maxQueueSize = MAX_QUEUE_SIZE_PER_STREAM * streamCount;
            packets = new LinkedBlockingQueue<byte[]>();
            new Thread(writerLoop).start();
        }
    }

    private void packetizeNalUnit(byte[] nalu1,
                                  ElementaryStreamPacketizer esPacketizer,
                                  MpegTsPacketizer tsPacketizer,
                                  ElementaryStreamDescription esDescription, long timestamp) {
        long presentationTimestamp = 9 * timestamp / 100;
//        Log.e("ts="+timestamp+" pts="+presentationTimestamp);

        esPacketizer
                .add(nalu1,
                        new ElementaryStreamPacketMetadata(
                                (byte) (BASE_ID_FOR_VIDEO_STREAMS + (esDescription
                                        .getPacketID() == PACKET_IDENTIFIERS[0] ? 0
                                        : 1)), false, false, true, presentationTimestamp, null));
        tsPacketizer.add(esPacketizer.getNextPacket(),
                new MpegTsPacketMetadata(false, esDescription.getPacketID()));

    }

    @Override
    public synchronized void put(Packet p, int idx) {
        packetizeNalUnit(p.data, esPacketizer[idx], tsPacketizer, esDescription[idx], p.timeStamp);
        byte[] tsPacket;
        int i = 0;
        while ((tsPacket = tsPacketizer.getNextPacket()) != null)
            i += multiplexer.add(tsPacket);
        byte[] buf = new byte[MpegTsPacketizer.PACKET_LENGTH * i];
        multiplexer.read(buf);
        if (p.isSPS_PPS())
            PPS[idx] = buf;
        send(buf, p.timeStamp);
    }

    private void send(byte[] data, int timeStamp) {
        if (packets != null) {
            try {
                if (packets.size() < maxQueueSize)
                    packets.put(data);
                else {
                    Log.e("Packet queue cleared!");
                    packets.clear();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        Packet p = new Packet(data, timeStamp);
        TsClient remove = null;
        for (TsClient client : clients) {
            boolean res = client.put(p);
            if (!res)
                remove = client;
        }
        if (remove != null) {
            remove.close();
            clients.remove(remove);
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("IOException while closing server socket");
            }
            for (TsClient client : clients)
                client.close();
            clients.clear();
        }
    }

}
