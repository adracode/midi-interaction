package fr.adracode.piano.keyboard.config;

import java.util.Objects;
import java.util.Optional;

public class KeyLeaf<T> extends KeyNode<T> {

    private final T value;

    public KeyLeaf(int octave, int tone, T value){
        super(octave, tone);
        this.value = value;
    }

    @Override
    public KeyNode<T> addChild(KeyNode<T> keyNode){
        throw new IllegalStateException("Can't add children to a leaf");
    }

    @Override
    public Optional<T> getValue(){
        return Optional.of(value);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof KeyLeaf<?> keyLeaf)) return false;
        if(!super.equals(o)) return false;
        return Objects.equals(value, keyLeaf.value);
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString(){
        return super.toString() + " - KeyLeaf{" +
                "value=" + value +
                '}';
    }
}
