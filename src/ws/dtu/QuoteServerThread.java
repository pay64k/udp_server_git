package ws.dtu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.portable.IDLEntity;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class QuoteServerThread extends Thread {

    int seq0=0,seq1=1,seq2=2,seq3=3;
    String data0="This ",data1="is ",data2="sample ",data3="text.";
    int pkt_amount=4;
    
    
    
    Map<Integer,String> map = new TreeMap<Integer, String>();
    
    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean moreQuotes = true;
    
    //States---------------------------
    private enum State{IDLE, WFR1, WFR2, STREAM, TEST};
        State currentState;
        State nextState=State.IDLE;//---------IDLE be default
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
        System.err.println("Server running!");
//        try {
//            in = new BufferedReader(new FileReader("one-liners.txt"));
//        } catch (FileNotFoundException e) {
//            System.err.println("Could not open quote file. Serving time instead.");
//        }
    }

    public void run() {
         //start timer:
           timer.start();
    while(true){
        try {
           
            
            currentState=nextState;
            
            switch(currentState){
                case IDLE:
                   // System.out.println("In State IDlE");
                    socket.receive(packet);
                    timer.reset();
                    received = new String(packet.getData(), 0, packet.getLength());
                    //System.out.println("Recieved data: " + received);
                    
                    if(received.equals("REQUEST:")){
                        
                        timer.reset();
                        //System.out.println("Change state to WFR1!");
                                             
                        //map = PrepareFile();
                        map = PrepareAnyFile(ReadFile(), 32);

                        SendPacket("pkt_amount:"+pkt_amount,packet);
                       // System.out.println("Sending pkt_amount..");
                        nextState=State.WFR1;
                    }

                    break;
                    
                case WFR1:
                    //System.out.println("In state WFR1!");
                    socket.receive(packet);
                    received = new String(packet.getData(), 0, packet.getLength());
                    //System.out.println("Recieved data: " + received); 
                    if (received.equals("REQUEST:")) {
                       SendPacket("pkt_amount:"+pkt_amount,packet);
                        //System.out.println("Sending pkt_amount..");
                       timer.reset();
                    }
                    else if (received.equals("ACK")) {
                        nextState=State.STREAM;
                        //send all pkts in the first try:
                        int ii=0;
                        for(Map.Entry<Integer,String> entry : map.entrySet()) {
                              //System.out.println(entry.getKey() + " => " + entry.getValue());
                             pkts_num_to_send.add(entry.getKey());
                             //System.out.println("Pkt_num to send: " + pkts_num_to_send.get(ii));
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
                    //System.out.println("In State STREAM");
                   
                    SendStream(map, pkts_num_to_send);
                    
                    //System.out.println("Sending: sent_all");
                    SendPacket("sent_all", packet);
                     //clear missing pkts array:
                    pkts_num_to_send.clear();
                    nextState=State.WFR2;
                    break;
                    
                case WFR2:
                    //System.out.println("In State WFR2");
                    
                    socket.receive(packet);
                    timer.reset();
                    received = new String(packet.getData(), 0, packet.getLength());
                   //System.out.println("Recieved: " + received);
                    if (received.contains("END")) {
                        SendPacket("ACK", packet);
                       // System.out.println("WFR2 -> STREAM");
                        nextState=State.STREAM;
                    }
                    else if(received.contains("got_all_pkts")){
                        //System.out.println("no_more_to_send!");
                        nextState=State.IDLE;
                    }
                    else if (received.contains("is_all_sent?")) {
                       // System.out.println("Sending: sent_all");
                        SendPacket("sent_all", packet);
                    }
                    else if(received.equals("ACK")){
                        nextState=State.STREAM;
                    }
                    else if (received.contains("REQUEST:")) {
                        
                    }
                    else{
                        //store missing pkt_num
                        //System.out.println("Missing pkt: "+received);
                        if(pkts_num_to_send.contains(received)){
                            //System.out.println("Already stored missing pkt nr: " + received);
                        }
                        else{
                        pkts_num_to_send.add(received);
                        }
                    }
                    break;
                    
                case TEST:
                    //read file:
                    byte[] file = ReadFile();
                    
                    PrepareAnyFile(ReadFile(),8);
                    //print bytes:
                    //System.out.println(Arrays.toString(file));
                    //convert to readable characters:
                    String testString = new String(file);
                    //print them:
                    //System.out.println(1151%8);
                    nextState=State.IDLE;
                    System.exit(1);
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
    
    public Map PrepareAnyFile(byte [] byte_array, int packet_lenght){
        System.out.println("Array size: " + byte_array.length);
        //See if byte array is dividable by 8:
        int array_length = byte_array.length;
        int modulo = array_length % packet_lenght;
        if (modulo==0) {
            System.out.println("Dividable by " + packet_lenght);
            Map<Integer,String> file_map = new TreeMap<Integer, String>();  
            for(int i=0; i < byte_array.length/packet_lenght; i++)
                {
                    byte[] result = new byte[packet_lenght];
                    for(int j=0; j<packet_lenght; j++){
                        result[j] = byte_array[packet_lenght*i+j];
                        }               
                
                try {
                    file_map.put(i, new String(result, "UTF-8"));
                    timer.reset();
                    //System.out.print(new String(result));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                }
                System.out.println("Map size: " + file_map.size());
                pkt_amount=file_map.size();
                return file_map;
        }
        else{
            System.out.println("Not dividable by "+ packet_lenght+ " , modulo: "+ modulo);
            int zeros_amount = packet_lenght - modulo;
            //new longer array:
            byte [] new_array = new byte[byte_array.length+zeros_amount];
            //copy old array into new longer array:          
            System.arraycopy(byte_array, 0, new_array, 0, byte_array.length);
            //pad characters:
            for (int i = array_length; i < new_array.length; i++) {
                new_array[i]=0x00;
            }
            System.out.println("Padded array lenght: " + new_array.length);
            //System.out.println(new String(new_array));
            //convert to 8 bytes Strings:
            Map<Integer,String> file_map = new TreeMap<Integer, String>();
                        
                for(int i=0; i < new_array.length/packet_lenght; i++)
                {
                    byte[] result = new byte[packet_lenght];
                    for(int j=0; j<packet_lenght; j++){
                        result[j] = new_array[packet_lenght*i+j];
                        }               
                
                try {
                    file_map.put(i, new String(result, "UTF-8"));
                    timer.reset();
                    //System.out.print(new String(result));
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                }
                //System.out.println("Map size: " + file_map.size());
                pkt_amount=file_map.size();
                return file_map;
                }
    }
    
    public byte[] ReadFile(){
        Path path = Paths.get("C:/testJava/test.txt");
        try {
            byte[] data = Files.readAllBytes(path);
            return data;
        } catch (IOException ex) {
            Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
            
            System.exit(1);
            return null;
        }
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
        //System.out.println("Sending: " + temp);
        
        
        }
    }
}