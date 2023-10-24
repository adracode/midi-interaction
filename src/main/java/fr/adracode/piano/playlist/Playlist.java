package fr.adracode.piano.playlist;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Playlist implements Iterator<File> {
    
    private final List<File> playlist = new ArrayList<>();
    private boolean loop;
    
    private int current = -1;
    
    
    Playlist(PlaylistBuilder builder){
        playlist.addAll(Arrays.stream(builder.filenames).map(File::new).collect(Collectors.toList()));
        this.loop = builder.loop;
    }
    
    @Override
    public boolean hasNext(){
        return loop || current + 1 < playlist.size();
    }
    
    @Override
    public File next(){
        if(!hasNext()){
            throw new NoSuchElementException("Playlist has ended and is not on loop");
        }
        ++current;
        if(loop){
            current %= playlist.size();
        }
        return playlist.get(current);
    }
    
}
