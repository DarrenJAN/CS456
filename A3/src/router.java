import javax.xml.crypto.Data;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Vector;

public class router {
    public static final int NBR_ROUTER =5 ;
    public static final int LARGE_NUM = 65535;

    public static int router_id;
    public static String nse_host;
    public static int nse_port;
    public static int router_port;

    //
    public static  link_cost[] neighbour_link;
    public static  int nbr_link;
    public static Vector<pkt_LSPDU>  lspdus;
    public static int graph[][];
    public static PrintWriter router_log;
    public  static  DatagramSocket socket;




    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            router_id = Integer.parseInt(args[0]);
            nse_host = args[1];
            nse_port = Integer.parseInt(args[2]);
            router_port = Integer.parseInt(args[3]);

        }

        /*
        graph = new int[NBR_ROUTER][NBR_ROUTER];
        for(int i=0;i<NBR_ROUTER;i++){
            for(int j =0;j<NBR_ROUTER;j++){
                if(i == j){
                    graph[i][j] = 0;
                } else{
                    graph[i][j] = LARGE_NUM;
                }
            }
        }*/

        //create send  and receive socket
        DatagramSocket send_socket = new DatagramSocket();
        DatagramSocket rec_socket  = new DatagramSocket();
        socket = new DatagramSocket();


        //create the log file
        router_log = new PrintWriter(String.format("router%d.log", router_id));

        //1. send an Init packet to NSE
        send_init(send_socket);
        //2. receives a packet back from the nse
        recieveCircuitDB();

        //nbr_link = circuit_db.nbr_link;
        neighbour_link = new link_cost[nbr_link];
        //neighbour_link =  circuit_db.linkcost;
       // lspdus = new Vector<>();



        //3. send a Hello_pkt to its neighbour
        for(int i = 0; i< nbr_link;i++){
            int link_id = neighbour_link[i].link;
            int link_cost = neighbour_link[i].cost;
            pkt_LSPDU pkt_lspdu = new pkt_LSPDU(0,router_id,link_id,link_cost,0);
            lspdus.add(pkt_lspdu);
            send_hello(send_socket,router_id,link_id);
        }

        //4. receive hello from its neighbour
        int num_rec = 0;
        int MAX_rec = 25;
        while(num_rec < MAX_rec){
            byte [] rec_data = new byte[512];
            DatagramPacket r_packet = new DatagramPacket(rec_data,rec_data.length);
            rec_socket.receive(r_packet);
            int length = r_packet.getLength();
            //if we receive the hello pkt
            if(length <= 8 ){
                pkt_HELLO neighbour_pkt_hello = pkt_HELLO.parseUDPdata(rec_data);

                //send a set of LS_PDU packets to that neighbour
                int neighbour_id = neighbour_pkt_hello.router_id;
                for(int i = 0;i<nbr_link;i++){
                    //A router copying a received LS_PDU to its neighbour will both sender and via
                    pkt_LSPDU  pkt_lspdu = lspdus.get(i);
                    pkt_lspdu.set_sender(router_id);
                    pkt_lspdu.set_via(pkt_lspdu.link_id);
                    send_LSPDU(send_socket,pkt_lspdu);
                }

            //else if we receive the LSPDU pkt
            } else {
                pkt_LSPDU pkt_lspdu = pkt_LSPDU.parseUDPdata(rec_data);
                boolean unique  = false;

                for(int i =  0;i<lspdus.size();i++){
                    if(lspdus.get(i).router_id == pkt_lspdu.router_id &&lspdus.get(i).link_id == pkt_lspdu.link_id){
                        unique = true;
                    }
                }

                //if it is unique, we add information to the router's Link state Database
                if(unique){

                }

            }

        }

        ShortestPath t = new ShortestPath();
        t.dijkstra(graph,);



    }

    //send an INIT packet to NSE containing the router's id
    public static void send_init(DatagramSocket send_socket) throws Exception{
        pkt_INIT pkt = new pkt_INIT(router_id);
        byte [] content = pkt.getUDPdata();
        InetAddress ip = InetAddress.getByName(nse_host);
        try{
            DatagramPacket send_packet  = new DatagramPacket(content, content.length,ip,nse_port);
            send_socket.send(send_packet);
            router_log.printf("R"+router_id +" "+"sends an INIT: router_id "+ router_id+"\n");
            //router_log.printf("R%d sends an INIT: %d\n", router_id, router_id);
            router_log.flush();


        } catch(SocketException e){
            e.printStackTrace();
        }
    }

    public static void rec_circuitDB(DatagramSocket rec_socket) throws  Exception{
        /*
        System.out.println(router_id + "rec1");
        byte[] rec_data = new byte[512]; // since 500 for data, 4 for seq, 4 for type, 4 for data_length
        DatagramPacket r_packet = new DatagramPacket(rec_data, rec_data.length);
        System.out.println(router_id + "rec2");
        rec_socket.receive(r_packet);
        System.out.println(router_id + "rec3");
        circuit_DB  rec_DB = circuit_DB.parseUDPdata(rec_data);
        router_log.printf("R"+router_id +" "+"receives an CIRCUIT_DB: nbr_link "+ nbr_link+"\n");
        router_log.flush();
        System.out.println(router_id + "rec4");
        //router_log.println("R%d receives a CIRCUIT_DB: nbr_link %d\n", routerId, circutDB.nbr_link);
        return rec_DB;

         */

        byte[] data = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        rec_socket.receive(receivePacket);
        circuit_DB circutDB = circuit_DB.getData(receivePacket.getData());
        router_log.printf("R%d receives a CIRCUIT_DB: nbr_link %d\n",router_id, circutDB.nbr_link);
        router_log.flush();

    }

    private static void recieveCircuitDB() throws Exception {
        byte[] data = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        socket.receive(receivePacket);
        circuit_DB circutDB = circuit_DB.getData(receivePacket.getData());


        router_log.printf("R%d receives a CIRCUIT_DB: nbr_link %d\n", router_id, circutDB.nbr_link);
        router_log.flush();

    }






}

