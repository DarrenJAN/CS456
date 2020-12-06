import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Vector;
import java.lang.*;


public class router {
    //set INF 65535
    public static final int NBR_ROUTER =5 ;
    public static final int LARGE_NUM = 65535;

    //Import information
    public static int router_id;
    public static String nse_host;
    public static int nse_port;
    public static int router_port;

    private  static PrintWriter router_log; //output log file
    public static int num_neighbour ;
    public static Packet.link_cost[] neighbour_link; //store neighbour's link
    public static Packet.link_cost[][] shortest_path; //store RIB information

    //Vector
    public static Vector<Integer> neighbour_id; //store neighbour's id
    public static Vector<Packet.pkt_LSPDU> lspduVector; //store all Packet.pkt_LSPDU information


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

        //Some initializations
        String file = "router"+router_id +".log";
        router_log = new PrintWriter(file);
        DatagramSocket socket = new DatagramSocket();
        lspduVector = new Vector<>();
        neighbour_id = new Vector<>();

        shortest_path= new Packet.link_cost[NBR_ROUTER][NBR_ROUTER];
        for(int i = 0; i<NBR_ROUTER;i++){
            for(int j=0;j<NBR_ROUTER;j++){
                if(i == j){
                    shortest_path[i][j] = new Packet.link_cost(LARGE_NUM, 0);
                } else {
                    shortest_path[i][j] = new Packet.link_cost(LARGE_NUM,LARGE_NUM);
                }
            }
        }

        //1. send the init packet to NSE
        send_init(socket);
        router_log.printf("R"+router_id +" "+"sends an INIT: router_id "+ router_id + "\n");
        router_log.flush();

        //2. Receive the cirecult_db packet from NSE
        Packet.circuit_DB circuit_db =  rec_circuitDB(socket);
        router_log.printf("R"+router_id +" "+"receives a CIRCUIT_DB: nbr_link "+ circuit_db.nbr_link+ "\n");
        router_log.flush();

        //----------------------------
         num_neighbour =circuit_db.nbr_link;
         neighbour_link= new Packet.link_cost[num_neighbour];
         neighbour_link = circuit_db.linkcost;


         //3. Send the Hello pkt to its neighbour
        for(int i = 0; i< num_neighbour;i++){
            int link_id = neighbour_link[i].link;
            int link_cost = neighbour_link[i].cost;
            Packet.pkt_LSPDU pkt_lspdu = new Packet.pkt_LSPDU(0,router_id,link_id,link_cost,0);
            lspduVector.add(pkt_lspdu); //add this LSPDU to vector

            send_hello(socket,router_id,link_id);
            router_log.printf("R"+router_id +" "+"sends a HELLO: router_id "+ router_id + " " + "link_id " + link_id+ "\n");
            router_log.flush();
        }
        Topology_DB();
        RIB();

