package fr.adracode.piano.keyboard;

import fr.adracode.piano.common.SetUtils;
import fr.adracode.piano.keyboard.key.Key;
import fr.adracode.piano.keyboard.key.KeyAction;
import fr.adracode.piano.keyboard.key.ToggleKey;
import fr.adracode.piano.keyboard.key.ToggleKeyState;
import fr.adracode.piano.keyboard.os.OSKeyboard;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.adracode.piano.keyboard.KeyboardHand.OCTAVE_WITH_PEDALS;
import static fr.adracode.piano.keyboard.key.ToggleKey.NO_TOGGLE;

public class ToggleManager extends Mapping<ToggleKey> implements HandleKey {

    private final List<Set<ToggleKey>> pressedTogglesByHand = new ArrayList<>(OCTAVE_WITH_PEDALS);
    private final boolean[] once = new boolean[OCTAVE_WITH_PEDALS];
    private final int sustain;

    private final Set<ToggleKey> activeToggles = new HashSet<>();
    private final Map<ToggleKey, ToggleKeyState> togglesState = new HashMap<>();

    public ToggleManager(OSKeyboard keyboard, int sustain, Long2ObjectMap<ToggleKey> mapping, int[] masks){
        super(keyboard, mapping, new KeyboardHand(0), masks);
        this.sustain = sustain;
        IntStream.range(0, OCTAVE_WITH_PEDALS).forEach($ -> pressedTogglesByHand.add(new HashSet<>()));
    }

    public boolean performAction(Key key, Consumer<KeyAction> consumer){
        Optional<KeyAction> keyAction = key.getAction(activeToggles).or(() -> fallback(key));
        boolean result = keyAction.map(action -> {
            consumer.accept(action);
            return true;
        }).orElse(false);

        togglesState.forEach((toggleKey, state) -> {
            //Trigger during held down -> has accomplished his duty, can't be once anymore
            if(state.isOnceCandidate()){
                state.setBeenUsed(true);
            }
            if(state.isOnce()){
                if(activeToggles.contains(toggleKey)){
                    disable(toggleKey);
                } else {
                    enable(toggleKey);
                }
                state.setOnce(false);
            }
        });
        return result;
    }

    public void setOnce(int octave){
        once[octave] = true;
    }

    private Optional<KeyAction> fallback(Key key){
        ToggleKey.Fallback fallback = ToggleKey.getFallback(activeToggles).orElse(null);
        if(fallback == null){
            return Optional.empty();
        }
        return fallback == ToggleKey.Fallback.NORMAL ?
                key.getAction(NO_TOGGLE) : Optional.empty();
    }

    @Override
    public void handle(int octave){
        Set<ToggleKey> currentToggles = getCurrentToggles(octave);
        var stillActive = SetUtils.intersection(currentToggles, pressedTogglesByHand.get(octave));

        //Disable toggles that aren't active anymore
        pressedTogglesByHand.get(octave).stream()
                .filter(k -> !stillActive.contains(k))
                //Avoid ConcurrentModificationException
                .toList().forEach(this::release);

        //Enable toggles that occur to be now active or disable permanents
        currentToggles.stream()
                .filter(k -> !stillActive.contains(k))
                .forEach(this::press);

        once[octave] = false;
    }

    private Set<ToggleKey> getCurrentToggles(int octave){
        return hand.getHand(octave)
                .map(hand -> Optional.ofNullable(mapping.get(Key.from(octave, hand & masks[octave])))
                        .map(Set::of)
                        .orElseGet(() -> Key.weightedBinaryStream(hand & masks[octave])
                                .filter(b -> b > 0)
                                .mapToObj(key -> mapping.get(Key.from(octave, key)))
                                .collect(Collectors.toSet())))
                .orElse(Set.of());
    }

    private void press(ToggleKey key){
        var activeToggle = pressedTogglesByHand.get(key.getOctave());
        activeToggle.add(key);

        ToggleKeyState state = getState(key);
        state.setOnceCandidate(false);
        state.setPressedOn(System.currentTimeMillis());
        if(once[key.getOctave()]){
            state.setOnceCandidate(true);
        }
        if(state.isPermanent()){
            disable(key);
        } else {
            enable(key);
        }
        state.setBeenUsed(false);
    }

    private void release(ToggleKey key){
        var activeToggle = pressedTogglesByHand.get(key.getOctave());
        activeToggle.remove(key);

        ToggleKeyState state = getState(key);
        //Too little time between enable and disable = PERMANENT
        if(!state.hasBeenUsed() && state.getPressedOn() > System.currentTimeMillis() - sustain){
            state.setOnce(state.isOnceCandidate());
            if(state.isOnce()){
                state.setOnceCandidate(false);
            } else {
                state.setPermanent(!state.isPermanent());
            }
        }
        if(!state.isOnce()){
            if(state.isPermanent()){
                enable(key);
            } else {
                disable(key);
            }
        }
        state.setBeenUsed(false);
    }

    private void enable(ToggleKey key){
        activeToggles.add(key);
        pressKeys(key.getAction(NO_TOGGLE.getId()).orElse(null));
    }

    private void disable(ToggleKey key){
        activeToggles.remove(key);
        releaseKeys(key.getAction(NO_TOGGLE.getId()).orElse(null));
    }

    private ToggleKeyState getState(ToggleKey key){
        return Optional.ofNullable(togglesState.get(key))
                .orElseGet(() -> {
                    ToggleKeyState newState = new ToggleKeyState();
                    togglesState.put(key, newState);
                    return newState;
                });
    }
}
