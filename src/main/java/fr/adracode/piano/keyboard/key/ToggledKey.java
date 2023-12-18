package fr.adracode.piano.keyboard.key;

import java.awt.event.KeyEvent;
import java.util.*;

public class ToggledKey {
    private static final ToggledKey[] EMPTY = new ToggledKey[0];

    private static final Map<String, ToggledKey> universe = new HashMap<>();

    private static byte ID = 0;

    private final String label;
    private final long id;
    private int keyCode;

    public ToggledKey(String label, int keyCode){
        if(ID == 63){
            throw new IllegalStateException("Toggled keys are limited to 64");
        }
        this.label = label;
        this.keyCode = keyCode;
        this.id = (long)Math.pow(2, ID++);
    }

    public long getId(){
        return id;
    }

    public OptionalInt getKeyCode(){
        return keyCode == 0 ? OptionalInt.empty() : OptionalInt.of(keyCode);
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

    public static long toggle(long toggled, ToggledKey key){
        if(isToggleOn(toggled, key)){
            toggled &= ~key.getId();
        } else {
            toggled |= key.getId();
        }
        return toggled;
    }

    public static boolean isToggleOn(long toggled, ToggledKey key){
        return (toggled & key.getId()) != 0;
    }

    public static ToggledKey get(String label){
        if(label == null || label.isBlank()){
            return null;
        }
        return Optional.ofNullable(universe.get(label))
                .orElseGet(() -> {
                    int keyCode = 0;
                    try {
                        keyCode = (int)KeyEvent.class.getField("VK_" + label).get(null);
                    } catch(IllegalAccessException | NoSuchFieldException ignored){ }
                    ToggledKey newKey = new ToggledKey(label, keyCode);
                    universe.put(label, newKey);
                    return newKey;
                });
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof ToggledKey that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode(){
        return Long.hashCode(id);
    }
}
