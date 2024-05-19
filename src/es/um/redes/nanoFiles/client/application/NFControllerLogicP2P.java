package es.um.redes.nanoFiles.client.application;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import es.um.redes.nanoFiles.client.comm.NFConnector;
import es.um.redes.nanoFiles.server.NFServer;
import es.um.redes.nanoFiles.server.NFServerSimple;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	/**
	 * El servidor de ficheros en segundo plano
	 */
	private NFServer bgFileServer = null;
	/**
	 * El servidor de ficheros en primer plano
	 */
	NFServerSimple serverSimple;
	/**
	 * El cliente para conectarse a otros peers
	 */
	NFConnector nfConnector;
	/**
	 * El controlador que permite interactuar con el directorio
	 */
	private NFControllerLogicDir controllerDir;

	protected NFControllerLogicP2P() {
	}

	protected NFControllerLogicP2P(NFControllerLogicDir controller) {
		// Referencia al controlador que gestiona la comunicación con el directorio
		controllerDir = controller;
	}

	/**
	 * Método para ejecutar un servidor de ficheros en primer plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void foregroundServeFiles(int port, String nickname) {
		/*
		 * TODO: Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
		 * este método. Si se produce una excepción de entrada/salida (error del que no
		 * es posible recuperarse), se debe informar sin abortar el programa
		 */

		 // Si ya esta actuando como servidor en segundo plano, no se puede ejecutar
		 if (isActiveBgserver()){
			System.err.println ("You can't run a foreground server while a background server is running");
			return;
		 }

		// TODO: Publicar ficheros compartidos al directorio
		if (controllerDir.publishLocalFilesToDirectory(port, nickname)){
			System.out.println("Files published to directory.");
		} else {
			// Mensaje de error que no puede ser servidor
			System.err.println("Unable to be a server and publish files to directory because it is already a server or the port has been used.");
			return ;
		}

		// TODO: Crear objeto servidor NFServerSimple ligado al puerto especificado
		
		try {
			serverSimple = new NFServerSimple(port, nickname, controllerDir);
		} catch (IOException e) {
			System.err.println("Unable to start the server.");
			return;
		}

		// TODO: Ejecutar servidor en primer plano
		try {
			serverSimple.run();
		} catch (IOException e) {
			System.err.println("Problem while server is running.");
		return;
		}
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void backgroundServeFiles(int port, String nickname) {
		/*
		 * TODO: Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
		 * este método. Si se produce una excepción de entrada/salida (error del que no
		 * es posible recuperarse), se debe informar sin abortar el programa
		 */
		// TODO: Comprobar que no existe ya un objeto NFServer previamente creado, en
		// cuyo caso el servidor ya está en marcha
		
		// Evitamos que se cree un nuevo servidor en segundo plano si ya hay uno en ejecución.
		if (isActiveBgserver()) {
			System.err.println ("* Server running*");
			return 	;
		}
			
		// TODO: Publicar ficheros compartidos al directorio

		if (controllerDir.publishLocalFilesToDirectory(port, nickname)){

			System.out.println("Files published to directory.");
		} else {
			// Mensaje de error que no puede ser servidor
			System.err.println("Unable to be a server and publish files to directory.");
			return ;
		}

		// TODO: Crear objeto servidor NFServer ligado al puerto especificado
		try {
			bgFileServer = new NFServer(port);
	} catch (IOException e) {
			
			bgFileServer = null;
			System.err.println ("Unable to start the server .");
			return;
	}
		
		// TODO: Arrancar un hilo servidor en segundo plano

		bgFileServer.startServer();

		// TODO: Imprimir mensaje informando de que el servidor está en marcha
		System.out.println ("NFServer server running on " + bgFileServer.getServerSocket().getLocalSocketAddress());			
	}

	/**
	 * Método para establecer una conexión con un peer servidor de ficheros
	 * 
	 * @param nickname El nick del servidor al que conectarse (o su IP:puerto)
	 * @return true si se ha podido establecer la conexión
	 */
	protected boolean browserEnter(String nickname) {
		
				boolean connected = false;
				/*
				 * TODO: Averiguar si el nickname es en realidad una cadena con IP:puerto, en
				 * cuyo caso no es necesario comunicarse con el directorio.
				 */
				InetSocketAddress clientAddress = null;
				// Creamos esta variable para distintguir si se ha mandado el nick o IP:Puerto.
				Boolean Ip_Puerto = false;
				String ip = "";
				int port = 0;
				int posSep = nickname.indexOf(":");
				if (posSep != -1) {
					Ip_Puerto = true;
					ip = nickname.substring(0, posSep);
					port = Integer.parseInt(nickname.substring(posSep+1));
					
				}else {
					/*
					 * TODO: Si es un nickname, preguntar al directorio la IP:puerto asociada a
					 * dicho peer servidor.
					 */
					clientAddress = controllerDir.lookupUserInDirectory(nickname);
				}
				/*
				 * TODO: Comprobar si la respuesta del directorio contiene una IP y puerto
				 * válidos (el peer servidor al que nos queremos conectar ha comunicado
				 * previamente al directorio el puerto en el que escucha). En caso contrario,
				 * informar y devolver falso.
				 */
				// Si se ha mandado el nombre del usuario servidor y no se ha encontrado su IP:Puerto entonces error.
				if ( Ip_Puerto == false && clientAddress == null) {
					System.out.println ("Unable to comunicate with the server. Invalid IP:Port or nickname.");
					// Devolvemos que no se ha podido conectar con el servidor (false).
					return connected;
				}else {
					// Si se ha mandado el nick del servidor obtenemos su IP y Puerto.
					if (clientAddress != null) {
						port = clientAddress.getPort();
						ip = new String(clientAddress.getAddress().getAddress(), StandardCharsets.UTF_8);
					}
				}
				
				/*
				 * TODO: Crear un objeto NFConnector para establecer la conexión con el peer
				 * servidor de ficheros. Si la conexión se establece con éxito, informar y
				 * devolver verdadero.
				 */
				
				InetSocketAddress serverAddress = new InetSocketAddress(ip, port);
				try {
					nfConnector = new NFConnector(serverAddress);
				} catch (Exception e) {
					System.err.println ("Unable to comunicate with the server.");
					return connected;
				}
				connected = true;
				return connected;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros al que nos
	 * hemos conectador mediante browser Enter
	 * 
	 * @param targetFileHash El hash del fichero a descargar
	 * @param localFileName  El nombre con el que se guardará el fichero descargado
	 */
	protected void browserDownloadFile(String targetFileHash, String localFileName) {
		/*
		 * TODO: Usar el NFConnector creado por browserEnter para descargar el fichero
		 * mediante el método "download". Se debe omprobar si ya existe un fichero con
		 * el mismo nombre en esta máquina, en cuyo caso se informa y no se realiza la
		 * descarga
		 */

		 // Buscamos si esta el fichero que queremos descargar en nuestro directorio de fihceros.
		
		if (targetFileHash.length() < 3) {
			System.err.println ("*El hash debe de ser como mínimo 3 caracteres o más.*");
			return ;
		}
		
		 FileInfo [] fichero = FileInfo.lookupHashSubstring(NanoFiles.db.getFiles(), targetFileHash);

		 // Sino esta, entonces
		 if (fichero.length == 0) {
			try {

				File file = new File(NanoFiles.sharedDirname + "/" + localFileName);
				nfConnector.download(targetFileHash, file);
				
			} catch (IOException e) {
				System.err.println ("*Download Error*"+e.getMessage());
				return ;
			}

		// Si esta entonces no lo descargamos.
		 }else System.out.println ("The file is already in the folder.");

	}

	protected void browserClose() {
		/*
		 * TODO: Cerrar el explorador de ficheros remoto (informar al servidor de que se
		 * va a desconectar)
		 */

		 try {
			nfConnector.disconnectedSocket();
		} catch (IOException e) {
			System.err.println("*Unable to disconect socket*");
		}

	}

	protected void browserQueryFiles() {
		/*
		 * TODO: Crear un objeto NFConnector y guardarlo el atributo correspondiente
		 * para ser usado por otros métodos de esta clase mientras se está en una sesión
		 * del explorador de ficheros remoto.
		 * 
		 */
	}

	// Nos dice si esta activo el servidor en segundo plano.
	public boolean isActiveBgserver(){
		return bgFileServer != null;
	}

	public void stopBgServer() {
		// TODO Auto-generated method stub
		    bgFileServer.setParada(true);
			bgFileServer = null;
		
	}
	
}
