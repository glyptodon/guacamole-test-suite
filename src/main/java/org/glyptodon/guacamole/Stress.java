
package org.glyptodon.guacamole;



import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.protocol.GuacamoleInstruction;

public class Stress {

    public static void main(String[] args) throws Exception {

        // STUB: Parse from args

        String hostname = "localhost";
        int port = 4822;

        // Build configuration
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol("vnc");
        config.setParameter("hostname", "proto-test");
        config.setParameter("port", "5901");
       
        System.err.println("Connecting...");
        
        // Connect
        GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
            new InetGuacamoleSocket(hostname, port),
            config
        );

        System.err.println("Connected.");
        
        // Get I/O objects
        GuacamoleWriter writer = socket.getWriter();
        GuacamoleReader reader = socket.getReader();
        
        try {

            // Read all instructions
            GuacamoleInstruction instruction;
            while ((instruction = reader.readInstruction()) != null) {

                // Respond to all sync instructions
                if (instruction.getOpcode().equals("sync"))
                    writer.writeInstruction(instruction);

                System.err.println(instruction.getOpcode());
                
            }

        }
        catch (GuacamoleException e) {
            System.err.println("Error reading instruction stream");
            e.printStackTrace(System.err);
        }

        System.err.println("Disconnected.");

    }

}

