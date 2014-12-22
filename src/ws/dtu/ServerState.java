/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ws.dtu;

/**
 *
 * @author Kubala  
 */

public enum ServerState  { 
    IDLE {
        @Override
        public ServerState next(String message) {
//            switch(message)
//            {
//                case "rcv_pkt": return FAIL;
//                default: return FAIL;
//            }
            return FAIL;
        }
    },
    //Wait For Reply 1
    WFR1{
        @Override
        public ServerState next(String message) {
            
            return FAIL;
        }
    },
     //Wait For Reply 2
     WFR2{
        @Override
        public ServerState next(String message) {
            
            return FAIL;
        }
    },
     //Wait For Reply 2
     STREAM{
        @Override
        public ServerState next(String message) {
            
            return FAIL;
        }
    },
    FAIL {
        @Override
        public ServerState next(String message) {
            return IDLE;
        }
    };

    public abstract ServerState next(String message);
}