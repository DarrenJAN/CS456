import javax.swing.tree.ExpandVetoException;
import java.io.*;
import java.net.*;
import java.math.*;


public class Senderr {

    public static void send_packet(DatagramSocket send_socket, byte []content, String Address, int dec_port) throws Exception{
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

        //
        DatagramSocket send_socket = new DatagramSocket();
        DatagramSocket rec_socket = new DatagramSocket(Integer.parseInt(args[2]));
        host_address = args[0];
        emulator_port = Integer.parseInt(args[1]);

        int window_base = 0 ;
        int next_seq = 0;

        while(window_base != 0){
            while(next_seq < window_base+ 10 && next_seq < size){
                byte []  content = packets[next_seq].getUDPdata();
                send_packet(send_socket,content,host_address,emulator_port);
                seq_log.println(next_seq%32);
                next_seq++;
            }

            try{
                rec_socket.setSoTimeout(500);
                packet rec_pactet = rec_packet(rec_socket,ack_log);

                if (rec_pactet.getSeqNum() ==window_base %32){
                    window_base++;
                } else if(rec_pactet.getSeqNum() >window_base %32  &&  (rec_pactet.getSeqNum() -window_base %32)<10){
                    window_base += rec_pactet.getSeqNum() - window_base %32 +1;
                } else if(rec_pactet.getSeqNum() <10  && (rec_pactet.getSeqNum() +32 - window_base)<10) {
                    window_base += rec_pactet.getSeqNum() +32 - window_base % 32+1;
                }
            } catch (SocketTimeoutException e ){
                for (int i = window_base; i<next_seq; i++){
                    byte [] content=  packets[i].getUDPdata();
                    send_packet(send_socket,content,host_address,emulator_port);
                    seq_log.println(i%32);
                }
            }
        }

        packet eof = packet.createEOT(size%SeqNumModulo);
        byte [] content = eof.getUDPdata();
        send_packet(send_socket,content,host_address,emulator_port);
        seq_log.println(eof.getSeqNum());

        while(true){
            try{
                packet rec_packet = rec_packet(rec_socket, ack_log);
                if(rec_packet.getType() ==2) break;
            } catch(SocketTimeoutException e) {
                continue;
            }
        }

        rec_socket.close();
        send_socket.close();
        ack_log.close();
        seq_log.close();


    }
}
