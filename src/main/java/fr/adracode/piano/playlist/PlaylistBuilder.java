package fr.adracode.piano.playlist;

public class PlaylistBuilder {
    
    String[] filenames;
    boolean loop;
    
    public PlaylistBuilder filenames(String... filename){
        this.filenames = filename;
        return this;
    }
    
    public PlaylistBuilder loop(boolean loop){
        this.loop = loop;
        return this;
    }
    
    public Playlist build(){
        return new Playlist(this);
    }
    
}
