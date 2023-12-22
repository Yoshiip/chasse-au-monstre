package fr.univlille.multiplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MultiplayerUtils {
  private MultiplayerUtils() {}

  /**
   * Gets the output stream of a socket as an instance of `PrintWriter`.
   * The print writer is automatically flushed.
   * @param socket The socket to get the output stream from.
   * @return The output stream of the given socket as an instance of `PrintWriter`.
   * @throws IOException
   */
  public static PrintWriter getOutputFromSocket(Socket socket) throws IOException {
    return new PrintWriter(socket.getOutputStream(), true); // "true" to automatically send the information once something is written
  }

  /**
   * Gets the input stream of a socket as an instance of `BufferedReader`.
   * @param socket The socket to get the input stream from.
   * @return The input stream of the given socket as an instance of `BufferedReader`
   * @throws IOException
   */
  public static BufferedReader getInputFromSocket(Socket socket) throws IOException {
    return new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  /**
	 * Gets the name of the host that's running the server.
	 * It's the name of the physical machine running this code.
	 * @return The name of the physical machine, or "???" if it's unknown.
	 */
	public static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "???";
		}
	}
}
