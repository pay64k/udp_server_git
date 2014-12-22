/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ws.dtu;

/**
 *
 * @author Kubala  
 */

public class ServerState
{
    private State currentState;

    /**
     * @return the currentState
     */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * @param currentState the currentState to set
     */
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
    
    enum State  { 
        IDLE {
            public State next(String message) {
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
            public State next(String message) {

                return FAIL;
            }
        },
         //Wait For Reply 2
         WFR2{
            public State next(String message) {

                return FAIL;
            }
        },
         //Wait For Reply 2
         STREAM{
            public State next(String message) {

                return FAIL;
            }
        },
        FAIL {
            public State next(String message) {
                return IDLE;
            }
        };
        
        public abstract State next(String message);
    }
}