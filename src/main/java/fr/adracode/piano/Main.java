package fr.adracode.piano;

import org.apache.commons.cli.*;
import sun.misc.Signal;

public class Main {

    public static void main(String[] args) throws ParseException{
        Options options = new Options();

        options.addOption(Option.builder()
                .option("f")
                .longOpt("file")
                .hasArgs()
                .desc("file(s) to play")
                .build());

        options.addOption(Option.builder()
                .option("r")
                .longOpt("replay")
                .hasArg(false)
                .desc("replay file when stopped")
                .build());

        options.addOption(Option.builder()
                .option("s")
                .longOpt("start")
                .hasArg(true)
                .desc("start at microsecond")
                .type(Number.class)
                .build());

        options.addOption(Option.builder()
                .option("t")
                .longOpt("tempo")
                .hasArg(true)
                .desc("change tempo (<1 slower, >1 faster")
                .type(Number.class)
                .build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves its purpose

        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e){
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        DeviceHandler deviceHandler = new DeviceHandler();

        MidiFilePlayer player = new MidiFilePlayer(deviceHandler);

        Signal.handle(new Signal("INT"), signal -> player.stop());

        if(cmd.hasOption("file")){
            new MidiHandler(deviceHandler).handle(player::reRun);
            player.readMidiFile(cmd.getOptionValues("file"),
                    cmd.hasOption("replay"),
                    cmd.hasOption("start") ? ((Number)(cmd.getParsedOptionValue("start"))).longValue() : 0,
                    cmd.hasOption("tempo") ? ((Number)(cmd.getParsedOptionValue("tempo"))).floatValue() : 1.0F);
        } else {
        }
    }

}
