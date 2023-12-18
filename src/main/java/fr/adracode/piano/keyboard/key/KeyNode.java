package fr.adracode.piano.keyboard.key;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.Objects;
import java.util.Optional;

public class KeyNode<T> {

    private long key;
    private Long2ObjectMap<KeyNode<T>> children;

    public KeyNode(long key){
        this.key = key;
    }

    public KeyNode<T> addChild(KeyNode<T> keyNode){
        if(children == null){
            children = new Long2ObjectArrayMap<>();
        }
        children.put(keyNode.key, keyNode);
        return keyNode;
    }

    public Optional<T> getValue(){
        return Optional.empty();
    }

    public Optional<KeyNode<T>> get(long key){
        if(children == null){
            return Optional.empty();
        }
        return Optional.ofNullable(children.get(key));
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof KeyNode<?> keyNode)) return false;
        return key == keyNode.key;
    }

    @Override
    public int hashCode(){
        return Objects.hash(key);
    }

    @Override
    public String toString(){
        return "KeyNode{" +
                "key=" + key +
                ", children=" + children +
                '}';
    }
}
