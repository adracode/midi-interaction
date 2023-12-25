package fr.adracode.piano.keyboard.key;

import java.util.Objects;

public class TimestampUsableBypass {
    private final long timestamp;
    private boolean isUsed;

    public TimestampUsableBypass(long timestamp){
        this.timestamp = timestamp;
    }

    public long timestamp(){ return timestamp; }

    public boolean consume(){
        if(isUsed){
            return false;
        } else {
            isUsed = true;
            return true;
        }
    }

    @Override
    public boolean equals(Object obj){
        if(obj == this)
            return true;
        if(obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (TimestampUsableBypass)obj;
        return this.timestamp == that.timestamp &&
                this.isUsed == that.isUsed;
    }

    @Override
    public int hashCode(){
        return Objects.hash(timestamp, isUsed);
    }

    @Override
    public String toString(){
        return "TimestampWithOnce[" +
                "timestamp=" + timestamp + ", " +
                "isUsed=" + isUsed + ']';
    }

}