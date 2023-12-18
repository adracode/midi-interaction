package fr.adracode.piano.keyboard.key;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ToggledKey {
    private static final ToggledKey[] EMPTY = new ToggledKey[0];

    private static final Map<String, ToggledKey> universe = new HashMap<>();

    private static byte ID = 0;

    private final String label;
    private final long id;

    public ToggledKey(String label){
        if(ID == 63){
            throw new IllegalStateException("Toggled keys are limited to 64");
        }
        this.label = label;
        this.id = (long)Math.pow(2, ID++);
    }

    public long getId(){
        return id;
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
        if((toggled & key.getId()) == 0){
            toggled |= key.getId();
        } else {
            toggled &= ~key.getId();
        }
        return toggled;
    }

    public static ToggledKey get(String label){
        if(label == null || label.isBlank()){
            return null;
        }
        return Optional.ofNullable(universe.get(label))
                .orElseGet(() -> {
                    ToggledKey newKey = new ToggledKey(label);
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
