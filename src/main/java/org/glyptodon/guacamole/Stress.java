
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

public class Stress {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(Stress.class);

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

    private static String getArgValue(String arg) {

        // If no equals sign, then no value
        int eq = arg.indexOf('=');
        if (eq == -1)
            return null;

        return arg.substring(eq+1);

    }

    public static void main(String[] args) throws Exception {

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        String hostname = "localhost";
        int port = 4822;

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

        // Connect
        GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
            new InetGuacamoleSocket(hostname, port),
            config
        );

        logger.info("Connected.");

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

        try {

            // Read all instructions
            GuacamoleInstruction instruction;
            while ((instruction = reader.readInstruction()) != null) {

                // Get current time
                long current = System.currentTimeMillis();

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

        }
        catch (GuacamoleException e) {
            logger.error("Error reading instruction stream.", e);
        }

        logger.info("Disconnected.");

    }

}

