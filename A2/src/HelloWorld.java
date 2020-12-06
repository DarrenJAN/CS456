/*
import java.io.*;
import java.net.*;


public class receiverjj {

    public static void send_packet(DatagramSocket send_socket, byte []content, String Address, int dec_port) throws Exception{
        InetAddress ip = InetAddress.getByName(Address);
        try{
            DatagramPacket send_packet  = new DatagramPacket(content, content.length,ip,dec_port);
            send_socket.send(send_packet);
        } catch(SocketException e){
            e.printStackTrace();
        }
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

        DatagramSocket rec_socket = new DatagramSocket(rec_port);
        DatagramSocket send_socket = new DatagramSocket(emulator_port);
        InetAddress Ip = InetAddress.getByName(host_address);
        PrintWriter datawrite = new PrintWriter(filename);
        PrintWriter arr_log = new PrintWriter("arrival_log");

        int nextpacet= 0;
        byte [] rec_data  = new byte[1024];

        DatagramPacket rec_packet = new DatagramPacket(rec_data,rec_data.length);

        while(true) {
            rec_socket.receive(rec_packet);
            packet recPacket = packet.parseUDPdata(rec_packet.getData());
            arr_log.println(recPacket.getSeqNum());
            if(nextpacet == recPacket.getSeqNum()) {
                if(recPacket.getType() ==2 ) {
                    packet eof  = packet.createEOT(recPacket.getSeqNum());
                    byte[] eof_data = eof.getUDPdata();
                    DatagramPacket send_packet = new DatagramPacket(eof_data,eof_data.length,Ip,emulator_port);
                    send_socket.send(send_packet);

                    arr_log.close();
                    datawrite.close();
                    send_socket.close();
                    rec_socket.close();
                    return;
                } else {
                    datawrite.println(new String(rec_packet.getData()));

                    //
                    packet ackPacket = packet.createACK(recPacket.getSeqNum());
                    byte [] ack_data = ackPacket.getUDPdata();
                    DatagramPacket send_packet = new DatagramPacket(ack_data,ack_data.length,Ip,emulator_port);
                    send_socket.send(send_packet);
                    nextpacet++;
                    nextpacet = nextpacet% SeqNumModulo;
                }
            }
            //
            else{
                packet last_packet = packet.createACK((nextpacet +31)%32);
                byte [] last_data = last_packet.getUDPdata();
                DatagramPacket send_packet = new DatagramPacket(last_data,last_data.length,Ip,emulator_port);
                send_socket.send(send_packet);
            }
        }

    }


}

//------------------------------------------

import javax.swing.tree.ExpandVetoException;
        import java.io.*;
        import java.net.*;
        import java.math.*;

public class senderddd {


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

    static packet rec_packet(DatagramSocket rec_socket, PrintWriter Ack) throws Exception {

        byte[] rec_data = new byte[512]; // since 500 for data, 4 for seq, 4 for type, 4 for data_length
        DatagramPacket r_packet = new DatagramPacket(rec_data, rec_data.length);

        //start to receive data
        rec_socket.receive(r_packet);
        packet rec_packet = packet.parseUDPdata(rec_data);

        //write into Ack.log
        Ack.write(rec_packet.getSeqNum());

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

        //our output
        PrintWriter seq_log = new PrintWriter("seqnum.log");
        PrintWriter ack_log = new PrintWriter("ack.log");


        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            host_address = args[0];
            emulator_port = Integer.parseInt(args[1]);
            rec_port = Integer.parseInt(args[2]);
            filename = args[3];
        }


        //read from file
        File file = new File(args[3]);
        byte sentence[] = new byte[(int)file.length()];
        FileInputStream fileinputstream = new FileInputStream(file);
        fileinputstream.read(sentence);

        //preapare packets
        int size = sentence.length/500;
        int transffered = 0;
        if (sentence.length % 500 != 0) size += 1;
        packet packets[] = new packet[size];
        for (int i = 0; i < size; i++) {
            byte cp[] = new byte[Math.min(500, sentence.length - transffered)];
            System.arraycopy(sentence, transffered, cp, 0, Math.min(500, sentence.length - transffered));
            packets[i] = packet.createPacket(i % 32, new String(cp));
            transffered += 500;
        }

        System.out.println("hello you are here");
        System.out.println(size);
        //create socket
        DatagramSocket send_socket = new DatagramSocket(emulator_port);  //rec sends to u-emulator
        DatagramSocket rec_socket = new DatagramSocket(rec_port); // used to receive the Data
        InetAddress ip = InetAddress.getByName(host_address);

        //
        //---------------------Now Start to send packet-----------------
        //
        int windowBase = 0;
        int nextPacket = 0;
        int timeOut = 500;
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        //repeat sending from windowBase to windowBase + 10
        while (windowBase != size) {
            while (nextPacket < windowBase + 10 && nextPacket < size) {
                byte[] sendData = packets[nextPacket].getUDPdata();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        IPAddress, Integer.parseInt(args[1]));
                send_socket.send(sendPacket);
                seq_log.println(nextPacket % 32);
                nextPacket++;
            }
            try { // could time out
                rec_socket.setSoTimeout(timeOut);
                byte[] ack = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(ack, ack.length);
                rec_socket.receive(recvPacket);
                packet pk = packet.parseUDPdata(recvPacket.getData());
                ack_log.println(pk.getSeqNum());
                if (pk.getSeqNum() == windowBase % 32) {
                    windowBase++;
                } else if (pk.getSeqNum() > windowBase % 32 &&
                        (pk.getSeqNum() - windowBase % 32) < 10) {
                    windowBase += pk.getSeqNum() - windowBase % 32 + 1;
                } else if (pk.getSeqNum() < 10 &&
                        (pk.getSeqNum() + 32 - windowBase % 32) < 10) {
                    windowBase += pk.getSeqNum() + 32 - windowBase % 32 + 1;
                }
            } catch (SocketTimeoutException e) { // lost all pac
                for (int i = windowBase; i < nextPacket; i++) {
                    byte[] sendData = packets[i].getUDPdata();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            IPAddress, Integer.parseInt(args[1]));
                    send_socket.send(sendPacket);
                    seq_log.println(i % 32);
                }
            }
        }


        //------------ALL packets are sent successfully ------------
        //            Now send the EOF packet
        System.out.println("Hello you are here  asshole");
        //send eot
        packet eot = packet.createEOT(size);
        byte[] eotData = eot.getUDPdata();
        DatagramPacket eotPacket = new DatagramPacket(eotData, eotData.length,
                IPAddress, Integer.parseInt(args[1]));
        send_socket.send(eotPacket);
        seq_log.println(eot.getSeqNum());
        while (true) { //can not exit before EOT from receiver
            try {
                byte[] recv = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(recv, recv.length);
                rec_socket.receive(recvPacket);
                packet pk = packet.parseUDPdata(recvPacket.getData());
                if (pk.getType() == 2) break;
            } catch (SocketTimeoutException e) {
                continue;
            }
        }

        //closing
        send_socket.close();
        rec_socket.close();
        seq_log.close();
        ack_log.close();
    }

}

 */
