package fr.adracode.piano.keyboard.key;

import com.spencerwi.either.Either;

import java.util.Objects;
import java.util.Optional;

public final class KeyAction {
    private final Either<Integer, String> result;
    private final ToggledKey toggle;

    public KeyAction(String result, Integer keyCode, ToggledKey toggle){
        this.result = keyCode == null && result == null ? null : Either.either(() -> keyCode, () -> result);
        this.toggle = toggle;
    }

    public Optional<Either<Integer, String>> getResult(){
        return Optional.ofNullable(result);
    }

    public Optional<ToggledKey> getToggle(){
        return Optional.ofNullable(toggle);
    }

    @Override
    public boolean equals(Object obj){
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        var that = (KeyAction)obj;
        return Objects.equals(this.result, that.result) &&
                Objects.equals(this.toggle, that.toggle);
    }

    @Override
    public int hashCode(){
        return Objects.hash(result, toggle);
    }

    @Override
    public String toString(){
        return "KeyAction[" +
                "result=" + (result == null ? "null" : result.isLeft() ? result.getLeft() : result.isRight() ? result.getRight() : "") + ", " +
                "toggle=" + toggle + ']';
    }

    public static class Builder {

        private String result;
        private Integer keyCode;
        private String toggle;

        public Builder result(String result){
            this.result = result;
            return this;
        }

        public Builder keyCode(Integer keyCode){
            this.keyCode = keyCode;
            return this;
        }

        public Builder toggle(String toggle){
            this.toggle = toggle;
            return this;
        }

        public KeyAction build(){
            return new KeyAction(result, keyCode,
                    ToggledKey.get(toggle));
        }
    }


}
