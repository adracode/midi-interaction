package fr.adracode.piano.keyboard.key;

import java.util.*;

public class ToggledKey {
    private static final ToggledKey[] EMPTY = new ToggledKey[0];

    private static final Map<String, ToggledKey> universe = new HashMap<>();
    private static final List<ToggledKey> universeById = new ArrayList<>();

    private static byte ID = 0;

    private final String label;
    private final long id;
    private final int keyCode;

    public ToggledKey(String label, int keyCode){
        if(ID == 63){
            throw new IllegalStateException("Toggled keys are limited to 64");
        }
        this.label = label;
        this.keyCode = keyCode;
        //TODO: optimize
        this.id = (long)Math.pow(2, ID++);
    }

    public long getId(){
        return id;
    }

    public OptionalInt getKeyCode(){
        return keyCode == 0 ? OptionalInt.empty() : OptionalInt.of(keyCode);
    }

    public String getLabel(){
        return label;
    }

    public static byte getNextId(){
        return ID;
    }

    public static long of(Collection<ToggledKey> keys){
        return of(keys.toArray(EMPTY));
    }

    public static long of(ToggledKey... keys){
        long key = 0;
        for(ToggledKey toggledKey : keys){
            key |= toggledKey.id;
        }
        return key;
    }

    public static long toggle(long toggled, long keyId){
        return toggled ^ keyId;
    }

    public static boolean isToggleOn(long toggled, long keyId){
        return (toggled & keyId) != 0;
    }

    public static ToggledKey get(String label){
        if(label == null || label.isBlank()){
            return null;
        }
        return Optional.ofNullable(universe.get(label))
                .orElseGet(() -> {
                    ;
                    ToggledKey newKey = new ToggledKey(label, Key.getKeyCode(label).orElse(0));
                    universe.put(label, newKey);
                    universeById.add(newKey);
                    return newKey;
                });
    }

    public static ToggledKey get(long id){
        return universeById.get(Long.numberOfTrailingZeros(id));
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(!(o instanceof ToggledKey that))
            return false;
        return id == that.id;
    }

    @Override
    public int hashCode(){
        return Long.hashCode(id);
    }
}
