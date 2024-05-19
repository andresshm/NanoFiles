package es.um.redes.nanoFiles.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import es.um.redes.nanoFiles.client.application.NFControllerLogicDir;

// PARA SERVIDORES EN PRIMER PLANO

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 100;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private ServerSocket serverSocket = null;
	private NFControllerLogicDir controllerDir;
	private String nick;

	public NFServerSimple (int port, String nombre, NFControllerLogicDir contro) throws IOException {
		/*
		 * TODO: Crear una direción de socket a partir del puerto especificado
		 */
		InetSocketAddress FileServerSocketAddress = new InetSocketAddress(port);
		/*
		 * TODO: Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(FileServerSocketAddress);
		serverSocket.setReuseAddress(true);
		serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS); // HAcemos que espera solo un timeout
		controllerDir = contro;
		nick = nombre;
		
		/*
		 * TODO: (Opcional) Establecer un timeout para que el método accept no espere
		 * indefinidamente
		 */
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * @throws IOException
	 * 
	 */
	public void run() throws IOException {
		/*
		 * TODO: Comprobar que el socket servidor está creado y ligado
		 */
		BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
		boolean stopServer = false;
		System.out.println("Enter '" + STOP_SERVER_COMMAND + "' to stop the server");
		Socket socket = null;
		while (!stopServer) {
			/*
			 * TODO: Usar el socket servidor para esperar conexiones de otros peers que
			 * soliciten descargar ficheros
			 */

			try {
				socket = serverSocket.accept();
				
				if(socket != null) {
					NFServerComm.serveFilesToClient(socket);
				}
			} catch (java.net.SocketTimeoutException e) {
				if (standardInput.ready()) {
					String command = standardInput.readLine();
					if (STOP_SERVER_COMMAND.equals(command)) {
						stopServer = true;
						stopServer();
					}
			}
			} catch (IOException e) {
    			System.err.println("Problem with the server: " + e.getMessage());
    			stopServer();
				break;
			}
			/*
			 * TODO: Al establecerse la conexión con un peer, la comunicación con dicho
			 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
			 * hay que pasarle el objeto Socket devuelto por accept (retorna un nuevo socket
			 * para hablar directamente con el nuevo cliente conectado)
			 */
			
			/*
			 * TODO: (Para poder detener el servidor y volver a aceptar comandos).
			 * Establecer un temporizador en el ServerSocket antes de ligarlo, para
			 * comprobar mediante standardInput.ready()) periódicamente si se ha tecleado el
			 * comando "fgstop", en cuyo caso se cierra el socket servidor y se sale del
			 * bucle
			 */
		}
		System.out.println("NFServerSimple stopped");

	}
	public void stopServer() {

		try {
			serverSocket.close();
			controllerDir.fgstop(nick);
		} catch (IOException e) {
			System.err.println ("*Error stopping server*");
		}
	}
}
