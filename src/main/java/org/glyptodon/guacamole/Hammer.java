
/**
 * Copyright (C) 2013 Glyptodon LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package org.glyptodon.guacamole;

import java.util.Random;
import org.glyptodon.guacamole.io.GuacamoleWriter;

/**
 * Thread which continuously sends random Guacamole instructions through a
 * GuacamoleWriter. Every element of these instructions is prefixed with an
 * underscore, thus preventing interpretation by guacd or libguac (no
 * Guacamole protocol instruction begins with an underscore).
 * 
 * @author Michael Jumper
 */
public class Hammer extends Thread {

    /**
     * The maximum value for any random Unicode codepoint in a random
     * Guacamole protocol element.
     */
    private static final int MAX_CODEPOINT = 0x7F;

    /**
     * The minimum value for any random Unicode codepoint in a random
     * Guacamole protocol element.
     */
    private static final int MIN_CODEPOINT = 0x20;
  
    /**
     * The maximum length of any random element.
     */
    private static final int MAX_LENGTH = 1024;
    
    /**
     * Random generator used for producing random element lengths and content.
     */
    private Random rand = new Random();

    /**
     * The GuacamoleWriter to use when writing protocol data.
     */
    private GuacamoleWriter writer;
   
    /**
     * Creates a new Hammer thread which writes random instructions to the
     * given GuacamoleWriter.
     * 
     * @param writer The GuacamoleWriter to write instructions to.
     */
    public Hammer(GuacamoleWriter writer) {
        this.writer = writer;
    }
    
    /**
     * Returns a random Guacamole protocol element. The terminator of this
     * element is random as well.
     * 
     * @return A String containing a single Guacamole protocol element,
     *         including terminator.
     */
    private String getRandomElement() {

        // Use random length
        int length = rand.nextInt(MAX_LENGTH);
        StringBuilder b = new StringBuilder();
        b.append(Integer.toString(length+1));
        b.append("._");

        // Generate random content
        for (int i=0; i<length; i++)
            b.appendCodePoint(rand.nextInt(MAX_CODEPOINT - MIN_CODEPOINT + 1) + MIN_CODEPOINT);

        // Add random terminator
        if (rand.nextBoolean())
            b.append(',');
        else
            b.append(';');
        
        return b.toString();
        
    }

    /**
     * Given arbitrary string data, writes that data to the GuacamoleWriter
     * in random-length chunks.
     * 
     * @param s The String to write randomly.
     * @throws GuacamoleException If an error occurs while writing the data.
     */
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
            }
        }
        catch (GuacamoleException e) {
            // Ignore
        }
        
    }
    
}
