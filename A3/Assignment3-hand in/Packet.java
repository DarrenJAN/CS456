import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet {
    static class circuit_DB {
        public int nbr_link; /* number of links attached to a router */
        public link_cost[] linkcost; /* we assume that at most NBR_ROUTER links are attached to each router */

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

    static class pkt_LSPDU
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
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(sender);
            buffer.putInt(router_id);
            buffer.putInt(link_id);
            buffer.putInt(cost);
            buffer.putInt(via);
            return buffer.array();
        }

        public static pkt_LSPDU parseUDPdata(byte[] UDPdata) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
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

    static class pkt_INIT {
        public int router_id;

        public pkt_INIT(int id){
            this.router_id = id;
        }

        public byte[] getUDPdata() {
            ByteBuffer buffer = ByteBuffer.allocate(4); //since it is an integer
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(router_id);
            return buffer.array();
        }

        public static pkt_INIT parseUDPdata(byte[] UDPdata) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int router_id = buffer.getInt();
            return new pkt_INIT(router_id);
        }
    }

    static class pkt_HELLO
    {
        public int router_id;
        public int link_id;

        public pkt_HELLO(int router_id, int link_id){
            this.router_id = router_id;
            this.link_id = link_id;
        }

        public byte[] getUDPdata() {
            ByteBuffer buffer = ByteBuffer.allocate(8); //since it is an integer
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(router_id);
            buffer.putInt(link_id);
            return buffer.array();
        }

        public static pkt_HELLO parseUDPdata(byte[] UDPdata) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int router_id = buffer.getInt();
            int link_id = buffer.getInt();
            return new pkt_HELLO(router_id,link_id);
        }
    }

    static class link_cost
    {
        public int link;
        public int cost;

        public link_cost(int link, int cost){
            this.link = link;
            this.cost = cost;
        }
    }
}

