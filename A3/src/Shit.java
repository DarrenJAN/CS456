/*
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class router {
    public static final int NBR_ROUTER =5 ;
    public static final int LARGE_NUM = 65535;

    public static int router_id;
    public static String nse_host;
    public static int nse_port;
    public static int router_port;
    private static DatagramSocket socket;
    private  static PrintWriter routerLog;


    public static void main(String[] args) throws  Exception{
        if (args.length != 4) {
            System.out.println("Wrong parameters");
            System.exit(0);
        } else {
            router_id = Integer.parseInt(args[0]);
            nse_host = args[1];
            nse_port = Integer.parseInt(args[2]);
            router_port = Integer.parseInt(args[3]);

        }

        socket = new DatagramSocket();
        routerLog = new PrintWriter(new FileWriter(String.format("router%d.log", router_id)), true);

        send_init();
        recieveCircuitDB();

    }

    public static void send_init() throws  Exception{
        pkt_INIT pkt_init = new pkt_INIT(router_id);
        byte []content = pkt_init.getUDPdata();
        InetAddress ip = InetAddress.getByName(nse_host);
        DatagramPacket datagramPacket = new DatagramPacket(content,content.length,ip,nse_port);
        socket.send(datagramPacket);
        routerLog.printf("R%d sends an INIT: router_id %d\n", router_id,router_id);
        routerLog.flush();
    }

    private static void recieveCircuitDB() throws Exception {
        byte[] data = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        socket.receive(receivePacket);
        circuit_DB circutDB = circuit_DB.parseUDPdata(data);
        routerLog.printf("R%d receives a CIRCUIT_DB: nbr_link %d\n", router_id, circutDB.nbr_link);
        routerLog.flush();

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

class circuit_DB {
    public int nbr_link; /* number of links attached to a router
    public link_cost[] linkcost; /* we assume that at most NBR_ROUTER links are attached to each router

    public circuit_DB(int nbr_link, link_cost[] linkcost) {
        this.nbr_link = nbr_link;
        this.linkcost = linkcost;
    }

    public static circuit_DB parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int nbr_link = buffer.getInt();
        link_cost[] linkcost = new link_cost[nbr_link];

        for(int i=0;i<nbr_link;i++){
            int link = buffer.getInt();
            int cost = buffer.getInt();
            linkcost[i] = new link_cost(link,cost);
        }


        return new circuit_DB(nbr_link,linkcost);
    }
}
