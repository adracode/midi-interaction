package fr.adracode.piano.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;

public class OneTimeStop implements AutoCloseable {
    private boolean stop;
    private Thread thread;
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private Supplier<Boolean> runIf = () -> true;
    private TerminalInteraction stopIfTerminal = terminal -> false;
    private boolean blocking = true;
    private int waiting = 100;
    
    public OneTimeStop setBlocking(boolean blocking){
        this.blocking = blocking;
        return this;
    }
    
    public OneTimeStop setStopOnInteraction(final TerminalInteraction stopIf){
        this.stopIfTerminal = stopIf;
        return this;
    }
    
    public OneTimeStop setRunningCondition(final Supplier<Boolean> runningCondition){
        this.runIf = runningCondition;
        return this;
    }
    
    public OneTimeStop setWaitingTime(int millisecond){
        this.waiting = millisecond;
        return this;
    }
    
    public OneTimeStop block(){
        final Runnable run = () -> {
            while(true){
                try {
                    if(!runIf.get() || stopIfTerminal.onInteraction(reader)){
                        stop = true;
                        break;
                    }
                    Thread.sleep(waiting);
                } catch(InterruptedException e){
                    break;
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        };
        if(blocking){
            run.run();
        } else {
            thread = new Thread(run);
            thread.start();
        }
        return this;
    }
    
    public boolean hasStopped(){
        return stop;
    }
    
    @Override
    public void close() throws IOException{
        thread.interrupt();
        reader.close();
    }
    
    @FunctionalInterface
    public interface TerminalInteraction {
        boolean onInteraction(BufferedReader terminal) throws IOException;
    }
}