//---------------------------------------------------------------------------

class pkt_HELLO
{
    public int router_id;
    public int link_id;

    public pkt_HELLO(int router_id, int link_id){
        this.router_id = router_id;
        this.link_id = link_id;
    }

    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(8); //since it is an integer
        buffer.putInt(router_id);
        buffer.putInt(link_id);
        return buffer.array();
    }

    public static pkt_HELLO parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        int router_id = buffer.getInt();
        int link_id = buffer.getInt();
        return new pkt_HELLO(router_id,link_id);
    }
}


class pkt_LSPDU
{
    public int sender;
    public int router_id;
    public int link_id;
    public int cost;
    public int via;
    /* for simplicity we consider only 5 routers */
    /* id of the router who sends the HELLO PDU */
    /* id of the link through which it is sent */
    /* sender of the LS PDU */
    /* router id */
    /* link id */
    /* cost of the link */
    /* id of the link through which the LS PDU is sent */

    public pkt_LSPDU(int sender,int router_id, int link_id, int cost, int via){
        this.sender = sender;
        this.router_id = router_id;
        this.link_id = link_id;
        this.cost = cost;
        this.via = via;
    }


    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(20); //since it is an integer
        buffer.putInt(sender);
        buffer.putInt(router_id);
        buffer.putInt(link_id);
        buffer.putInt(cost);
        buffer.putInt(via);
        return buffer.array();
    }

    public static pkt_LSPDU parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        int sender = buffer.getInt();
        int router_id = buffer.getInt();
        int link_id = buffer.getInt();
        int cost  = buffer.getInt();
        int via = buffer.getInt();

        return new pkt_LSPDU(sender,router_id,link_id,cost,via);
    }

    public void set_sender(int sender){
        this.sender = sender;
    }

    public void set_via(int via){
        this.via = via;
    }


}

class pkt_INIT {
    public int router_id;

    public pkt_INIT(int id){
        this.router_id = id;
    }

    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(4); //since it is an integer
        buffer.putInt(router_id);
        return buffer.array();
    }

    public static pkt_INIT parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        int router_id = buffer.getInt();
        return new pkt_INIT(router_id);
    }
}

class link_cost
{
    public int link;
    public int cost;

    public link_cost(int link, int cost){
        this.link = link;
        this.cost = cost;
    }
}

class circuit_DB
{
    public int nbr_link;
    public link_cost[] linkcost;
/* we assume that at most NBR_ROUTER links are attached to each router */

    public circuit_DB(int nbr_link, link_cost[] linkcost){
        this.nbr_link = nbr_link;
        this.linkcost = linkcost;
    }

    public static circuit_DB parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        int nbr_link = buffer.getInt();
        link_cost[] linkcost = new link_cost[nbr_link];

        for(int i=0;i<nbr_link;i++){
            int link = buffer.getInt();
            int cost = buffer.getInt();
            linkcost[i] = new link_cost(link,cost);
        }


        return new circuit_DB(nbr_link,linkcost);
    }


    public static circuit_DB getData(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int nbr_link = buffer.getInt();
        link_cost[] linkcost = new link_cost[nbr_link];

        for (int i=0; i < nbr_link; i++) {
            int link_id = buffer.getInt();
            int cost = buffer.getInt();
            linkcost[i] = new link_cost(link_id, cost);
        }

        return new circuit_DB(nbr_link, linkcost);
    }
}





