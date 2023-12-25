package fr.adracode.piano.keyboard.key;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;
import java.util.Optional;

public final class KeyAction {
    private final String unicode;
    private final IntList keys;

    public KeyAction(String unicode, IntList keys){
        this.keys = keys;
        this.unicode = unicode;
    }

    public Optional<String> getUnicode(){
        return Optional.ofNullable(unicode);
    }

    public Optional<IntList> getKeys(){
        return Optional.ofNullable(keys);
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(!(o instanceof KeyAction action))
            return false;
        return Objects.equals(unicode, action.unicode) && Objects.equals(keys, action.keys);
    }

    @Override
    public int hashCode(){
        return Objects.hash(unicode, keys);
    }

    public static class Builder {

        private String result = "";
        private IntList keys = new IntArrayList();

        public Builder(KeyAction action){
            if(action != null){
                this.result = action.unicode;
                this.keys = action.keys;
            }
        }

        public Builder result(String result){
            this.result = this.result + result;
            return this;
        }

        public Builder keyCode(int keyCode){
            this.keys.add(keyCode);
            return this;
        }

        public KeyAction build(){
            return new KeyAction(
                    result.isEmpty() ? null : result,
                    keys.isEmpty() ? null : keys);
        }
    }


}