        //4.Receive pkt_hello or pkt_lspdu from its neighbour
        int MAX_REC = 14; //number of edges *2 = 7*2 = 14
        while(lspduVector.size() < MAX_REC ){
            //receive the packet
            byte[] rec_data = new byte[512];
            DatagramPacket datagramPacket = new DatagramPacket(rec_data,rec_data.length);
            socket.receive(datagramPacket);
            int length = datagramPacket.getLength();
            //if this is a Hello packet
            if(length <= 8){
                Packet.pkt_HELLO pkt_hello = Packet.pkt_HELLO.parseUDPdata(rec_data);
                int r_id  = pkt_hello.router_id;
                int link_id = pkt_hello.link_id;
                router_log.printf("R"+router_id +" "+"receives a HELLO: router_id "+ r_id + " " + "link_id " + link_id+ "\n");
                router_log.flush();

                //update neighbour's id information and shortest path information
                neighbour_id.add(r_id);
                int link_cost = get_cost(link_id);
                shortest_path[router_id-1][r_id-1] = new Packet.link_cost(link_id,link_cost);
                shortest_path[r_id-1][router_id-1] = new Packet.link_cost(link_id,link_cost);

                //send a set of its LS_PDU to that neighbour
                for(int i = 0;i<lspduVector.size();i++){
                    Packet.pkt_LSPDU pkt_lspdu = lspduVector.get(i);
                    pkt_lspdu.set_sender(router_id);  //set sender
                    pkt_lspdu.set_via(pkt_lspdu.link_id); //set via link id
                    send_LSPDU(socket,pkt_lspdu);
                }
            }
            //else if this is a LSPDU_pkt
            else {
                Packet.pkt_LSPDU pkt_lspdu = Packet.pkt_LSPDU.parseUDPdata(rec_data);
                int sender = pkt_lspdu.sender;
                int that_neighbour_id = pkt_lspdu.router_id;
                int link_id = pkt_lspdu.link_id;
                int link_cost = pkt_lspdu.cost;
                int via = pkt_lspdu.via;
                router_log.printf("R"+router_id +" "+"receives a LSPDU: sender " + sender+ " " + "router_id " + that_neighbour_id + " " + "link_id " + link_id+ " "+ "Packet.link_cost "+link_cost + " via "+ via +   "\n");
                router_log.flush();

                //check if this pkt is unique LSPDU information
                boolean unique = true;
                for(int i = 0;i<lspduVector.size();i++){
                    if(lspduVector.get(i).router_id == pkt_lspdu.router_id && lspduVector.get(i).link_id == pkt_lspdu.link_id && lspduVector.get(i).cost == pkt_lspdu.cost){
                        unique = false;
                    }
                }

                //if it is unique, add it to LSPDU information and inform the rest of neighbour the new LSPDU, change the sender and via
                if(unique){
                    lspduVector.add(pkt_lspdu);
                    Packet.pkt_LSPDU send_lspdu = pkt_lspdu;
                    send_lspdu.set_sender(router_id);   //set sender
                    for(int i =0;i<num_neighbour;i++){
                        if(that_neighbour_id != neighbour_link[i].link){  //ignore the neighbour who sent this LSPDU
                            send_lspdu.set_via(neighbour_link[i].link);   //set via
                            send_LSPDU(socket,send_lspdu);
                        }
                    }
                    Topology_DB(); //Logging Topology
                    RIB();  //Shortest path calculated and logging RIB
                }

            }
        }
    }

    //---------some helper functions----------
    public static int get_cost(int link_id){
        int cost = LARGE_NUM;
        for(int i = 0;i<neighbour_link.length;i++){
            if(neighbour_link[i].link == link_id)
                cost =  neighbour_link[i].cost;
        }
        return  cost;
    }

    public static void send_init(DatagramSocket send_socket) throws  Exception{
        try{
            //create init packet
            Packet.pkt_INIT pkt_init = new Packet.pkt_INIT(router_id);
            byte []content = pkt_init.getUDPdata();
            InetAddress ip = InetAddress.getByName(nse_host);
            DatagramPacket datagramPacket = new DatagramPacket(content,content.length,ip,nse_port);
            send_socket.send(datagramPacket);
        } catch (Exception e ){
            e.printStackTrace();
        }

    }

    public static Packet.circuit_DB rec_circuitDB(DatagramSocket rec_socket) throws Exception {
        byte[] rec_data = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(rec_data, rec_data.length);
        rec_socket.receive(receivePacket);
        Packet.circuit_DB circutDB = Packet.circuit_DB.parseUDPdata(rec_data);
        return circutDB;

    }

    public static void send_hello(DatagramSocket send_socket,int router_id, int link_id) throws  Exception {
        try {
            //create Hello packet
            Packet.pkt_HELLO pkt_hello = new Packet.pkt_HELLO(router_id, link_id);
            byte[] content = pkt_hello.getUDPdata();
            InetAddress ip = InetAddress.getByName(nse_host);
            DatagramPacket send_packet = new DatagramPacket(content, content.length, ip, nse_port);
            send_socket.send(send_packet);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void send_LSPDU(DatagramSocket send_socket, Packet.pkt_LSPDU pkt_lspdu) throws  Exception{
        int r_id = pkt_lspdu.router_id;
        int link_id = pkt_lspdu.link_id;
        int link_cost = pkt_lspdu.cost;
        int via = pkt_lspdu.via;
        byte [] content = pkt_lspdu.getUDPdata();
        InetAddress ip = InetAddress.getByName(nse_host);
        try {
            DatagramPacket send_packet = new DatagramPacket(content,content.length, ip,nse_port);
            send_socket.send(send_packet);
            router_log.printf("R"+router_id +" "+"sends a LSPDU: sender " + router_id+ " " + "router_id " + r_id + " " + "link_id " + link_id+ " "+ "Packet.link_cost "+link_cost + " via "+ via +   "\n");
            router_log.flush();
        } catch (Exception e  ){
            e.printStackTrace();
        }

    }

    //we need log each topology_DB each time we change it
    public static void Topology_DB(){
        for(int i = 0;i<NBR_ROUTER;i++){
            int cur_router_id =  i+1;
            int cur_length = 0;
            Vector<Packet.pkt_LSPDU> tmp = new Vector<>();
            for(int j = 0 ;j<lspduVector.size();j++){
                if(cur_router_id == lspduVector.get(j).router_id){
                    tmp.add(lspduVector.get(j));
                    cur_length++;
                }
            }
            if(cur_length == 0)
                continue;
            else{
                router_log.printf("R"+router_id+" -> R"+cur_router_id+" nbr link "+cur_length + "\n");
                router_log.flush();
                print_Topology(tmp,cur_router_id,cur_length);

            }


        }
    }


    public static void print_Topology(Vector<Packet.pkt_LSPDU> tmp, int cur_router_id, int cur_length){
        for(int i= 0;i<cur_length;i++){
            Packet.pkt_LSPDU pkt = tmp.get(i);
            int link_id = pkt.link_id;
            int cost = pkt.cost;
            router_log.printf("R"+router_id +" -> R"+ cur_router_id+ " link " + link_id + " cost "+ cost + "\n" );
            router_log.flush();
        }
    }

    public static void RIB(){
        //update shortest path information
        for(int i =0;i<lspduVector.size();i++){
            for(int j = 0;j<lspduVector.size();j++){
                if(i !=j && lspduVector.get(i).link_id == lspduVector.get(j).link_id){
                    int link_cost = lspduVector.get(i).cost;
                    int link_id = lspduVector.get(i).link_id;
                    int first_router = lspduVector.get(i).router_id;
                    int second_router = lspduVector.get(j).router_id;
                    shortest_path[first_router-1][second_router-1]  = new Packet.link_cost(link_id,link_cost);
                    shortest_path[second_router-1][first_router-1] =  new Packet.link_cost(link_id,link_cost);
                }
            }
        }
        //compute the shortest path
        dijkstra(shortest_path,router_id-1);


    }

    public static void dijkstra(Packet.link_cost shortest_path[][], int src){
        Boolean used[] = new Boolean[NBR_ROUTER];
        int dist[] = new int[NBR_ROUTER]; // The output array. dist[i] will hold
        int predecessor[]  = new int[]{-1,-1,-1,-1,-1};


        for (int i = 0; i < NBR_ROUTER; i++) {
            if(i == src)
                dist[src] = 0;
            else
                dist[i] = LARGE_NUM;
            used[i] = false;
        }


        for (int i  = 0; i < NBR_ROUTER-1; i++) {
            //we choose node that has the minimum distance from src
            int u = get_min_index(dist, used);
            used[u] = true;

            for (int v = 0; v < NBR_ROUTER; v++) {
                Packet.link_cost cur_link = shortest_path[u][v];

                if (dist[u] != LARGE_NUM &&  used[v]== false && cur_link.cost != 0  ) {
                    if((dist[u] + cur_link.cost < dist[v])){
                        dist[v] = dist[u] + cur_link.cost;
                        predecessor[v] = u;
                    }
                }
            }
        }

        //logging the RIB information
        for (int i = 0; i < NBR_ROUTER; i++) {
            int cur_id = i+1;
            //Local, 0
            if(cur_id == router_id){
                router_log.printf("R" +router_id+ " -> R" + cur_id + " -> "+ "Local, " + 0+"\n");
                router_log.flush();
            }else {
                if(dist[i] == LARGE_NUM) {
                    router_log.printf("R" +router_id+ " -> R" + cur_id + " -> " + "INF, "  +  LARGE_NUM + "\n");
                    router_log.flush();
                }else {
                    int next_id =get_next_id(predecessor,i,router_id-1);
                    router_log.printf("R" +router_id+ " -> R" + cur_id + " -> "+ "R"+next_id +", " +  dist[i]+"\n");
                    router_log.flush();

                }

            }

        }

    }

    public static int get_min_index(int dist[], Boolean used[])
    {
        int min_index = 0;
        int min_num = LARGE_NUM;
        for (int i = 0; i < NBR_ROUTER; i++)
            if (dist[i] < min_num && used[i] == false ) {
                min_num = dist[i];
                min_index = i;
            }

        return min_index;
    }



    public static int get_next_id( int [] predecessor, int src, int des){
        int tmp_src = src;
        while(predecessor[tmp_src] != des){
            tmp_src = predecessor[tmp_src];
        }
        return tmp_src+1;
    }




}




