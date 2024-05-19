package es.um.redes.nanoFiles.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import es.um.redes.nanoFiles.directory.connector.DirectoryConnector;

public class NFControllerLogicDir {
	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Método para conectar con el directorio y obtener el número de peers que están
	 * sirviendo ficheros
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 */
	boolean logIntoDirectory(String directoryHostname) {
		/*
		 * TODO: Debe crear un objeto DirectoryConnector a partir del parámetro
		 * directoryHostname y guardarlo en el atributo correspondiente. A continuación,
		 * utilizarlo para comunicarse con el directorio y realizar tratar de realizar
		 * el "login", informar por pantalla del éxito/fracaso y devolver dicho valor
		 */
		boolean result = false;
		boolean connectionOk = true;
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
		} catch (IOException e) {
			System.err.println("Fallo en la conexión con el directorio.");
			connectionOk = false;
		}
		if (connectionOk) {
			int numServers = 0;
			try {
				numServers = directoryConnector.logIntoDirectory();
			} catch (IOException e) {
				System.err.println("* (faileld to connect to the direcototy) *");
				connectionOk = false;
			}
			if (connectionOk) {
			System.out.println("number os file servers"+Integer.toString(numServers));
				result = true;
			}
		}
		return result;
	}

	/**
	 * Método para registrar el nick del usuario en el directorio
	 * 
	 * @param nickname el nombre de usuario a registrar
	 * @return true si el nick es válido (no contiene ":") y se ha registrado
	 *         nickname correctamente con el directorio (no estaba duplicado), falso
	 *         en caso contrario.
	 */
	boolean registerNickInDirectory(String nickname) {
		/*
		 * TODO: Registrar un nick. Comunicarse con el directorio (a través del
		 * directoryConnector) para solicitar registrar un nick. Debe informar por
		 * pantalla si el registro fue exitoso o fallido, y devolver dicho valor
		 * booleano. Se debe comprobar antes que el nick no contiene el carácter ':'.
		 */
		boolean result;
		try {
			result = directoryConnector.registerNickname(nickname);
		} catch (IOException e) {
			result = false;
			System.err.println("* Error communicating with directory.");
		}
		
		return result;
	}

	/**
	 * Método para obtener de entre los peer servidores registrados en el directorio
	 * la IP:puerto del peer con el nick especificado
	 * 
	 * @param nickname el nick del peer por cuya IP:puerto se pregunta
	 * @return La dirección de socket del peer identificado por dich nick, o null si
	 *         no se encuentra ningún peer con ese nick.
	 */
	InetSocketAddress lookupUserInDirectory(String nickname) {
		/*
		 * TODO: Obtener IP:puerto asociada a nickname. Comunicarse con el directorio (a
		 * través del directoryConnector) para preguntar la dirección de socket en la
		 * que el peer con 'nickname' está sirviendo ficheros. Si no se obtiene una
		 * respuesta con IP:puerto válidos, se debe devolver null.
		 */
		
		InetSocketAddress peerAddr = null;
		try {
			peerAddr = directoryConnector.LookupUsername(nickname);
		} catch (IOException e) {
			peerAddr = null;
			System.err.println("* Error communicating with directory.");
		}
		return peerAddr;
	}

	/**
	 * Método para publicar la lista de ficheros que este peer está compartiendo.
	 * 
	 * @param port     El puerto en el que este peer escucha solicitudes de conexión
	 *                 de otros peers.
	 * @param nickname El nick de este peer, que será asociado a lista de ficheros y
	 *                 su IP:port
	 * @return true si la publicación fue exitosa, false en caso contrario.
	 */
	boolean publishLocalFilesToDirectory(int port, String nickname) {
		/*
		 * TODO: Enviar la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para enviar la lista de ficheros servidos por
		 * este peer con nick 'nickname' en el puerto 'port'. Los ficheros de la carpeta
		 * local compartida están disponibles en NanoFiles.db).
		 */
		try {
			return directoryConnector.publishLocalFiles (port, nickname, NanoFiles.db.getFiles());
		} catch (IOException e) {
			System.err.println ("* Error communicating with directory.");
			return false;
		}
	}

	/**
	 * Método para obtener y mostrar la lista de nicks registrados en el directorio
	 */
	void getUserListFromDirectory() {
		/*
		 * TODO: Obtener la lista de usuarios registrados. Comunicarse con el directorio
		 * (a través del directoryConnector) para obtener la lista de nicks registrados
		 * e imprimirla por pantalla.
		 */
		try {
			byte [] listaUsuarios = directoryConnector.getUserList();
			
			String lista = new String(listaUsuarios, StandardCharsets.UTF_8);
			
			char [] nombres = lista.toCharArray();
			
			char aux1 = ':';
			boolean servidores = false;
			
			for (int i = 0; i < nombres.length; i++) {
				
				if (servidores) {
					if ( nombres[i] == ':') {
						System.out.print("[*server*]");
						System.out.println ();
					}else {
						System.out.print(nombres[i]);
					}
				}else{
					if ( nombres[i] == ':') {
						if (aux1 == ':') {
							servidores = true;
						}else {
							System.out.println ();
						}
					}else {
						 System.out.print(nombres[i]);
					}
				}
			    aux1 = nombres[i];
			}
		} catch (IOException e) {
			System.err.println("* Error communicating with directory.");
		}
	}

	/**
	 * Método para obtener y mostrar la lista de ficheros que los peer servidores
	 * han publicado al directorio
	 */
	void getFileListFromDirectory() {
		/*
		 * TODO: Obtener la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para obtener la lista de ficheros e imprimirla
		 * por pantalla.
		 */

		 try {
			byte [] listaFicheros = directoryConnector.getFileList();

			String listaFich = new String(listaFicheros, StandardCharsets.UTF_8);

			String DELIMITADOR_CAMPOS = "::";
			String DELIMITADOR_FICHEROS = ":::";

			String [] arrayFicheros = listaFich.split(DELIMITADOR_FICHEROS + "");

			if (arrayFicheros[0].equals("")) System.out.println ("No hay ficheros en el directorio.");
			else {
					System.out.println ("Nombre\t\tTamaño\t\t\tHash\t\t\t\tServidores");
					for (int i = 0; i < arrayFicheros.length; i++){
						String [] arrayCampos = arrayFicheros[i].split(DELIMITADOR_CAMPOS + "");
						System.out.println (arrayCampos[0]+"\t"+arrayCampos[1]+"\t"+arrayCampos[2]+"\t"+arrayCampos[3]);
					}
				}
		} catch (IOException e) {
			System.err.println ("*Error communicating with directory*");
		}
	}

	/**
	 * Método para desconectarse del directorio (cerrar sesión) 
	 */
	public boolean logout(String nick) {
		/*
		 * TODO: Dar de baja el nickname. Al salir del programa, se debe dar de baja el
		 * nick registrado con el directorio y cerrar el socket usado por el
		 * directoryConnector.
		 */
		
		try {
			if (directoryConnector.logoutDirectory(nick)) {
				directoryConnector.reseatDirectoryConnector();
				return true;
			}else return false;
		} catch (IOException e) {
			
			System.err.println("* Error communicating with directory.");
			return false;
		}
		
	}

	public void fgstop (String nick){
		try {
			if (!directoryConnector.desconectarServidor(nick, NanoFiles.db.getFiles())){
				System.err.println("* Error disconnecting with directory.");
			}
		} catch (IOException e) {
			System.err.println("* Error communicating with directory.");
		}
	}

	}