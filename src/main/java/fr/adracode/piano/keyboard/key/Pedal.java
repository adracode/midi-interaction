package fr.adracode.piano.keyboard.key;

import java.util.*;

import static fr.adracode.piano.keyboard.KeyboardMapping.OCTAVE;

public class Pedal {

    private static final Map<String, Pedal> universe = new HashMap<>();
    private static int ID = 1;

    private final Key fakeKey;
    private boolean toggleMode;

    public Pedal(){
        this.fakeKey = new Key(OCTAVE - 1, ID++);
    }

    public Key getFakeKey(){
        return fakeKey;
    }

    public boolean isToggleMode(){
        return toggleMode;
    }

    public void asToggle(){
        this.toggleMode = true;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(!(o instanceof Pedal pedal))
            return false;
        return fakeKey == pedal.fakeKey;
    }

    @Override
    public int hashCode(){
        return Objects.hash(fakeKey);
    }

    public static Collection<Pedal> all(){
        return Collections.unmodifiableCollection(universe.values());
    }

    public static Pedal get(String label){
        if(label == null || label.isBlank()){
            return null;
        }
        return Optional.ofNullable(universe.get(label))
                .orElseGet(() -> {
                    Pedal newPedal = new Pedal();
                    universe.put(label, newPedal);
                    return newPedal;
                });
    }
}
