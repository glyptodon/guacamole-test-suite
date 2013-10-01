
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

import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.protocol.GuacamoleInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application which stresses implementations of the Guacamole protocol by
 * acting as a source of load. By default, the load behavior is passive, but
 * an active mode can be enabled with the --enable=hammer option.
 * 
 * @author Michael Jumper
 */
public class Stress {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(Stress.class);

    /**
     * Given a command-line argument of the form "--name=value", returns the
     * name, if any.
     * 
     * @param arg The argument to parse.
     * @return The name of the argument, if any, or null if no name exists.
     */
    private static String getArgName(String arg) {

        // Arg must start with --
        if (!arg.startsWith("--"))
            return null;

        // If no equals sign, return entire remaining string
        int eq = arg.indexOf('=');
        if (eq == -1)
            return arg.substring(2);

        // Otherwise grab string before equals
        return arg.substring(2, eq);

    }

    /**
     * Given a command-line argument of the form "--name=value", returns the
     * value, if any.
     * 
     * @param arg The argument to parse.
     * @return The value of the argument, if any, or null if no value exists.
     */
    private static String getArgValue(String arg) {

        // If no equals sign, then no value
        int eq = arg.indexOf('=');
        if (eq == -1)
            return null;

        return arg.substring(eq+1);

    }

    /**
     * Performs stress tests on a Guacamole protocol implementation based on
     * the arguments given.
     * 
     * Only a single "--protocol" argument is required, specifying the protocol
     * to request when connecting to guacd. By default, the program will connect
     * to localhost at port 4822 but this can be changed by specifying a
     * different hostname:port. Protocol-specific arguments are given as normal
     * command-line options.
     * 
     * Active stress testing can be enabled with "--enable=hammer".
     * 
     * For example:
     * 
     * java Stress --protocol=vnc --hostname=test --port=5901 localhost:4822
     * 
     * @param args Arguments describing how the test should be run.
     */
    public static void main(String[] args) {

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        String hostname = "localhost";
        int port = 4822;
        int time_limit = 0;

        boolean hammer = false;

        // Parse arguments
        for (String arg : args) {

            // If not an argument, parse as hostname:port
            String name = getArgName(arg);
            if (name == null) {

                // If no colon, assume default port
                int colon = arg.indexOf(':');
                if (colon == -1)
                    hostname = arg;

                // Otherwise, parse both
                else {
                    hostname = arg.substring(0, colon);
                    port = Integer.parseInt(arg.substring(colon+1));
                }

            }

            // If protocol, set protocol of config
            else if (name.equals("protocol"))
                config.setProtocol(getArgValue(arg));

            // Read time limit if given
            else if (name.equals("time-limit"))
                time_limit = Integer.parseInt(getArgValue(arg));

            // Special modes
            else if (name.equals("enable")) {
                String value = getArgValue(arg);
                if ("hammer".equals(value))
                    hammer = true;
            }

            // Otherwise, set value of parameter in config
            else
                config.setParameter(name, getArgValue(arg));

        }

        // Check that protocol was specified
        if (config.getProtocol() == null) {
            logger.error("Required option --protocol was not specified.");
            System.exit(1);
        }

        logger.info("Connecting to guacd at {}, port {}...", hostname, port);

        try {

            // Connect
            GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(hostname, port),
                config
            );

            logger.info("Connected.");
            long connection_start = System.currentTimeMillis();

            // Get I/O objects
            GuacamoleWriter writer = socket.getWriter();
            GuacamoleReader reader = socket.getReader();

            if (hammer) {
                logger.info("Hammer mode enabled.");
                Hammer h = new Hammer(writer);
                h.start();
            }
            
            // Frame statistics
            long frame_start = System.currentTimeMillis();
            int bytes = 0;

            // Read all instructions
            GuacamoleInstruction instruction;
            while ((instruction = reader.readInstruction()) != null) {

                // Get current time
                long current = System.currentTimeMillis();

                // Stop if past time limit
                if (time_limit != 0 && current - connection_start >= time_limit) {
                    logger.info("Time limit reached.");
                    System.exit(0);
                }
                
                // Respond to all sync instructions
                if (instruction.getOpcode().equals("sync")) {

                    // Note frame
                    if (bytes != 0)
                        logger.info("Frame duration={}ms, {} bytes",
                                current-frame_start, bytes);

                    // Respond to sync (unless something else will be writing)
                    if (!hammer)
                        writer.writeInstruction(instruction);

                    // Next frame
                    frame_start = current;
                    bytes = 0;

                }

                else if (instruction.getOpcode().equals("error")) {
                    logger.error("Error from guacd: {}", instruction.getArgs().get(0));
                    break;
                }

                // Otherwise, add total bytes
                else {
                    for (String arg : instruction.getArgs())
                        bytes += arg.length();
                }

            } // end for each instruction

            logger.error("End of input stream.");
            
        }
        catch (GuacamoleException e) {
            logger.error("Error reading instruction stream.", e);
        }

        logger.info("Disconnected.");
        System.exit(1);

    }

}

