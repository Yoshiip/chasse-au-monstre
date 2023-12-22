package fr.univlille.multiplayer;

import java.io.IOException;
import java.io.BufferedReader;
import java.net.UnknownHostException;
import java.net.Socket;

public class Client extends MultiplayerBody {
	private static Client instance = null;
	private Socket socket;
	
	private Client() {}

	public static Client getInstance() {
		if (instance == null) {
			instance = new Client();
		}
		return instance;
	}

	/**
	 * Creates a socket between the server at the given address on the given port.
	 * @param serverAddress The address of the server (the name of the physical machine that's hosting the server).
	 * @param port          The port used by the server to communicate.
	 * @throws UnknownHostException If the given server address isn't found.
	 * @throws IOException          If for some reason the connection cannot be establish.
	 */
	public void connect(String serverAddress, int port) throws UnknownHostException, IOException {
		socket = new Socket(serverAddress, port);
		announcePresence();

		System.out.println("client successfully connected, and sent welcome to the server");

		new Thread(() -> {
			try {
				String serverMessage = "";
				BufferedReader in = MultiplayerUtils.getInputFromSocket(socket);
				while (isAlive() && (serverMessage = in.readLine()) != null) {
					MultiplayerCommunication incoming;
					try {
						incoming = new MultiplayerCommunication(serverMessage);
						System.out.println("Client is reading communication from server : " + incoming);
						incomingBuffer.add(incoming);
						if (onIncomingCommunicationCallback != null) {
							onIncomingCommunicationCallback.run();
						}
						System.out.println("client analysed communication from server : " + incoming.toString());
					} catch (InvalidCommunicationException e) {
						// An invalid communication is ignored.
						System.err.println("Server received invalid communication: " + serverMessage);
					}
				}
			} catch (IOException e) {
				// When `kill()` is executed,
				// an IOException ("socket closed") is thrown here.
				// We catch and we don't want to do anything with it.
			}
		}).start();
	}

	/**
	 * Terminates the client socket and informs the server about it.
	 * @throws IOException
	 */
	@Override
	public void kill(boolean propagate) throws IOException {
		if (!isAlive()) {
			System.out.println("trying to kill the client whereas it's not alive.");
			System.out.println("Buffer : " + incomingBuffer);
			return;
		}
		super.kill(propagate);
		if (propagate) {
			sendMessageToServer(
				new MultiplayerCommunication(
					MultiplayerCommand.DISCONNECTION,
					socket.getLocalAddress().toString()
				)
			);
		}
		socket.close();
		System.out.println("client was killed.");
	}

	/**
	 * Checks if the client was initialized and if it's successfully connected to the server.
	 * @return `true` if the client is connected, `false` otherwise.
	 */
	@Override
	public boolean isAlive() {
		return socket != null && !socket.isClosed();
	}

	/**
	 * Sends a message to the server.
	 * @param message The message to send to the server.
	 * @throws IOException
	 */
	public void sendMessageToServer(MultiplayerCommunication message) throws IOException {
		MultiplayerUtils.getOutputFromSocket(socket).println(message.toString());
	}

	/**
	 * Sends a message to the server announcing the successfull connection of the client.
	 * @throws IOException
	 */
	private void announcePresence() throws IOException {
		sendMessageToServer(
			new MultiplayerCommunication(
				MultiplayerCommand.JOIN,
				MultiplayerUtils.getHostname()
			)
		);
	}
}