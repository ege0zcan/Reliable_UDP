//import sun.security.mscapi.KeyStore;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sender {
    // Constants
    private static final int SEGMENT_SIZE = 1024; // 1KB packets
    private static final int HEADER_SIZE = 2; // 2 Byte headers
    private static final int ACK_SIZE = 2; // 2 Byte headers

    private static final int SENT = 0;
    private static final int ACKED = 1;
    private static final int USABLE = 2;

    // Variables
    private static String file_path;
    private static int receiver_port;
    private static int window_size_N;
    private static int retransmission_timeout;
    private static String ip;
    private static File image;
    private static DatagramSocket clientSocket;
    private static InetAddress ip_address;

    public static void main(String args[]) {
        // Get  command line arguments
        file_path = args[0];
        receiver_port = Integer.parseInt(args[1]);
        window_size_N = Integer.parseInt(args[2]);
        retransmission_timeout = Integer.parseInt(args[3]); //given in milliseconds
        ip = "127.0.0.1"; //localhost
        // Create file object for image
        image = new File(file_path);
        // Arraylist that will store the packets
        List<byte[]> segments;

        try{
            // Split image into packets
            segments = splitFile(image);

            // Set variables
            startConnection();

            // Send packets
            sendPackets(segments);

            stopConnection();
        }catch (IOException e) {
            e.printStackTrace();
        }

//        // Send packets
//        for (byte[] segment : segments)
//        {
//            MyThread test = new MyThread(segment,retransmission_timeout,clientSocket,ip_address,receiver_port,sequence_no);
//            //sendSegment(segment);
//            //start timer
//            //checkResponse();
//            sequence_no++;
//        }
    }

    public static void sendPackets(List<byte[]> segments) throws IOException {
        int send_base = 1; // initial send_base
//        int send_end = window_size_N; // initial endpoint for send window
        int next_seq = send_base; // sequence number of next packet to send

        int no_of_packets = segments.size();
        int no_of_received_ack = 0;

        MyThread[] threads = new MyThread[window_size_N];
        int[] ack_buffer = new int[window_size_N];
        for(int i = 0; i <window_size_N ; i++)
        {
            ack_buffer[i] = USABLE;
        }


        while (no_of_received_ack < no_of_packets)
        {
            // Send packets from send_window
            while( next_seq >= send_base && next_seq < send_base + window_size_N && next_seq <= no_of_packets)
            {
                int thread_index = (next_seq-1) % window_size_N;

                if(ack_buffer[thread_index] == USABLE)
                {
                    byte[] segment = segments.get(next_seq-1);
                    threads[thread_index] = new MyThread(segment,retransmission_timeout,clientSocket,ip_address,receiver_port,next_seq);
                    System.out.println("Sending packet no: " + next_seq + " Thread_Index "  + thread_index );
                    ack_buffer[thread_index] = SENT;
                    next_seq++;
                }
            }

            // Get ACK
            int ack_sequence_no = checkResponse();

            // If ACK'ed packet in window
            if(ack_sequence_no >= send_base && ack_sequence_no < send_base + window_size_N )
            {
                int thread_index = (ack_sequence_no-1) % window_size_N;
                MyThread acked_thread = threads[thread_index];

                // If ACK'ed thread alive
                if(acked_thread.isAlive())
                {
                    acked_thread.interrupt();
                    no_of_received_ack++;
                    ack_buffer[thread_index] = ACKED;

                    // If ACK received for first  packet in window, increase send base
                    if(ack_sequence_no == send_base)
                    {
                        ack_buffer[(send_base-1)%window_size_N] = USABLE;
                        send_base++;

                        // Check buffer for other ACK'ed packets
                        while(ack_buffer[(send_base-1)%window_size_N] == ACKED)
                        {
                            ack_buffer[(send_base-1)%window_size_N] = USABLE;
                            send_base++;
                        }

                    }
                }
            }
        }
        System.out.println("NO OF ACK: "+ no_of_received_ack);
        System.out.println("SEND_BASE" + send_base);
        System.out.println("NEXT SEQ: "+ next_seq);
    }

    public static List<byte[]> splitFile(File f) throws IOException {
        int sequence_no = 1;
        byte[] file_content = Files.readAllBytes(f.toPath());
        int file_size = file_content.length;
        int segment_count = (int)(Math.ceil((double)file_size / (double)(SEGMENT_SIZE-HEADER_SIZE)));

        List<byte[]> segments = new ArrayList<byte[]>();

        int start_index = 0;
        int end_index = SEGMENT_SIZE - HEADER_SIZE;

        while (sequence_no < segment_count)
        {
            byte[] segment = new byte[SEGMENT_SIZE];

            // 2 byte header with sequence number, in big endian format
            ByteBuffer b = ByteBuffer.allocate(4);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putInt(sequence_no);
            byte[] big_endian = b.array();
            segment[0] = big_endian[2];
            segment[1] = big_endian[3];

            byte[] slice = Arrays.copyOfRange(file_content,start_index,end_index);
            for(int i=0; i<SEGMENT_SIZE-HEADER_SIZE;i++)
            {
                segment[i+HEADER_SIZE] = slice[i];
            }

            segments.add(segment);
            sequence_no++;

            start_index = end_index ;
            end_index = start_index + SEGMENT_SIZE-HEADER_SIZE;
        }

        //handle the last sequence
        end_index = file_size;
        byte[] segment = new byte[end_index-start_index+HEADER_SIZE];

        // 2 byte header with sequence number, in big endian format
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(sequence_no);
        byte[] big_endian = b.array();
        segment[0] = big_endian[2];
        segment[1] = big_endian[3];

        byte[] slice = Arrays.copyOfRange(file_content,start_index,end_index);
        for(int i=0; i<slice.length; i++)
        {
            segment[i+HEADER_SIZE] = slice[i];
        }
        segments.add(segment);
        return segments;
    }

    public static void startConnection() throws IOException {
        clientSocket =  new DatagramSocket();
        ip_address = InetAddress.getByName(ip);
    }

    public static void sendSegment(byte[] segment) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(segment, segment.length, ip_address, receiver_port);
        clientSocket.send(sendPacket);
    }

    public static int checkResponse() throws IOException {
        byte[] receiveData = new byte[ACK_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        byte[] data = receivePacket.getData();
        byte[] big_endian = new byte[4];
        big_endian[0] = 0;
        big_endian[1] = 0;
        big_endian[2] = data[0];
        big_endian[3] = data[1];
        int ack_seq_no = java.nio.ByteBuffer.wrap(big_endian).getInt();
        System.out.println("ACK GELDI: "+ ack_seq_no);
        return ack_seq_no;
    }


    public static  void stopConnection() throws IOException {
        byte[] head = {0,0}; // double zero is sent to announce end of transmission
        DatagramPacket sendPacket = new DatagramPacket(head, 2, ip_address, receiver_port);
        clientSocket.send(sendPacket);
        clientSocket.close();
    }
}