package fr.adracode.piano.keyboard.config;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.Objects;
import java.util.Optional;

public class KeyNode<T> {

    private final int octave;
    private final int tone;

    private Long2ObjectMap<KeyNode<T>> children;

    public KeyNode(int octave, int tone){
        this.octave = octave;
        this.tone = tone;
    }

    public KeyNode<T> addChild(KeyNode<T> keyNode){
        if(children == null){
            children = new Long2ObjectArrayMap<>();
        }
        children.put(Key.from(keyNode.octave, keyNode.tone), keyNode);
        return keyNode;
    }

    public Optional<T> getResult(){
        return Optional.empty();
    }

    public Optional<KeyNode<T>> get(int octave, int tone){
        if(children == null){
            return Optional.empty();
        }
        long key = Key.from(octave, tone);
        KeyNode<T> child = children.get(key);
        return Optional.ofNullable(child);
    }

    public int getOctave(){
        return octave;
    }

    public int getTone(){
        return tone;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof KeyNode<?> keyNode)) return false;
        return octave == keyNode.octave && tone == keyNode.tone;
    }

    @Override
    public int hashCode(){
        return Objects.hash(octave, tone);
    }

    @Override
    public String toString(){
        return "KeyNode{" +
                "octave=" + octave +
                ", tone=" + tone +
                ", children=" + children +
                '}';
    }
}
