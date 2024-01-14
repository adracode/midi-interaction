package fr.adracode.piano;

import fr.adracode.piano.common.OneTimeStop;
import fr.adracode.piano.keyboard.KeyboardInterface;
import fr.adracode.piano.keyboard.os.WindowsKeyboard;
import fr.adracode.piano.mqtt.MqttPublisher;
import fr.adracode.piano.mqtt.MqttSubscriber;
import fr.adracode.piano.mqtt.MqttTopicPublisher;
import org.apache.commons.cli.*;
import org.eclipse.paho.client.mqttv3.MqttException;
import sun.misc.Signal;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class Main {
    
    public static void main(String[] args) throws ParseException, MqttException, AWTException, IOException{
        Main main = new Main();
        Options options = new Options();
        
        options.addOption(Option.builder()
                .option("i")
                .longOpt("interface")
                .hasArg()
                .desc("midi interface to use")
                .build());
        
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
                .option("m")
                .longOpt("mqtt")
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
                .hasArg(true)
                .type(String.class)
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
        DeviceHandler.Get getDevice = cmd.hasOption("interface") ?
                deviceHandler.getMidiDeviceInterfaces(cmd.getOptionValue("interface")) :
                deviceHandler.getMidiDeviceInterfaces();
        
        if(cmd.hasOption("file")){
            
            main.midiFilePlayer(getDevice,
                    cmd.getOptionValues("file"),
                    cmd.hasOption("replay"),
                    cmd.hasOption("start") ? ((Number)(cmd.getParsedOptionValue("start"))).longValue() : 0,
                    cmd.hasOption("tempo") ? ((Number)(cmd.getParsedOptionValue("tempo"))).floatValue() : 1.0F);
            
        } else if(cmd.hasOption("mqtt")){
            
            main.sendToMQTT(
                    cmd.getOptionValue("hostname"),
                    ((Number)cmd.getParsedOptionValue("port")).intValue(),
                    getDevice);
            
        } else if(cmd.hasOption("simulate-keyboard")){
            
            KeyboardInterface keyboardInterface = new KeyboardInterface(cmd.getOptionValue("simulate-keyboard"), new WindowsKeyboard());
            if(cmd.hasOption("host") && cmd.hasOption("port")){
                
                main.simulateKeyboardFromMQTT(
                        cmd.getOptionValue("hostname"),
                        ((Number)cmd.getParsedOptionValue("port")).intValue(),
                        keyboardInterface);
                
            } else {
                main.simulateKeyboard(getDevice, keyboardInterface);
            }
        }
    }
    
    private final Supplier<NoSuchElementException> noMidiFound =
            () -> new NoSuchElementException("No MIDI interface found");
    
    public void midiFilePlayer(DeviceHandler.Get getDevice, String[] files, boolean replay, long start, float tempo){
        MidiFilePlayer player = new MidiFilePlayer();
        Signal.handle(new Signal("INT"), signal -> player.stop());
        
        try(var out = getDevice.firstOut().orElseThrow(noMidiFound);
            var in = getDevice.firstIn().orElseThrow(noMidiFound)) {
            
            in.open();
            new MidiReader(in, (message, timestamp) -> {
                if(message instanceof ShortMessage sm){
                    if(sm.getCommand() == 176 && sm.getData1() == 64 && sm.getData2() == 0){
                        player.reRun();
                    }
                }
            }).run();
            
            out.open();
            player.readMidiFile(out,
                    files, replay, start, tempo);
            
            
        } catch(MidiUnavailableException e){
            throw new RuntimeException(e);
        }
    }
    
    public void simulateKeyboard(DeviceHandler.Get getDevice, KeyboardInterface keyboardInterface){
        try(var in = getDevice.firstIn().orElseThrow(noMidiFound);
            var threadLoop = new OneTimeStop().setWaitingTime(1000).setBlocking(true)) {
            
            in.open();
            
            new MidiReader(in, ((midiMessage, timeStamp) -> {
                if(midiMessage instanceof ShortMessage shortMessage){
                    if(shortMessage.getCommand() == 240){
                        return;
                    }
                    keyboardInterface.handle(shortMessage.getCommand(), shortMessage.getData1(), shortMessage.getData2());
                }
            })).run();
            
            Signal.handle(new Signal("INT"), signal -> {
                Thread.currentThread().interrupt();
            });
            
            threadLoop.block();
            
        } catch(MidiUnavailableException | IOException e){
            throw new RuntimeException(e);
        }
    }
    
    public void sendToMQTT(String hostname, int port, DeviceHandler.Get getDevice) throws MqttException{
        MqttTopicPublisher publisher = new MqttTopicPublisher(new MqttPublisher(
                hostname,
                port
        ), "piano/midi");
        
        try(var in = getDevice.firstIn().orElseThrow(noMidiFound)) {
            in.open();
            new MidiReader(in, ((midiMessage, timeStamp) -> {
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
            })).run();
        } catch(MidiUnavailableException e){
            throw new RuntimeException(e);
        }
        
        Signal.handle(new Signal("INT"), signal -> {
            try {
                publisher.close();
            } catch(IOException e){
                throw new RuntimeException(e);
            }
        });
    }
    
    public void simulateKeyboardFromMQTT(String hostname, int port, KeyboardInterface keyboardInterface) throws MqttException{
        MqttSubscriber subscriber = new MqttSubscriber(
                hostname,
                port,
                "piano/midi", keyboardInterface
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
