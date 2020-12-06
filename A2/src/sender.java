import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.File;
import java.net.*;


public class sender {

    static final int maxDataLength = 500;
    static final int SeqNumModulo = 32;
    static final int window_size = 10;
    static final int timer = 100;

    static String host_address = null;
    static int emulator_port = 0;
    static int rec_port = 0;
    static String filename = null;


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

        try{
            byte[] rec_data = new byte[512]; // since 500 for data, 4 for seq, 4 for type, 4 for data_length
            DatagramPacket r_packet = new DatagramPacket(rec_data, rec_data.length);

            //sender start to receive data and start the timer
            rec_socket.setSoTimeout(timer);
            rec_socket.receive(r_packet);
            packet  rec_packet = packet.parseUDPdata(rec_data);
            return rec_packet;

        } catch (SocketTimeoutException e ) //Time out!
        {
            throw  e;
        }
    }



    public static void main(String[] args) throws Exception {



        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            host_address = args[0];
            emulator_port = Integer.parseInt(args[1]); //sender sends packets to u-emulator
            rec_port = Integer.parseInt(args[2]); // used by sender to receive the Data from emulator
            filename = args[3];
        }

        //create socket
        DatagramSocket send_socket = new DatagramSocket(emulator_port);  //sender sends packets to u-emulator
        DatagramSocket rec_socket = new DatagramSocket(rec_port); // used by sender to receive the Data from emulator

        //create our output and log file
        PrintWriter seq_log = new PrintWriter("seqnum.log");
        PrintWriter ack_log = new PrintWriter("ack.log");



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

        //divide data into packets
        int data_length = str_content.length();
        int count = 0;
        int num_packet = (int) Math.ceil((double) data_length / maxDataLength);
        packet packets[] = new packet[num_packet];

        for (int i = 0; i < num_packet; i++) {
            int length = maxDataLength;
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

        int send_base = 0;
        int next_seq_num = 0;

        while(true) {

            //All packets are sent successfully, then we send the EOT packet
            if(send_base == num_packet) {
                packet eot_packet = packet.createEOT(num_packet);
                send_packet(send_socket,eot_packet,host_address,emulator_port);
                seq_log.println(eot_packet.getSeqNum()); //write to seqnum.log

                // wait for the EOT packet back
                while (true) {
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

            //There exits some packets that need to sent
            else{
                //send but not yet ack
                for(;next_seq_num  < (send_base + window_size) && (next_seq_num  < num_packet); next_seq_num++) {
                    int seq_modulo= next_seq_num%32;
                    send_packet(send_socket,packets[next_seq_num],host_address,emulator_port);
                    seq_log.println(seq_modulo); //seqnum.log
                }

                try {
                    //receive packet and record the ack num
                    packet rec_packet = rec_packet(rec_socket);
                    int type = rec_packet.getType();
                    int seq_num =rec_packet.getSeqNum();
                    ack_log.println(seq_num);   //ack.log

                    if(type == 2){
                        send_socket.close();
                        rec_socket.close();
                        seq_log.close();
                        ack_log.close();
                        System.exit(0);

                    }
                    int base_modulo = send_base % SeqNumModulo;
                    int num_ack= seq_num - base_modulo;

                    if (seq_num == base_modulo) { //receiver accepts correctly, then we shift the base
                        send_base++;
                    } else if ( (num_ack < window_size) && seq_num > base_modulo   ) {//last cumulatively ACKed byte
                        send_base = send_base + num_ack+ 1;
                    }
                } catch (SocketTimeoutException e) {
                    //Time out! We gonna resend the file [send_base] -> [next_seq_num-1]
                    for (int i = send_base; i < next_seq_num ; i++) {
                        int seq_modulo= i%32;
                        //resend the packet and record the seq num
                        send_packet(send_socket,packets[i],host_address,emulator_port);
                        seq_log.println(seq_modulo);
                    }
                }
            }
        }



    }

}
