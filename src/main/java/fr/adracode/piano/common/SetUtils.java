package fr.adracode.piano.common;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {

    public static <T> Set<T> intersection(Set<T> a, Set<T> b){
        Set<T> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return intersection;
    }
}
