import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sender {
    // Constant
    private static final int SEGMENT_SIZE = 1024; // 1KB packets
    private static final int HEADER_SIZE = 2; // 2 Byte headers
    private static final int ACK_SIZE = 2; // 2 Byte headers

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


        int sequence_no = 1;
        try{
            // Split image into packets
            List<byte[]> segments = splitFile(image);
            // Set variables
            startConnection();
            // Send packets
            for (byte[] segment : segments)
            {
                MyThread test = new MyThread(segment,retransmission_timeout,clientSocket,ip_address,receiver_port,sequence_no);
                //sendSegment(segment);
                //start timer
                //checkResponse();
                sequence_no++;
            }
            stopConnection();
        }catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void checkResponse() throws IOException {
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
        System.out.println(ack_seq_no);
    }


    public static  void stopConnection() throws IOException {
        byte[] head = {0,0}; // double zero is sent to announce end of transmission
        DatagramPacket sendPacket = new DatagramPacket(head, 2, ip_address, receiver_port);
        clientSocket.send(sendPacket);
        clientSocket.close();
    }
}