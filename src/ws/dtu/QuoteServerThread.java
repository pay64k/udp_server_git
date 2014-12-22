package ws.dtu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Object;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.portable.IDLEntity;

public class QuoteServerThread extends Thread {

    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean moreQuotes = true;
    
    //States---------------------------
    private enum State{IDLE, WFR1, WFR2, STREAM};
        State currentState;
        State nextState=State.IDLE;
    //---------------------------------

        //setup timer timeout in milliseconds:
        Timer timer = new Timer(10000);
        
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
                    socket.receive(packet);
                    received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recieved data: " + received);
                    
                    if(received.equals("REQUEST:")){
                        timer.reset();
                        System.out.println("Change state to WFR1!");
                        nextState=State.WFR1;
                        
                        SendPacket("pkt_amount:10",packet);
                    }
                    break;
                    
                case WFR1:
                    System.out.println("In state WFR1!");
                    socket.receive(packet);
                    received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Recieved data: " + received); 
                    if (received.equals("isNAK")) {
                        
                    }
                    nextState=State.IDLE;
                    System.out.println("Back to IDLE");
                    break;
                    
                default:
                    nextState=currentState;
                    break;
            }
            
/*        while (moreQuotes) {
            try {
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                System.out.println("Request from: " + packet.getAddress().toString());
                
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Recieved data: " + received);

                // figure out response
                String dString = null;
                if (in == null)
                    dString = new Date().toString();
                else
                    dString = getNextQuote();

                System.err.println("Sending: " + dString);
                buf = dString.getBytes();
                
		// send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
		moreQuotes = false;
            }
        }
        socket.close();
    }

    protected String getNextQuote() {
        String returnValue = null;
        try {
            if ((returnValue = in.readLine()) == null) {
                in.close();
		moreQuotes = false;
                returnValue = "No more quotes. Goodbye.";
            }
        } catch (IOException e) {
            returnValue = "IOException occurred in server.";
        }
        return returnValue;
*/
        } catch (IOException ex) {
            Logger.getLogger(QuoteServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   }
    public void SendPacket(String data, DatagramPacket packet){
        try {
            
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
}