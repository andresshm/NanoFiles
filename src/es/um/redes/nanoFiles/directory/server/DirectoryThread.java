package es.um.redes.nanoFiles.directory.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import es.um.redes.nanoFiles.client.application.NFControllerLogicDir;
import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.directory.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class DirectoryThread extends Thread {

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	protected DatagramSocket socket = null;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	protected double messageDiscardProbability;

	/**
	 * Estructura para guardar los nicks de usuarios registrados, y la fecha/hora de
	 * registro
	 * 
	 */
	private HashMap<String, LocalDateTime> nicks;
	
	/**
	 * Estructura para guardar los usuarios servidores (nick, direcciones de socket
	 * TCP)
	 */
	// TCP)
	private HashMap<String, InetSocketAddress> servers;
	
	/**
	 * Estructura para guardar la lista de ficheros publicados por todos los peers
	 * servidores, cada fichero identificado por su hash
	 */
	private HashMap<String, FileInfo> files;
	
	/**
	 * Estructura para guardar la lista de ficheros publicados por su respectivo servidor
	 * (hash, nick).
	*/
	private HashMap<String, String> serve_files;
	

	public DirectoryThread(int directoryPort, double corruptionProbability) throws SocketException {
		/*
		 * TODO: Crear dirección de socket con el puerto en el que escucha el directorio
		 */
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		// TODO: Crear el socket UDP asociado a la dirección de socket anterior
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;
		nicks = new HashMap<String, LocalDateTime>();
		servers = new HashMap <String, InetSocketAddress>();
		files = new HashMap <String, FileInfo>();
		serve_files = new HashMap <String, String>();
	}

	@Override
	public void run() {
		InetSocketAddress clientId = null;

		System.out.println("Directory starting...");

		while (true) {
			try {

				// TODO: Recibimos a través del socket el datagrama con mensaje de solicitud
				byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
				// Preparamos el paquete para recibir información.
				DatagramPacket requestPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
				// Nos quedamos a la escucha.
				socket.receive(requestPacket);
			
				// TODO: Averiguamos quién es el cliente
				clientId = (InetSocketAddress) requestPacket.getSocketAddress();

				// Vemos si el mensaje debe ser descartado por la probabilidad de descarte

				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED datagram from " + clientId);
					continue;
				}

				// Analizamos la solicitud y la procesamos
				// Si tiene información la decodificamos
				if (requestPacket.getData().length > 0) {
					// Llama a la función de abajo.
					processRequestFromClient(requestPacket.getData(), clientId);
				} else {
					System.err.println("Directory received EMPTY datagram from " + clientId);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Directory received EMPTY datagram from " + clientId);				
				break;
			}
		}
		// Cerrar el socket
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// TODO: Construir un objeto mensaje (DirMessage) a partir de los datos recibidos
		// PeerMessage.buildMessageFromReceivedData(data)
		
		// Obtenemos un mensaje en un buffer de byte con los bytes con un codigo ya sea del login y otro.
		DirMessage receivedMessage = DirMessage.buildMessageFromReceivedData(data);
				
		// TODO: Actualizar estado del directorio y enviar una respuesta en función del
		// tipo de mensaje recibido
		
		byte receivedOpcode = receivedMessage.getOpcode();
		switch (receivedOpcode) {
		case DirMessageOps.OPCODE_LOGIN: 
			// Llamamos a la función de abajo.
			sendLoginOK(clientAddr);
			break;
		
		case DirMessageOps.OPCODE_REGISTER_USERNAME: 
			String nickToRegister = new String (receivedMessage.getUserName());
			if (nicks.containsKey(nickToRegister)) {
				sendRegisterFAIL(clientAddr);
			}
			else {
			
				//System.out.print(nickToRegister);
				nicks.put(nickToRegister, LocalDateTime.now());
				sendRegisterOK(clientAddr);
			}
			break;
		case DirMessageOps.OPCODE_GETUSERS:
				sendUserlist(clientAddr);
				break;
		case DirMessageOps.OPCODE_GETFILES:
				sendFileList(clientAddr);
				break;
		case DirMessageOps.OPCODE_LOOKUP_USERNAME:
				String nickToLookUp = new String (receivedMessage.getUserName());
				
				if (servers.containsKey(nickToLookUp)) {
					UsernameFound (clientAddr, nickToLookUp);
				}
				else {
					UsernameNotFound(clientAddr);
				}
				break;
		case DirMessageOps.OPCODE_SERVE_FILES:
				
				// Obtenemos el puerto
				int puerto = receivedMessage.getPuerto();
				InetSocketAddress socket_server = new InetSocketAddress(puerto);
				// Obtenemos el nickname
				String nick = new String (receivedMessage.getUserName());
				// Obtenemos los ficheros
				String fil = new String (receivedMessage.getFiles());
				// Si el usuario ya esta registrado como servidor ERROR.
				if (servers.containsKey(nick)) {
					server_files_fail(clientAddr);
				}else{
					// si el puerto esta repetido en el diccionario entonces tampoco puede ser servidor
					// hacer un bucle donde se recorra el diccionario servers y se obtenda el valor 
					// de cada entrada y se obtenga el campo puerto y se compare con el puerto
					// que se esta intentando registrar
					boolean registrar = true;
					for (HashMap.Entry<String, InetSocketAddress> entry : servers.entrySet()) {
						// Si ya hay un servidor con ese puerto, entonces no se puede registrar como servidor.
						if (entry.getValue().getPort() == puerto) {
							registrar = false;
							break;
						}
					}
					
					if (registrar == false) {
						server_files_fail(clientAddr);
					} else{
						servers.put(nick, socket_server);
						String hash = "";
						FileInfo file;
						String[] fich = fil.split("::" + ""); // Separamos pares <hash, FILE> de la cadena

						// Si hay ficheros que agregar se añade al diccionario.
						if (!fich[0].equals("")){	
							for (int i = 0; i < fich.length; i ++) {
								String[] fich_2 = fich [i].split(":" + ""); // Separamos hash y FILE
								hash = fich_2 [0].toLowerCase();
								file = FileInfo.fromEncodedString(fich_2[1]);
								// Si un fichero esta ya en el servidor no lo añadas
								if (files.containsKey(hash)) {
									
									// Si ya estaba el fichero pero con otro servidor, entonces lo añadimos esa informacion
									String servs = serve_files.get(hash);
									// Si un mismo servidor tiene 2 fichero iguales hacer que no se añada el nombre del servidor dos veces.
									
									String[] servs_repe = servs.split(":" + "");
									boolean repetido = false;
									
									for (int j = 0; j < servs_repe.length; j ++) {
										
										if (servs_repe[j].equals(nick)) {
											repetido = true;
										}
									}
									
									if (!repetido) {
										servs += ":" + nick ;
										serve_files.put(hash, servs);
										
										// modificamos el nombre del fichero para que tenga los nombres del ficheros segun el servidor que lo tenga.
										FileInfo fichero = files.get(hash);
										String nombre_fich = fichero.fileName+":"+file.fileName;
										fichero.fileName = nombre_fich;
										files.put(hash, fichero);
									}
									
								}else {
									// Añadimos al servidor el fichero.
									files.put(hash, file);
									// Añadimos el nombre del servdor con su respectico fichero.
									serve_files.put(hash, nick);
									//System.out.println ("Metemos al usuario: "+nick+" en el fihcero "+file.fileName);
								}
							}
						}
						server_files_ok (clientAddr);
					}
				}
				break;
			case DirMessageOps.OPCODE_SERVE_STOP:
				String nick_to_stop = new String (receivedMessage.getUserName());
				String fil_to_stop = new String (receivedMessage.getFiles());
				if (!servers.containsKey(nick_to_stop)){
					server_stop_fail(clientAddr);
				}else{
					// Eliminamos del diccionarios de servidores.
					servers.remove(nick_to_stop);

					String hash_stop = "";
					String[] fich_stop = fil_to_stop.split("::" + ""); // Separamos hash de la cadena

					// Si hay ficheros que se puedan eliminar del diccionario
					if (!fich_stop[0].equals("")){	
						for (int i = 0; i < fich_stop.length; i ++) {
							hash_stop = fich_stop[i];
							// Si un fichero esta en el servidor se elimina
							if (files.containsKey(hash_stop)) {
								
								/*
								 * Tener en cuenta que cuando se borra un fichero no se debe de eliminar del
								 * directorio hasta que no haya mas servidores que tengan ese fichero.
								 * */
								
								String servs = serve_files.get(hash_stop);
								
								String[] servs_repe = servs.split(":" + "");
								String nicks = "";
								
								FileInfo fichero = files.get(hash_stop);
								
								String [] fichero_nombres = fichero.fileName.split(":" + "");
								
								String fichero_nombre_final = "";
								
								for (int j = 0; j < servs_repe.length; j ++) {
									
									if (!servs_repe[j].equals(nick_to_stop)) {
										if (nicks.equals("")) {
											nicks += servs_repe[j];
											fichero_nombre_final += fichero_nombres[j] ;
										}
										else{
											nicks += ":" + servs_repe[j];
											fichero_nombre_final += ":" + fichero_nombres[j] ;
										}
									}
								}
								
								// Sino hay mas servidores con ese fichero, entonces lo borramos
								if (nicks.equals("")) {
									serve_files.remove(hash_stop);
									files.remove(hash_stop);
									// Eliminamos del diccionario de fichero - servidor cuando no hay ningun otro servidor con ese fichero
								}else{
									serve_files.put(hash_stop, nicks);
									// Quitamos el nombre del fichero segun el usuario que ha dejado de ser servidor.
									fichero.fileName = fichero_nombre_final;
									files.put(hash_stop, fichero);
								}
							}
						}
					}
					server_stop_ok(clientAddr);
				}
				break;
			case DirMessageOps.OPCODE_QUIT:
				String nick_to_aquit = new String (receivedMessage.getUserName());
				if (!nicks.containsKey(nick_to_aquit)){
					logout_Fail(clientAddr);
				}else{
					nicks.remove(nick_to_aquit);
					logout_Ok(clientAddr);
				}
				break;
		default:
		}
		
	}

	// Método para enviar la confirmación del login
	private void sendLoginOK(InetSocketAddress clientAddr) throws IOException {
		// TODO: Construir el datagrama con la respuesta y enviarlo por el socket al cliente
		// byte[] responseData = DirMessage.buildXXXXXXXResponseMessage();
		
		int numServers = servers.size();
		// Obtenemos un buffer de bytes que nos da info sobre los bytes del codigo de la opcion y número de los servidores.
		byte [] responseData = DirMessage.buildLoginOKResponseMessage(numServers);
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendRegisterOK(InetSocketAddress clientAddr) throws IOException {
		// TODO: Construir el datagrama con la respuesta y enviarlo por el socket al cliente
		// byte[] responseData = DirMessage.buildXXXXXXXResponseMessage();
		//String nick = clientAdd.getHostName();
		// Obtenemos un buffer de bytes que nos da info sobre los bytes del codigo de la opcion y número de los servidores.
		byte [] responseData = DirMessage.buildRegisterOKResponseMessage();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void sendRegisterFAIL(InetSocketAddress clientAddr) throws IOException {
		// TODO: Construir el datagrama con la respuesta y enviarlo por el socket al cliente
		// byte[] responseData = DirMessage.buildXXXXXXXResponseMessage();
		
		// Obtenemos un buffer de bytes que nos da info sobre los bytes del codigo de la opcion y número de los servidores.
		byte [] responseData = DirMessage.buildRegisterFAILResponseMessage();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendUserlist (InetSocketAddress clientAddr) throws IOException{
		
		byte [] responseData = DirMessage.buildUserListResponseMessage(nicks, servers);
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void sendFileList (InetSocketAddress clientAddr) throws IOException{
		
		byte [] responseData = DirMessage.buildFileListResponseMessage(files, serve_files);
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void UsernameNotFound (InetSocketAddress clientAddr) throws IOException{
		
		byte [] responseData = DirMessage.UsernameNotFound();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	
	private void UsernameFound (InetSocketAddress clientAddr, String nickToLookUp) throws IOException{
		
		byte [] responseData = DirMessage.UsernameFound(servers, nickToLookUp);
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	private void server_files_ok (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.serverFilesOk();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	private void server_files_fail (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.serverFilesFail();

		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void server_stop_fail (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.fgstopFail();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void server_stop_ok (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.fgstopOk();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
	
	private void logout_Fail (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.logoutFail();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	private void logout_Ok (InetSocketAddress clientAddr) throws IOException {
		byte [] responseData = DirMessage.logoutOk();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}

	/*
private void sendUserlistFail (InetSocketAddress clientAddr) throws IOException{
		
		byte [] responseData = DirMessage.buildUserListFailResponseMessage();
		// Creamos el paquete a enviar con la info que hemos dicho a la dirección del cliente.
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length ,clientAddr);
		socket.send(responsePacket);
	}
*/
}
