package fr.adracode.piano.keyboard.key;

public class ToggleKeyState {

    private boolean isPermanent;
    private boolean onceCandidate;
    private boolean beenUsed;
    private boolean once;
    private long pressedOn;

    public boolean isPermanent(){
        return isPermanent;
    }

    public boolean isOnce(){
        return once;
    }

    public boolean isOnceCandidate(){
        return onceCandidate;
    }

    public boolean hasBeenUsed(){
        return beenUsed;
    }

    public ToggleKeyState setPermanent(boolean permanent){
        isPermanent = permanent;
        return this;
    }

    public long getPressedOn(){
        return pressedOn;
    }

    public ToggleKeyState setPressedOn(long pressedOn){
        this.pressedOn = pressedOn;
        return this;
    }

    public ToggleKeyState setOnce(boolean once){
        this.once = once;
        return this;
    }

    public void setOnceCandidate(boolean onceCandidate){
        this.onceCandidate = onceCandidate;
    }

    public void setBeenUsed(boolean beenUsed){
        this.beenUsed = beenUsed;
    }

    @Override
    public String toString(){
        return "ToggleKeyState{" +
                "isPermanent=" + isPermanent +
                ", onceCandidate=" + onceCandidate +
                ", beenUsed=" + beenUsed +
                ", once=" + once +
                ", pressedOn=" + pressedOn +
                '}';
    }
}
