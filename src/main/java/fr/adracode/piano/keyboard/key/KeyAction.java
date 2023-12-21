package fr.adracode.piano.keyboard.key;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;
import java.util.Optional;

public final class KeyAction {
    private final String unicode;
    private final IntList keys;
    private final ToggleKey toggle;

    public KeyAction(String unicode, IntList keys, ToggleKey toggle){
        this.keys = keys;
        this.unicode = unicode;
        this.toggle = toggle;
    }

    public Optional<String> getUnicode(){
        return Optional.ofNullable(unicode);
    }

    public Optional<IntList> getKeys(){
        return Optional.ofNullable(keys);
    }

    public Optional<ToggleKey> getToggle(){
        return Optional.ofNullable(toggle);
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(!(o instanceof KeyAction keyAction))
            return false;
        return Objects.equals(unicode, keyAction.unicode) && Objects.equals(keys, keyAction.keys) && Objects.equals(toggle, keyAction.toggle);
    }

    @Override
    public int hashCode(){
        return Objects.hash(unicode, keys, toggle);
    }

    public static class Builder {

        private String result;
        private IntList keys;
        private String toggle;

        public Builder result(String result){
            this.result = result;
            return this;
        }

        public Builder keyCode(IntList keyCode){
            this.keys = keyCode;
            return this;
        }

        public Builder toggle(String toggle){
            this.toggle = toggle;
            return this;
        }

        public KeyAction build(){
            return new KeyAction(result, keys, ToggleKey.get(toggle));
        }
    }


}
