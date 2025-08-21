package com.corundumstudio.socketio.protocol;

import java.util.concurrent.atomic.AtomicReference;

import io.socket.parser.IOParser;
import io.socket.parser.Packet;

public class NativeSocketIOClientUtil {
  private static final IOParser.Encoder ENCODER = new IOParser.Encoder();

  /**
   * Converts a Socket.IO packet to a native message format.
   *
   * @param packet
   * @return
   */
  public static String getNativeMessage(Packet packet) {
    AtomicReference<String> result = new AtomicReference<>();
    ENCODER.encode(
        packet,
        encodedPackets -> {
          for (Object pack : encodedPackets) {
            io.socket.engineio.parser.Packet<String> enginePacket =
                new io.socket.engineio.parser.Packet<String>(
                    io.socket.engineio.parser.Packet.MESSAGE);
            if (pack instanceof String) {
              enginePacket.data = (String) pack;
              io.socket.engineio.parser.Parser.encodePacket(
                  enginePacket,
                  data -> {
                    result.set(data.toString());
                  });
            }
          }
        });
    return result.get();
  }

  /**
   * Gets the pure Socket.IO protocol encoding without Engine.IO wrapper. This method returns the
   * raw Socket.IO packet format as specified in the protocol documentation.
   *
   * @param packet
   * @return
   */
  public static String getSocketIOProtocolEncoding(Packet packet) {
    AtomicReference<String> result = new AtomicReference<>();
    ENCODER.encode(
        packet,
        encodedPackets -> {
          for (Object pack : encodedPackets) {
            if (pack instanceof String) {
              result.set((String) pack);
              break; // Take the first encoded packet (Socket.IO format)
            }
          }
        });
    return result.get();
  }
}
