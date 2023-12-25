package fr.adracode.piano.keyboard.key;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ToggleKey extends Key {
    private static final ToggleKey[] EMPTY = new ToggleKey[0];

    private static byte ID = 0;

    public static final ToggleKey NO_TOGGLE;

    static{
        NO_TOGGLE = new ToggleKey(0, 0, "NO_TOGGLE", 0, 0);
    }

    private final String label;
    private final long id;
    private Fallback fallback;

    public ToggleKey(int octave, int note, String label){
        this(octave, note, label, Key.getKeyCode(label).orElse(0), 1L << ID++);
    }

    private ToggleKey(int octave, int note, String label, int keyCode, long id){
        super(octave, note);
        if(ID == 63){
            throw new IllegalStateException("Toggled keys are limited to 64");
        }
        addResult(keyCode, 0);
        this.label = label;
        this.id = id;
    }

    public long getId(){
        return id;
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

    public static Optional<Fallback> getFallback(Collection<ToggleKey> toggleKeys){
        return toggleKeys.stream()
                .map(ToggleKey::getFallback)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream().findFirst();
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

    public enum Fallback implements Comparable<Fallback> {
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
