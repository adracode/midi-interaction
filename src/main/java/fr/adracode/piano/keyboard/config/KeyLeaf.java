package fr.adracode.piano.keyboard.config;

import java.util.Objects;
import java.util.Optional;

public class KeyLeaf<T> extends KeyNode<T> {

    private final T result;

    public KeyLeaf(int octave, int tone, T result){
        super(octave, tone);
        this.result = result;
    }

    @Override
    public KeyNode<T> addChild(KeyNode<T> keyNode){
        throw new IllegalStateException("Can't add children to a leaf");
    }

    @Override
    public Optional<T> getResult(){
        return Optional.of(result);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof KeyLeaf<?> keyLeaf)) return false;
        if(!super.equals(o)) return false;
        return Objects.equals(result, keyLeaf.result);
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), result);
    }

    @Override
    public String toString(){
        return super.toString() + " - KeyLeaf{" +
                "result=" + result +
                '}';
    }
}
