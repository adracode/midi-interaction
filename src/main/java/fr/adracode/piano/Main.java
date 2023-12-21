package fr.adracode.piano;

import fr.adracode.piano.keyboard.KeyboardSimulator;
import fr.adracode.piano.mqtt.MqttPublisher;
import fr.adracode.piano.mqtt.MqttSubscriber;
import fr.adracode.piano.mqtt.MqttTopicPublisher;
import org.apache.commons.cli.*;
import org.eclipse.paho.client.mqttv3.MqttException;
import sun.misc.Signal;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws ParseException, MqttException, AWTException, IOException{
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

        options.addOption(Option.builder()
                .option("k")
                .longOpt("keyboard")
                .desc("send keys through mqtt")
                .build());

        options.addOption(Option.builder()
                .option("h")
                .longOpt("hostname")
                .hasArg()
                .desc("hostname of mqtt broker").build());

        options.addOption(Option.builder()
                .option("p")
                .longOpt("port")
                .hasArg()
                .type(Number.class)
                .desc("port of mqtt broker").build());

        options.addOption(Option.builder()
                .longOpt("simulate-keyboard")
                .desc("simulate keyboard press")
                .build());

        options.addOption(Option.builder()
                .longOpt("mapping")
                .desc("mapping for keyboard simulation")
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


        if(cmd.hasOption("file")){
            Signal.handle(new Signal("INT"), signal -> player.stop());
            new MidiHandler(deviceHandler).handle(player::reRun);
            player.readMidiFile(cmd.getOptionValues("file"),
                    cmd.hasOption("replay"),
                    cmd.hasOption("start") ? ((Number)(cmd.getParsedOptionValue("start"))).longValue() : 0,
                    cmd.hasOption("tempo") ? ((Number)(cmd.getParsedOptionValue("tempo"))).floatValue() : 1.0F);
        } else if(cmd.hasOption("keyboard")){
            MqttTopicPublisher publisher = new MqttTopicPublisher(new MqttPublisher(
                    cmd.getOptionValue("hostname"),
                    ((Number)cmd.getParsedOptionValue("port")).intValue()
            ), "piano/keyboard");
            MidiReader reader = new MidiReader(deviceHandler, ((midiMessage, timeStamp) -> {
                try {
                    if(midiMessage instanceof ShortMessage shortMessage){
                        if(shortMessage.getCommand() == 240){
                            return;
                        }
                        publisher.publish(shortMessage.getCommand() + ", " + shortMessage.getData1() + ", " + shortMessage.getData2());
                    }
                } catch(MqttException e){
                    e.printStackTrace();
                }
            }));
            Signal.handle(new Signal("INT"), signal -> {
                try {
                    reader.close();
                    publisher.close();
                } catch(IOException e){
                    throw new RuntimeException(e);
                }
            });
            reader.run();
        } else if(cmd.hasOption("simulate-keyboard")){
            MqttSubscriber subscriber = new MqttSubscriber(
                    cmd.getOptionValue("hostname"),
                    ((Number)cmd.getParsedOptionValue("port")).intValue(),
                    "piano/keyboard",
                    new KeyboardSimulator("mapping.yml")
            );
            Signal.handle(new Signal("INT"), signal -> {
                try {
                    subscriber.close();
                } catch(IOException e){
                    throw new RuntimeException(e);
                }
            });
        }
    }

}
