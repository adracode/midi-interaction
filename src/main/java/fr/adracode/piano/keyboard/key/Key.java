package fr.adracode.piano.keyboard.key;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public class Key {
    private final int octave;
    private final int tone;
    private Long2ObjectMap<KeyAction> actions;

    public Key(int octave, int tone){
        this.octave = octave;
        this.tone = tone;
    }

    public void addResult(String asString, long togglesId){
        addResult(action -> action.result(asString), togglesId);
    }

    public void addResult(int asKey, long togglesId){
        if(asKey == 0){
            return;
        }
        addResult(action -> action.keyCode(asKey), togglesId);
    }

    private void addResult(UnaryOperator<KeyAction.Builder> edit, long togglesId){
        if(actions == null){
            actions = new Long2ObjectOpenHashMap<>();
        }
        actions.put(togglesId, edit.apply(new KeyAction.Builder(actions.get(togglesId))).build());
    }

    public Optional<KeyAction> getAction(ToggleKey toggle){
        return getAction(List.of(toggle));
    }

    public Optional<KeyAction> getAction(Collection<ToggleKey> toggles){
        return getAction(ToggleKey.of(toggles));
    }

    public Optional<KeyAction> getAction(long toggles){
        if(actions == null){
            return Optional.empty();
        }
        return Optional.ofNullable(actions.get(toggles));
    }

    public int getOctave(){
        return octave;
    }

    public int getTone(){
        return tone;
    }

    public long getKey(){
        return from(octave, tone);
    }

    @Override
    public boolean equals(Object obj){
        if(obj == this)
            return true;
        if(obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (Key)obj;
        return this.octave == that.octave &&
                this.tone == that.tone;
    }

    @Override
    public int hashCode(){
        return Objects.hash(octave, tone);
    }

    @Override
    public String toString(){
        return "Key[" +
                "octave=" + octave + ", " +
                "tone=" + tone + ']';
    }

    public static long from(int octave, int tone){
        return ((long)octave << 32) | tone;
    }

    public static Key to(long key){
        return new Key((int)(key >> 32), (int)(key));
    }

    public static Optional<Integer> getKeyCode(String key){
        try {
            return Optional.of((int)KeyEvent.class.getField("VK_" + key).get(null));
        } catch(NoSuchFieldException | IllegalAccessException $){
            try {
                return Optional.of(-Integer.parseInt(key));
            } catch(NumberFormatException $_){
                return Optional.empty();
            }
        }
    }

    public static IntStream binaryStream(int number){
        return IntStream.range(0, 32)
                .map(i -> (number >> i) & 1);
    }

    public static IntStream weightedBinaryStream(long number){
        return IntStream.range(0, 64)
                .map(i -> (int)((number >> i) & 1) * (1 << i));
    }


}
