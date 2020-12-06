
/*import java.io.*;
import java.net.*;
import java.math.*;

public class sender {


    static final int time = 500;


    public static void send_packet(DatagramSocket send_socket, packet sendpacket, String Address, int dec_port) throws Exception{
        byte [] content = sendpacket.getUDPdata();
        InetAddress ip = InetAddress.getByName(Address);
        try{
            DatagramPacket send_packet  = new DatagramPacket(content, content.length,ip,dec_port);
            send_socket.send(send_packet);
        } catch(SocketException e){
            e.printStackTrace();
        }
    }

    public static packet rec_packet(DatagramSocket rec_socket) throws Exception {

        byte[] rec_data = new byte[1024]; // since 500 for data, 4 for seq, 4 for type, 4 for data_length
        DatagramPacket r_packet = new DatagramPacket(rec_data, rec_data.length);
        //start to receive data
        rec_socket.receive(r_packet);
        packet   rec_packet = packet.parseUDPdata(rec_data);

        return rec_packet;
    }



    public static void main(String[] args) throws Exception {
        //constant
        final int maxDataLength = 500;
        final int SeqNumModulo = 32;
        final int window_size = 10;

        String host_address = null;
        int emulator_port = 0;
        int rec_port = 0;
        String filename = null;


        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            host_address = args[0];
            emulator_port = Integer.parseInt(args[1]);
            rec_port = Integer.parseInt(args[2]);
            filename = args[3];
        }

        //our output
        PrintWriter seq_log = new PrintWriter("seqnum.log");
        PrintWriter ack_log = new PrintWriter("ack.log");

        //create socket
        DatagramSocket send_socket = new DatagramSocket(emulator_port);  //rec sends to u-emulator
        DatagramSocket rec_socket = new DatagramSocket(rec_port); // used to receive the Data


        //read the file
        File file = new File(filename);
        if(!file.exists()){
            throw new RuntimeException("File does not exist!");
        }

        FileInputStream fis = new FileInputStream(file);
        int content_length  = (int)file.length();
        byte content[] = new byte[content_length];
        fis.read(content);
        String str_content = new String(content,0,content_length);

        //divide data into packet
        int data_length = str_content.length();
        int count = 0;
        int num_packet = (int) Math.ceil((double) data_length / maxDataLength);
        packet packets[] = new packet[num_packet];

        for (int i = 0; i < num_packet; i++) {
            int length = 500;
            //last packet
            if (i == num_packet - 1) {
                length = data_length - count;
            }
            String data_str = str_content.substring(count, count + length);
            packets[i] = packet.createPacket(i % SeqNumModulo, data_str);
            count = count + length;
        }


        //
        //---------------------Now Start to send packet-----------------
        //
        int base = 0;
        int nextPacket = 0;
        int timeOut = 500;

        //repeat sending from windowBase to windowBase + 10
        while (base != num_packet) {

            while (nextPacket < base + window_size && nextPacket < num_packet) {
                send_packet(send_socket,packets[nextPacket],host_address,emulator_port);
                seq_log.println(nextPacket % SeqNumModulo);
                nextPacket++;
            }

            try {
                //start the timer
                rec_socket.setSoTimeout(timeOut);
                //receive packet and record the ack num
                packet rec_packet = rec_packet(rec_socket);
                ack_log.println(rec_packet.getSeqNum());

                int base_seq = base % SeqNumModulo;
                int num_ack= rec_packet.getSeqNum() - base_seq;

                if (rec_packet.getSeqNum() == base_seq) { //if receiver accepts correctly, then we shift the base
                    base++;
                    System.out.println("This is condition 1");

                    //last cumulatively ACKed byte
                } else if (rec_packet.getSeqNum() > base_seq && (num_ack) < window_size) {
                    System.out.println("This is condition 2");
                    base = base + num_ack+ 1;
                } else if (rec_packet.getSeqNum() < window_size &&
                        (SeqNumModulo + num_ack) < window_size) {
                    System.out.println("This is condition 3");
                    base = base+ SeqNumModulo +num_ack + 1;
                }
            } catch (SocketTimeoutException e) {
                // lost all pac
                System.out.println("This is time out");
                for (int i = base; i < nextPacket; i++) {
                    //resend the packet and record the seq num
                    send_packet(send_socket,packets[i],host_address,emulator_port);
                    seq_log.println(i % 32);
                }
            }
        }


        //------------ALL packets are sent successfully ------------
        //            Now send the EOT packet
        packet eot_packet = packet.createEOT(num_packet);
        send_packet(send_socket,eot_packet,host_address,emulator_port);
        seq_log.println(eot_packet.getSeqNum());
        while (true) { //can not exit before EOT from receiver
            try {
                packet rec_packet =rec_packet(rec_socket);
                ack_log.println(rec_packet.getSeqNum());
                if (rec_packet.getType() == 2) {
                    send_socket.close();
                    rec_socket.close();
                    seq_log.close();
                    ack_log.close();
                    System.exit(0);
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            }
        }


    }

}

////
  fk this
  else if (rec_packet.getSeqNum() < window_size &&  (SeqNumModulo + num_ack) < window_size) {
                        System.out.println("condition 3 ");
                      send_base = send_base+ SeqNumModulo +num_ack + 1;
                    }
 */

