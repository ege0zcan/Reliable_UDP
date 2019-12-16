import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MyThread implements Runnable {
    byte[] segment;
    int timeout;
    DatagramSocket clientSocket;
    InetAddress ip_address;
    int receiver_port;
    int sequence_no;

    Thread t;

    MyThread(byte[] segment, int timeout, DatagramSocket clientSocket, InetAddress ip_address, int receiver_port, int sequence_no) {
        this.segment = segment;
        this.timeout = timeout;
        this.clientSocket = clientSocket;
        this.ip_address = ip_address;
        this.receiver_port = receiver_port;
        this.sequence_no = sequence_no;
        t = new Thread(this);
        System.out.println("New thread: " + t);
        System.out.println("Alo" + sequence_no);
        t.start();
    }


    public void run() {
        try {
            while (true) {
                // Send packet
                //sendSegment(segment);

                // Wait for main thread notification or timeout
                Thread.sleep(timeout);
            }
        }

        // Stop if main thread interrupts this thread
        catch (InterruptedException e) {
            return;
        }
//        } catch (IOException e) {
//        e.printStackTrace();
//        }
//    }

//    public void sendSegment(byte[] segment) throws IOException {
//        DatagramPacket sendPacket = new DatagramPacket(segment, segment.length, ip_address, receiver_port);
//        clientSocket.send(sendPacket);
//    }
    }
}

class MultiThread {
    public static void main(String args[]) {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            System.out.println("Main thread Interrupted");
        }
        System.out.println("Main thread exiting.");
    }
}