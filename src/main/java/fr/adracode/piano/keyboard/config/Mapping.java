package fr.adracode.piano.keyboard.config;

import fr.adracode.piano.keyboard.key.KeyNode;

import java.util.Optional;

public class Mapping<T> {

    private final KeyNode<T> root;
    private KeyNode<T> current;

    public Mapping(KeyNode<T> root){
        this.root = root;
    }

    public Optional<KeyNode<T>> getNext(long id){
        var next = (current == null ? root : current).get(id);
        next.ifPresent(n -> current = n);
        return next;
    }

    public void resetPath(){
        current = null;
    }


}
