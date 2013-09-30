package org.glyptodon.guacamole;

import java.util.Random;
import org.glyptodon.guacamole.io.GuacamoleWriter;

public class Hammer extends Thread {

    private Random rand = new Random();
    private GuacamoleWriter writer;
    
    public Hammer(GuacamoleWriter writer) {
        this.writer = writer;
    }
    
    private String getRandomElement() {

        // Use random length
        int length = rand.nextInt(1024);
        StringBuilder b = new StringBuilder();
        b.append(Integer.toString(length+1));
        b.append("._");

        // Generate random content
        for (int i=0; i<length; i++)
            b.appendCodePoint(rand.nextInt(0x7F - 0x20 + 1) + 0x20);

        // Add random terminator
        if (rand.nextBoolean())
            b.append(',');
        else
            b.append(';');
        
        return b.toString();
        
    }

    public void writeRandomly(String s) throws GuacamoleException {

        int original_length = s.length();
        while (s.length() > 0) {

            // Get random chunk
            int write_length = rand.nextInt(original_length) + 1;
            if (write_length > s.length())
                write_length = s.length();

            String chunk = s.substring(0, write_length);
            s = s.substring(write_length);

            // Write random chunk
            writer.write(chunk.toCharArray());
            
        }
        
    }
    
    @Override
    public void run() {
        
        try {
            for (;;) {
                String element = getRandomElement();
                writeRandomly(element);
                Thread.sleep(50);
            }
        }
        catch (InterruptedException e) {
            // Ignore
        }
        catch (GuacamoleException e) {
            // Ignore
        }
        
    }
    
}
