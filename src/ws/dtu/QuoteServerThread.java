package ws.dtu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.portable.IDLEntity;

public class QuoteServerThread extends Thread {

    int seq0=0,seq1=1,seq2=2,seq3=3;
    String data0="This ",data1="is ",data2="sample ",data3="text.";
    int pkt_amount=4;
    
    Map<Integer,String> map = new TreeMap<Integer, String>();
    
    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean moreQuotes = true;
    
    //States---------------------------
    private enum State{IDLE, WFR1, WFR2, STREAM};
        State currentState;
        State nextState=State.IDLE;
    //---------------------------------

        //setup timer timeout in milliseconds:
        Timer timer = new Timer(30000);
        ArrayList dataList = new ArrayList();
        ArrayList seqList = new ArrayList();
        
        ArrayList pkts_num_to_send = new ArrayList();
        
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        String reply=null;
        String received=null;

    public QuoteServerThread() throws IOException {
	this("QuoteServerThread");
    }

    public QuoteServerThread(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(4445);
                
        try {
            in = new BufferedReader(new FileReader("one-liners.txt"));
        } catch (FileNotFoundException e) {
            System.err.println("Could not open quote file. Serving time instead.");
        }
    }

    public void run() {
         //start timer:
           timer.start();
    while(true){
        try {
           
            
            currentState=nextState;
            
            switch(currentState){
                case IDLE:
                    System.out.println("In State IDlE");
                    socket.receive(packet);
                    timer.reset();
                    received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recieved data: " + received);
                    
                    if(received.equals("REQUEST:")){
                        
                        timer.reset();
                        //System.out.println("Change state to WFR1!");
                                             
                        map = PrepareFile();

                        SendPacket("pkt_amount:"+pkt_amount,packet);
                        System.out.println("Sending pkt_amount..");
                        nextState=State.WFR1;
                    }

                    break;
                    
                case WFR1:
                    System.out.println("In state WFR1!");
                    socket.receive(packet);
                    received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recieved data: " + received); 
                    if (received.equals("REQUEST:")) {
                       SendPacket("pkt_amount:"+pkt_amount,packet);
                        System.out.println("Sending pkt_amount..");
                       timer.reset();
                    }
                    else if (received.equals("ACK")) {
                        nextState=State.STREAM;
                        //send all pkts in the first try:
                        int ii=0;
                        for(Map.Entry<Integer,String> entry : map.entrySet()) {
                              //System.out.println(entry.getKey() + " => " + entry.getValue());
                             pkts_num_to_send.add(entry.getKey());
                             System.out.println("Pkt_num to send: " + pkts_num_to_send.get(ii));
                             ii++;
                        }
                        //System.out.println("STREAMING");
                        timer.reset();
                    }
                    else if (received.equals("is_all_sent?")) {
                        nextState=State.STREAM;
                    }
//                    else if (received.equals("is_all_sent?")) {
//                        nextState=State.STREAM;
//                    }
                    
                    break;
                    
                case STREAM:
                    System.out.println("In State STREAM");
                   
                    SendStream(map, pkts_num_to_send);
                    
                    System.out.println("Sending: sent_all");
                    SendPacket("sent_all", packet);
                     //clear missing pkts array:
                    pkts_num_to_send.clear();
                    nextState=State.WFR2;
                    break;
                    
                case WFR2:
                    System.out.println("In State WFR2");
                    
                    socket.receive(packet);
                    timer.reset();
                    received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recieved: " + received);
                    if (received.contains("END")) {
                        SendPacket("ACK", packet);
                        System.out.println("WFR2 -> STREAM");
                        nextState=State.STREAM;
                    }
                    else if(received.contains("got_all_pkts")){
                        System.out.println("no_more_to_send!");
                        nextState=State.IDLE;
                    }
                    else if (received.contains("is_all_sent?")) {
                        System.out.println("Sending: sent_all");
                        SendPacket("sent_all", packet);
                    }
                    else if(received.equals("ACK")){
                        nextState=State.STREAM;
                    }
                    else{
                        //store missing pkt_num
                        System.out.println("Missing pkt: "+received);
                        if(pkts_num_to_send.contains(received)){
                            System.out.println("Already stored missing pkt nr: " + received);
                        }
                        else{
                        pkts_num_to_send.add(received);
                        }
                    }
                    break;
                    
                default:
                    nextState=currentState;
                    break;
            }
            

        } catch (IOException ex) {
            Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   }
    public void SendPacket(String data, DatagramPacket packet){
        try {
            buf = new byte[256];
            buf = data.getBytes();
            // send the response to the client at "address" and "port"
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        } catch (IOException ex) {
            Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Map PrepareFile(){
        Map<Integer,String> file_map = new TreeMap<Integer, String>();
        file_map.put(0, "This ");
        file_map.put(1, "is ");
        file_map.put(2, "sample ");
        file_map.put(3, "text.");
      
        return file_map;
    }
    
    public void SendStream(Map map,ArrayList missing_pkt_num){
        for (int i = 0; i < missing_pkt_num.size(); i++) {//!!!!remove -1 - its for testing
        String temp = "|";
        temp = temp.concat(missing_pkt_num.get(i).toString());
        //temp = temp.concat(seqList.get(i).toString());
        temp = temp.concat("|");
        int missing_num=Integer.parseInt(missing_pkt_num.get(i).toString());
            //System.out.println("Converted missing int: "+missing_num);
        temp = temp.concat(map.get(missing_num).toString());
        //temp = temp.concat(dataList.get(i).toString());
        SendPacket(temp, packet);
        System.out.println("Sending: " + temp);
        
        
        }
    }
}