package es.um.redes.nanoFiles.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Servidor que se ejecuta en un hilo propio. Creará objetos
 * {@link NFServerThread} cada vez que se conecte un cliente.
 */

// PARA SERVIDORES EN SEGUNDO PLANO.

public class NFServer implements Runnable {

	private ServerSocket serverSocket = null;
	
	private Boolean parada = false;
	
	private ArrayList<Socket> sockets = new ArrayList<>();
	
	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 100;

	public ServerSocket getServerSocket() {
		return serverSocket;
	}
	
	public NFServer(int port) throws IOException {
		/*
		 * TODO: Crear una direción de socket a partir del puerto especificado
		 */
		InetSocketAddress serverSocketAddress = new InetSocketAddress(port);
		
		/*
		 * TODO: Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */
		
		serverSocket = new ServerSocket();
		serverSocket.bind(serverSocketAddress);
		serverSocket.setReuseAddress(true);
		serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS);
	}
	
	public void setParada(Boolean parada){
		this.parada = parada;
	}
	
	public Boolean getParada(){
		return parada;
	}
	/**
	 * Método que ejecuta el hilo principal del servidor (creado por startServer).
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Socket socket = null;
		
			while (true) {
				/*
				 * TODO: Usar el socket servidor para esperar conexiones de otros peers que
				 * soliciten descargar ficheros
				 */
				
				try {
					socket = serverSocket.accept();
					System.out.println ("\nNew client connection from "+socket.getInetAddress().toString() +":"+ socket.getPort());
					
					if (socket != null) {
						sockets.add(socket);
						NFServerThread connectionThread = new NFServerThread(socket);
						connectionThread.start();
					}
					
				}catch (java.net.SocketTimeoutException e) {
						if (parada) {
							    stopServer();
								break;
						}
				}catch (SocketException e) {
					stopServer();
					System.err.println ("There was a problem with the local file server on "+serverSocket.getLocalSocketAddress());
					break;
				} catch (IOException e) {
					stopServer();
					System.err.println ("There was a problem with the local file server on "+serverSocket.getLocalSocketAddress());
					break;
				}
				
				/*
				 * TODO: Al establecerse la conexión con un peer, la comunicación con dicho
				 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
				 * hay que pasarle el objeto Socket devuelto por accept (retorna un nuevo socket
				 * para hablar directamente con el nuevo cliente conectado)
				 */
				
			}
	}

	/**
	 * Método que crea un hilo de esta clase y lo ejecuta en segundo plano,
	 * empezando por el método "run".
	 */
	public void startServer() {
		new Thread(this).start();
	}

	/**
	 * Método que detiene el servidor, cierra el socket servidor y termina los hilos
	 * que haya ejecutándose
	 */
	public void stopServer() {
		try {
			for (Socket socket : sockets) {
				System.out.println ("Cerramos: "+socket.toString());
				socket.close();
			}
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("There was a problem closing the server socket");
		}
	}
}
