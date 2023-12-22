package fr.univlille.multiplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server extends MultiplayerBody {
	private static Server instance = null;

	private ServerSocket server;
	private boolean acceptingNewUsers = true;
	private List<Socket> clientSockets = new ArrayList<>();

	private Server() { }

	public static Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	/**
	 * Creates a server socket and listens to client messages in non-blocking threads.
	 * It will accept any incoming request as long as `acceptingNewUsers` is `true`.
	 * By default, this function will set `acceptingNewUsers` to `true`.
	 * The clients will need the hostname (the name of the phsyical machine) in order to subscribe.
	 * @param port The port that the server will use for its communications.
	 */
	public void host(int port) {
		try {
			server = new ServerSocket(port);
			acceptingNewUsers = true; // just making sure it's `true` by default when creating the server (it wouldn't make sense to set it to `false` at this point)
			new Thread(() -> {
				try {
					while (isAlive()) {
						if (acceptingNewUsers) {
							Socket clientSocket = server.accept();
							System.out.println("accepted new user : " + clientSocket);
							welcomeIncomingClient(clientSocket);
							new Thread(new ClientHandler(clientSocket)).start();
						}
					}
				} catch (IOException e) {
					System.err.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
					System.err.println(e.getMessage());
				}
			}).start();
		} catch (SocketException e) {
			System.err.println("SOCKET EXCEPTION in host() of Server : this.server = " + this.server);
			System.err.println(e.getMessage());
			System.err.println(e.getCause());
		} catch (IOException e) {
			System.err.println("Exception caught when trying to initialize the server on port " + port);
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Stops new users from joining the server.
	 */
	public void closeRequests() {
		acceptingNewUsers = false;
	}

	/**
	 * Re-allows new users from joining the server.
	 */
	public void reacceptRequests() {
		acceptingNewUsers = true;
	}

	/**
	 * Checks if the server has clients.
	 * A server must be alive to have clients.
	 * @return `true` if the server has clients, `false` otherwise.
	 */
	public boolean hasClients() {
		return isAlive() && !clientSockets.isEmpty();
	}

	/**
	 * Stops the server.
	 * The server has to be restarted (by calling `host()`) if it needs to be used again.
	 * This methods sets `this.server` to `null`.
	 * 
	 * This method will inform all the listeners of the server termination.
	 * @throws IOException
	 */
	@Override
	public void kill(boolean propagate) throws IOException {
		if (!isAlive()) {
			return;
		}
		super.kill(propagate);
		if (propagate) {
			broadcast(
				new MultiplayerCommunication(
					MultiplayerCommand.SERVER_TERMINATION
				)
			);
		}
		for (Socket socket : clientSockets) socket.close();
		clientSockets.clear();
		server.close();
	}

	/**
	 * Checks if the server is running (the `ServerSocket` shouldn't be closed).
	 * @return `true` if the server is running, `false` otherwise.
	 */
	@Override
	public boolean isAlive() {
		return server != null && !server.isClosed();
	}

	/**
	 * Sends a message to a client of the server using its output stream.
	 * @param socket  The socket instance of the client.
	 * @param message The message to send to the client.
	 * @throws IOException If, for some reason, the output stream of the client socket led to an Exception.
	 */
	public void writeToClient(Socket clientSocket, MultiplayerCommunication message) throws IOException {
		MultiplayerUtils.getOutputFromSocket(clientSocket).println(message.toString());
	}

	/**
	 * Sends a welcome message to the client, confirming its successfull connection.
	 * The server sends its name along with the message.
	 * @param clientSocket The socket of the incoming client.
	 * @throws IOException
	 */
	private void welcomeIncomingClient(Socket clientSocket) throws IOException {
		writeToClient(clientSocket, new MultiplayerCommunication(MultiplayerCommand.HOST, MultiplayerUtils.getHostname()));
		clientSockets.add(clientSocket);
	}

	/**
	 * Sends a message to all users that are listening to this server.
	 * @param message The message to broadcast.
	 */
	public void broadcast(MultiplayerCommunication message) throws IOException {
		for (Socket client : clientSockets) {
			writeToClient(client, message);
		}
	}

	/**
	 * Removes a socket whose local address is equal to the given address.
	 * It's necessary to use something that can identity a unique socket,
	 * because when this method is executed, the client's socket hasn't been closed yet.
	 * @param localAddress The local address of the socket to remove.
	 * @return `true` it the socket was removed, `false` otherwise.
	 */
	public boolean removeClientSocket(String localAddress) {
		if (clientSockets.isEmpty()) {
			return false;
		}
		Iterator<Socket> iter = clientSockets.iterator();
		while (iter.hasNext()) {
			Socket clientSocket = iter.next();
			if (clientSocket.getLocalAddress().toString().equals(localAddress)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	private final class ClientHandler implements Runnable {
		private Socket clientSocket;

		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try (
				PrintWriter out = MultiplayerUtils.getOutputFromSocket(clientSocket);
				BufferedReader in = MultiplayerUtils.getInputFromSocket(clientSocket);
			) {
				String inputLine;
				while (isAlive() && (inputLine = in.readLine()) != null) {
					System.out.println("Server received: " + inputLine);
					try {
						MultiplayerCommunication incoming = new MultiplayerCommunication(inputLine);
						incomingBuffer.add(incoming);
						if (onIncomingCommunicationCallback != null) {
							onIncomingCommunicationCallback.run();
						}
						// The client socket was closed client-side,
						// therefore the socket must be removed from the list of subscribers
						if (incoming.isCommand(MultiplayerCommand.DISCONNECTION)) {
							removeClientSocket(incoming.getParameter(0));
						}
					} catch (InvalidCommunicationException e) {
						// Invalid communications are ignored.
						System.err.println("Server received invalid communication : " + inputLine);
					}
				}
			} catch (SocketException e) {
				System.err.println("SOCKET exception in Server ClientHandler (" + server + ")");
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.err.println("Error handling client input");
				e.printStackTrace();
			}
		}
	}
}