package fr.adracode.piano.keyboard.key;

import java.util.*;
import java.util.stream.Collectors;

public class ToggleKey {
    private static final ToggleKey[] EMPTY = new ToggleKey[0];

    private static final Map<String, ToggleKey> universe = new HashMap<>();
    private static final List<ToggleKey> universeById = new ArrayList<>();

    private static byte ID = 0;

    private final String label;
    private final long id;
    private final int keyCode;
    private Fallback fallback;

    public ToggleKey(String label, int keyCode){
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

    public Fallback getFallback(){
        return fallback;
    }

    public void setFallback(Fallback fallback){
        this.fallback = fallback;
    }

    public static byte getNextId(){
        return ID;
    }

    public static long of(Collection<ToggleKey> keys){
        return of(keys.toArray(EMPTY));
    }

    public static long of(ToggleKey... keys){
        long key = 0;
        for(ToggleKey toggledKey : keys){
            key |= toggledKey.id;
        }
        return key;
    }

    public static Set<ToggleKey> from(long keys){
        return Key.weightedBinaryStream(keys).filter(bit -> bit > 0).mapToObj(ToggleKey::get).collect(Collectors.toSet());
    }

    public static long toggle(long toggled, long keyId){
        return toggled ^ keyId;
    }

    public static boolean isToggleOn(long toggled, long keyId){
        return (toggled & keyId) != 0;
    }

    public static ToggleKey get(String label){
        if(label == null || label.isBlank()){
            return null;
        }
        return Optional.ofNullable(universe.get(label))
                .orElseGet(() -> {
                    ToggleKey newKey = new ToggleKey(label, Key.getKeyCode(label).orElse(0));
                    universe.put(label, newKey);
                    universeById.add(newKey);
                    return newKey;
                });
    }

    public static ToggleKey get(long id){
        return universeById.get(Long.numberOfTrailingZeros(id));
    }

    public static Optional<Fallback> getFallback(long id){
        Set<ToggleKey> toggleKey = from(id);
        if(toggleKey.size() != 1){
            return Optional.empty();
        }
        return Optional.ofNullable(toggleKey.stream().findFirst().get().getFallback());
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(!(o instanceof ToggleKey that))
            return false;
        return id == that.id;
    }

    @Override
    public int hashCode(){
        return Long.hashCode(id);
    }

    public enum Fallback {
        NORMAL;

        public static Optional<Fallback> of(String value){
            try {
                return Optional.of(valueOf(value));
            } catch(IllegalArgumentException | NullPointerException e){
                return Optional.empty();
            }
        }
    }
}
