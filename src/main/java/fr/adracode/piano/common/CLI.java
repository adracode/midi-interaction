package fr.adracode.piano.common;

import java.io.IOException;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Predicate;

public class CLI implements AutoCloseable {
    
    private Scanner scanner = new Scanner(System.in);
  
    public int printMenu(){
        System.out.println("Choisissez une action: ");
        System.out.println("1. Lire un fichier");
        System.out.println("0. Quitter");
        return getUserChoice(1);
    }
    
    public String getFile(){
        System.out.println("Entrez un fichier: ");
        return getUserInput();
    }
    
    @Override
    public void close() throws IOException{
        scanner.close();
    }
    
    //Récupère une chaîne de caractère depuis l'entrée utilisateur
    public String getUserInput(){
        return getUserInput(CLI::parseString);
    }
    
    //Récupère un type depuis l'entrée utilisateur
    public <T> T getUserInput(Convert<T> userInput){
        return getUserInput(userInput, i -> true);
    }
    
    //Récupère un type depuis l'entrée utilisateur qui respecte une contrainte
    public <T> T getUserInput(Convert<T> userInput, Predicate<T> isInputValid){
        boolean stop = false;
        T result = null;
        while(!stop){
            try {
                System.out.print("> ");
                result = userInput.convert(scanner.nextLine().trim());
                if(!isInputValid.test(result)){
                    System.out.println("Choix incorrect, veuillez réessayez.");
                } else {
                    stop = true;
                }
            } catch(IllegalArgumentException | ParseException e){
                System.out.println("Choix incorrect, veuillez réessayez.");
            } catch(NoSuchElementException | IllegalStateException e){
                //Ctrl-D
                stop = true;
                System.out.println();
            }
        }
        return result;
    }
    
    //Récupère le choix d'un menu depuis l'entrée utilisateur
    public int getUserChoice(int maxInput){
        Integer choice = getUserInput(Integer::parseInt, input -> input >= 0 && input <= maxInput);
        return choice == null ? 0 : choice;
    }
    
    //Méthode utilisé lorsque l'on a besoin d'une contrainte mais que l'on veut une chaîne de caractère
    public static String parseString(String raw){
        return raw;
    }
    
    //Interface pour convertir une chaîne de caractère en un autre type
    public interface Convert<T> {
        T convert(String raw) throws ParseException;
    }
    
}