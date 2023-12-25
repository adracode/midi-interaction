package fr.adracode.piano.keyboard;

import fr.adracode.piano.keyboard.key.TimestampUsableBypass;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class KeyDecoder {

    private final Int2ObjectMap<TimestampUsableBypass> handTimestamp = new Int2ObjectArrayMap<>();
    private final int sustainAfter;

    public KeyDecoder(int sustainAfter){
        this.sustainAfter = sustainAfter;
    }

    public void registerKey(int data){
        handTimestamp.put(data, new TimestampUsableBypass(System.currentTimeMillis()));
    }

    public void unregisterKey(int data){
        handTimestamp.remove(data);
    }

    public int getHand(){
        long sustainInterval = System.currentTimeMillis() - sustainAfter;
        return handTimestamp.int2ObjectEntrySet().stream()
                .filter(entry -> entry.getValue() != null && (entry.getValue().consume() || entry.getValue().timestamp() < sustainInterval))
                .map(Int2ObjectMap.Entry::getIntKey)
                .reduce(0, this::mergeHand);
    }

    public boolean hasKey(){
        return !handTimestamp.isEmpty();
    }

    private int mergeHand(int acc, int data){
        //TODO: simplify (TONE - (data - octave * TONE) ...)
        return acc | switch(data){
            case 0 -> 0b100000000000;
            case 1 -> 0b010000000000;
            case 2 -> 0b001000000000;
            case 3 -> 0b000100000000;
            case 4 -> 0b000010000000;
            case 5 -> 0b000001000000;
            case 6 -> 0b000000100000;
            case 7 -> 0b000000010000;
            case 8 -> 0b000000001000;
            case 9 -> 0b000000000100;
            case 10 -> 0b000000000010;
            case 11 -> 0b000000000001;
            default -> 0;
        };
    }

}
