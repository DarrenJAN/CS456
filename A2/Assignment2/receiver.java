import java.io.PrintWriter;
import java.net.*;



public class receiver {
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

        byte[] rec_data = new byte[512]; // since 500 for data, 4 for seq, 4 for type, 4 for data_length
        DatagramPacket r_packet = new DatagramPacket(rec_data, rec_data.length);
        //start to receive data
        rec_socket.receive(r_packet);
        packet rec_packet = packet.parseUDPdata(rec_data);


        return rec_packet;
    }

    public static void main(String[] args) throws Exception {


        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            host_address = args[0];
            emulator_port = Integer.parseInt(args[1]);
            rec_port = Integer.parseInt(args[2]);
            filename = args[3];
        }

        //create the received and sending socket
        DatagramSocket rec_socket = new DatagramSocket(rec_port);
        DatagramSocket send_socket = new DatagramSocket(emulator_port);

        //create the output file and arrival log
        PrintWriter outputfile= new PrintWriter(filename);
        PrintWriter arrival_log = new PrintWriter("arrival.log");

        //Start to receive from sender
        int expect_seq= 0;
        int num_ack = 0;

        while(true) {
            //Get the packet from emulator
            packet recPacket =  rec_packet(rec_socket);
            int seq_num =recPacket.getSeqNum();
            int type = recPacket.getType();
            byte[] data = recPacket.getData();
            arrival_log.println(seq_num);

            //if it it not the packet that we expect
            //then we discard the packet and resent an ACK packet for the most
            //recently received in-order packet;
            if(expect_seq != seq_num) {
                if (num_ack == 0) {
                    //if the first packet got lost, we delay
                    Thread.sleep(timer);
                } else {
                    int last_seq = 0;
                    if (expect_seq == 0) {
                        last_seq = SeqNumModulo - 1;
                    } else {
                        last_seq = (expect_seq % SeqNumModulo) - 1;
                    }
                    packet last_packet = packet.createACK(last_seq);
                    send_packet(send_socket, last_packet, host_address, emulator_port);
                }
            } else {
                //if it is the packet that we expect

                //if it is the Data packet
                if(type == 1) {
                    //create ACK packet
                    packet ackPacket = packet.createACK(seq_num);
                    send_packet(send_socket,ackPacket,host_address,emulator_port);
                    expect_seq = (expect_seq+1)% SeqNumModulo;

                    num_ack  ++;
                    String write = new  String(data);
                    outputfile.print(write);

                }//if it is the eof packet
                else if(type ==2 ) {
                    //create EOT packet
                    packet eof  = packet.createEOT(seq_num);
                    send_packet(send_socket,eof,host_address,emulator_port);

                    //close all connection and log file
                    arrival_log.close();
                    outputfile.close();
                    send_socket.close();
                    rec_socket.close();
                    System.exit(0);
                }
            }
        }

    }

}

