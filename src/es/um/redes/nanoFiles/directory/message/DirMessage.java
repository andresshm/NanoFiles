package es.um.redes.nanoFiles.directory.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import es.um.redes.nanoFiles.util.FileInfo;

import java.util.Set;


public class DirMessage {

	public static final int PACKET_MAX_SIZE = 65536;

	public static final byte OPCODE_SIZE_BYTES = 1;

	private byte opcode;

	// Solo presente mensaje de solicitud del nickname
	private String userName;
	
	// Para que se guarde en servers el puerto y nombre del servidor
	private int puerto = -1;

	// Para que se guarde en files hash y fichero del servidor
	private String ficheros;
	
	public DirMessage(byte operation) {
		//userlist
		assert (operation == DirMessageOps.OPCODE_GETUSERS ||
				operation == DirMessageOps.OPCODE_LOGIN);
		//aserto (opcode_userlist)
		opcode = operation;
	}


	/*
	 * TODO: Añadir atributos y crear otros constructores específicos para crear
	 * mensajes con otros tipos de datos
	 * 
	 */
	public DirMessage(byte operation, String nick) {
		assert (opcode == DirMessageOps.OPCODE_REGISTER_USERNAME);
		/*
		 * TODO: Añadir al aserto el resto de opcodes de mensajes con los mismos campos
		 * (utilizan el mismo constructor)
		 */
		opcode = operation;
		userName = nick;
	}
	
	// Cuando un usuario se hace servidor se tiene que guarsar su nick, puerto, y ficheros
	public DirMessage(byte operation, int port, String nick, String fich) {
		assert (opcode == DirMessageOps.OPCODE_SERVE_FILES);
		/*
		 * TODO: Añadir al aserto el resto de opcodes de mensajes con los mismos campos
		 * (utilizan el mismo constructor)
		 */
		opcode = operation;
		userName = nick;
		puerto = port;
		ficheros = fich;
	}

	// Cuando un usuario se hace servidor se tiene que guarsar su nick, puerto, y ficheros
	public DirMessage(byte operation, String nick, String fich) {
		assert (opcode == DirMessageOps.OPCODE_SERVE_STOP);
		/*
		 * TODO: Añadir al aserto el resto de opcodes de mensajes con los mismos campos
		 * (utilizan el mismo constructor)
		 */
		opcode = operation;
		userName = nick;
		ficheros = fich;
	}

	/**
	 * Método para obtener el tipo de mensaje (opcode)
	 * @return
	 */
	public byte getOpcode() {
		return opcode;
	}

	public String getUserName() {
		if (userName == null) {
			System.err.println(
					"PANIC: DirMessage.getUserName called but 'userName' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return userName;
	}
	
	public int getPuerto() {
		if (puerto == -1) {
			System.err.println(
					"PANIC: DirMessage.getPuerto called but 'puerto' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return puerto;
	}
	
	public String getFiles() {
		if (ficheros == null) {
			System.err.println(
					"PANIC: DirMessage.getFiles called but 'ficheros' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return ficheros;
	}


	/**
	 * MÃétodo de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El
	 * @return
	 */
	public static DirMessage buildMessageFromReceivedData(byte[] data) {
		/*
		 * TODO: En función del tipo de mensaje, parsear el resto de campos para extraer
		 * los valores y llamar al constructor para crear un objeto DirMessage que
		 * contenga en sus atributos toda la información del mensaje
		 */
	    DirMessage receivedMessage = null;
	    
	    ByteBuffer bb = ByteBuffer.wrap(data);
	    byte receivedOpCode = bb.get();
	
	    switch (receivedOpCode) {
	    case DirMessageOps.OPCODE_LOGIN:
	    	receivedMessage = new DirMessage(DirMessageOps.OPCODE_LOGIN);
	    	break;
	    case DirMessageOps.OPCODE_REGISTER_USERNAME:
	    	int longitud = bb.getInt();
	    	byte [] mensaje = new byte [longitud];
	    	bb.get(mensaje);
	    	// traducimos el mensaje para que se guarde en el hashmap en UTF-8
	    	String nick = new String(mensaje, StandardCharsets.UTF_8);
	    	receivedMessage = new DirMessage(DirMessageOps.OPCODE_REGISTER_USERNAME, nick);
	    	break;
	   case DirMessageOps.OPCODE_GETUSERS:
			receivedMessage = new DirMessage(DirMessageOps.OPCODE_GETUSERS);
			break;
		case DirMessageOps.OPCODE_GETFILES:
			receivedMessage = new DirMessage(DirMessageOps.OPCODE_GETFILES);
			break;
	   case DirMessageOps.OPCODE_LOOKUP_USERNAME:
		    int longitud1 = bb.getInt();
	    	byte [] mensaje1 = new byte [longitud1];
	    	bb.get(mensaje1);
	    	// traducimos el mensaje para que se guarde en el hashmap en UTF-8
	    	String nick1 = new String(mensaje1, StandardCharsets.UTF_8);
	    	receivedMessage = new DirMessage(DirMessageOps.OPCODE_LOOKUP_USERNAME, nick1);
	    	break;
	   case DirMessageOps.OPCODE_SERVE_FILES:
	   
		   int puerto = bb.getInt();
		   int longitud_nick = bb.getInt();
		   byte [] data_nomb = new byte [longitud_nick];
		   bb.get(data_nomb);
		   String nombre = new String(data_nomb, StandardCharsets.UTF_8);
		   int longitud_fich = bb.getInt();
		   byte [] data_fich = new byte [longitud_fich];
		   bb.get(data_fich);
		   String fich = new String(data_fich, StandardCharsets.UTF_8);
		   receivedMessage = new DirMessage(DirMessageOps.OPCODE_SERVE_FILES, puerto, nombre, fich);
		   break;

		case DirMessageOps.OPCODE_SERVE_STOP:
			int longitud_nick_Stop = bb.getInt();
		    byte [] data_nomb_stop = new byte [longitud_nick_Stop];
		    bb.get(data_nomb_stop);
		    String nombre_stop = new String(data_nomb_stop, StandardCharsets.UTF_8);
		    int longitud_fich_stop = bb.getInt();
		    byte [] data_fich_stop = new byte [longitud_fich_stop];
		    bb.get(data_fich_stop);
		    String fich_stop = new String(data_fich_stop, StandardCharsets.UTF_8);
			receivedMessage = new DirMessage(DirMessageOps.OPCODE_SERVE_STOP, nombre_stop, fich_stop);
			break;
		
		case DirMessageOps.OPCODE_QUIT:
			int longitud_nick_Quit = bb.getInt();
		    byte [] data_nomb_quit = new byte [longitud_nick_Quit];
		    bb.get(data_nomb_quit);
		    String nombre_quit = new String(data_nomb_quit, StandardCharsets.UTF_8);
			receivedMessage = new DirMessage(DirMessageOps.OPCODE_QUIT, nombre_quit);
			break;
			
	    default:
	    }
	    
		return receivedMessage;
	}

	/**
	 * Método para construir una solicitud de ingreso en el directorio
	 * 
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN);
		return bb.array();
	}

	/**
	 * Método para construir una respuesta al ingreso del peer en el directorio
	 * 
	 * @param numServers El número de peer registrados como servidor en el
	 *                   directorio
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginOKResponseMessage(int numServers) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN_OK);
		bb.putInt(numServers);
		return bb.array();
	}

	/**
	 * Método que procesa la respuesta a una solicitud de login
	 * 
	 * @param data El mensaje de respuesta recibido del directorio
	 * @return El número de peer servidores registrados en el directorio en el
	 *         momento del login, o -1 si el login en el servidor ha fallado
	 */
	public static int processLoginResponse(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data);
		// Obtiene el codigo de opción del paquete.
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_LOGIN_OK) {
			return buf.getInt(); // Return number of available file servers
		} else {
			return -1;
		}
	}

	public static byte[] buildRegisterRequestMessage(String nickname) {
		
		byte [] datos = nickname.getBytes();
		int longitud = datos.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME);
		bb.putInt(longitud);
		bb.put(datos);
		return bb.array();
	}
	
	public static byte[] buildRegisterOKResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_OK);
		return bb.array();
	}
	
	public static byte[] buildRegisterFAILResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL);
		return bb.array();
		
	}
	
	
	public static boolean processRegisterResponse(byte[] data) {
		// Obtiene el codigo de opción del paquete y envía la respuesta.
		
		if (data == null) return false;
		ByteBuffer buf = ByteBuffer.wrap(data);
		// Obtiene el codigo de opción del paquete.
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_REGISTER_USERNAME_OK) {
			return true; // Se ha podido registrar
		} else {
			return false; // No se ha podido resgistrar
		}
	}
	
	public static byte[] buildUserListRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETUSERS);
		return bb.array();
	}
	
	public static byte[] buildUserListResponseMessage(HashMap<String, LocalDateTime> usuarios, HashMap<String, InetSocketAddress> servers) {
	
	   String nickNoServers = "";
	   String nickServers = "";
	   for (HashMap.Entry<String, LocalDateTime> entry : usuarios.entrySet()) {
	        String nick = entry.getKey();
	        if (servers.containsKey(nick)) {
	            nickServers = nickServers + nick + ":";
	        }
	        else nickNoServers = nickNoServers + nick + ":";
		
	    }
	    // Distinguimos los usuarios servidores de los que no lo son mediante los ::
	    String nicks = nickNoServers+":"+nickServers;
	    byte [] datos = nicks.getBytes();
		int longitud = datos.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_USERLIST);
		bb.putInt(longitud);
		bb.put(datos);
		return bb.array();
	}
	/*
	public static byte[] buildUserListFailResponseMessage(){
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_USERLISTFAIL);
		return bb.array();
	}
	*/
	public static byte [] processUserListResponse(byte[] data) {
		// Obtiene el codigo de opción del paquete y envía la respuesta.
		if (data == null) return null;
		ByteBuffer buf = ByteBuffer.wrap(data);
		// Obtiene el codigo de opción del paquete.
		byte opcode = buf.get();
		int longitud = buf.getInt();
		byte [] nicks = new byte [longitud];
		buf.get(nicks);
		
		return nicks;
	}
	
	
	public static byte[] buildLookUpUsernameRequestMessage(String nickname) {
		
		byte [] datos = nickname.getBytes();
		int longitud = datos.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME);
		bb.putInt(longitud);
		bb.put(datos);
		return bb.array();
	}

	public static byte[] UsernameNotFound() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND);
		return bb.array();
	}
	
	// Obtenemos la direccion y la IP.
	public static byte[] UsernameFound(HashMap<String, InetSocketAddress> servers, String nickToLookUp) {
		
		int puerto = 0;
		byte [] direccion = null;
		InetSocketAddress socket = null;
		// Obtenemos el puerto y la direccion.
		for (HashMap.Entry<String, InetSocketAddress> entry : servers.entrySet()) {
		       String nick = entry.getKey();
		       if (nick.equals(nickToLookUp)) {
		    	   socket = entry.getValue();
		    	   break;
		       }
		}
		puerto = socket.getPort();
		
		//Obtenemos la direccion en forma de un array de bytes.
		direccion = socket.getAddress().getAddress();
		
		//int longitud_dir = direccion.length;
		// Construimos el mensaje
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+ Integer.BYTES+Integer.BYTES);
		bb.put (DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND);
		bb.putInt (puerto);
		bb.put(direccion);
		return bb.array();
	}
	
	
	public static InetSocketAddress processLookUpUsernameResponse (byte [] data) {
		
		if (data == null) return null;
		InetSocketAddress socket = null;
		ByteBuffer buf = ByteBuffer.wrap(data);
		// Obtiene el codigo de opción del paquete.
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND) {
			return null;
		}else {
			int puerto = buf.getInt();
			byte [] IP = new byte [Integer.BYTES];
			buf.get(IP);
			try {
				socket = new InetSocketAddress(InetAddress.getByAddress(IP), puerto);
			} catch (UnknownHostException e) {
				System.err.println ("Error Dirección IP no encontrada desconocida.");
			}
		}
		
		return socket;
	}
	
	public static byte [] buildPublishLocalFilesRequestMessage(int port, String nickname, FileInfo [] ficheros) {
		byte [] nick_data = nickname.getBytes(); // reservamos memoria para el nickname
		int longitud_nick = nick_data.length;
		String fich = ""; // reservamos memoria para los fihceros <hash:FILE>
		int i; 
		for (i = 0; i < ficheros.length-1; i ++) {
			fich +=  ficheros[i].fileHash.substring(0, 3);
			fich += ":";
			fich += ficheros[i].toEncodedString();
			fich += "::";
		}
		if (ficheros.length != 0){
			fich +=  ficheros[i].fileHash.substring(0, 3);
			fich += ":";
			fich += ficheros[i].toEncodedString();
		}
		byte [] fich_data = fich.getBytes();
		int longitud_fich = fich_data.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + Integer.BYTES + longitud_nick + Integer.BYTES + longitud_fich);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES);
		bb.putInt(port);
		bb.putInt(longitud_nick);
		bb.put(nick_data);
		bb.putInt(longitud_fich);
		bb.put(fich_data);
		return bb.array();
	}
	
	public static byte [] serverFilesOk() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_SERVE_FILES_OK);
		return bb.array();
	}
	
	public static byte [] serverFilesFail() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_SERVE_FILES_FAIL);
		return bb.array();	
	}
	
	public static boolean processPublishLocalFilesResponse(byte[] data) {
		if (data == null) return false;
		
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		return opcode == DirMessageOps.OPCODE_SERVE_FILES_OK;
	}

	public static byte [] buildFgstopRequestMessage (String nick, FileInfo [] ficheros){
		byte [] nick_data = nick.getBytes(); // reservamos memoria para el nickname
		int longitud_nick = nick_data.length;
		String fich = ""; // reservamos memoria para los fihceros <hash>
		int i; 
		for (i = 0; i < ficheros.length-1; i ++) {
			fich +=  ficheros[i].fileHash.substring(0, 3);
			fich += "::";
		}
		if (ficheros.length != 0){
			fich +=  ficheros[i].fileHash.substring(0, 3);
		}
		byte [] fich_data = fich.getBytes();
		int longitud_fich = fich_data.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud_nick + Integer.BYTES + longitud_fich);
		bb.put(DirMessageOps.OPCODE_SERVE_STOP);
		bb.putInt(longitud_nick);
		bb.put(nick_data);
		bb.putInt(longitud_fich);
		bb.put(fich_data);
		return bb.array();
	}

	public static byte [] fgstopOk() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_SERVE_STOP_OK);
		return bb.array();
	}

	public static byte [] fgstopFail(){
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put (DirMessageOps.OPCODE_SERVE_STOP_FAIL);
		return bb.array();
	}

	public static boolean processFgstopResponse(byte[] data) {
		if (data == null) return false;
		
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		return opcode == DirMessageOps.OPCODE_SERVE_STOP_OK;
	}
	
public static byte[] buildLogoutRequestMessage(String nickname) {
		
		byte [] datos = nickname.getBytes();
		int longitud = datos.length;
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
		bb.put(DirMessageOps.OPCODE_QUIT);
		bb.putInt(longitud);
		bb.put(datos);
		return bb.array();
	}

public static byte [] logoutFail() {
	ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
	bb.put (DirMessageOps.OPCODE_QUIT_FAIL);
	return bb.array();
}

public static byte [] logoutOk(){
	ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
	bb.put (DirMessageOps.OPCODE_QUIT_OK);
	return bb.array();
}

public static boolean processLogoutResponse (byte[] data) {
	
	if (data == null) return false;
	
	ByteBuffer buf = ByteBuffer.wrap(data);
	byte opcode = buf.get();
	return opcode == DirMessageOps.OPCODE_QUIT_OK;
	
}

public static byte [] buildFileListRequestMessage (){
	ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
	bb.put(DirMessageOps.OPCODE_GETFILES);
	return bb.array();
}

public static byte[] buildFileListResponseMessage( HashMap <String, FileInfo> files,  HashMap <String, String> serve_files){
	
	String ficheros = "";
	for (HashMap.Entry<String, FileInfo> entry : files.entrySet()) {
		 FileInfo info = entry.getValue();
		 String servidores = serve_files.get(entry.getKey());
		 
		ficheros = ficheros + info.fileName + "::" + info.fileSize + "::" + info.fileHash + "::"+ servidores +":::";
	 }

	 byte [] datos = ficheros.getBytes();
	 int longitud = datos.length;
	 ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES + longitud);
	 bb.put(DirMessageOps.OPCODE_FILELIST);
	 bb.putInt(longitud);
	 bb.put(datos);
	 return bb.array();
 }
 
 public static byte [] processFileListResponse(byte[] data) {
	// Obtiene el codigo de opción del paquete y envía la respuesta.
	
	if (data == null) return null;
	ByteBuffer buf = ByteBuffer.wrap(data);
	// Obtiene el codigo de opción del paquete.
	byte opcode = buf.get();
	int longitud = buf.getInt();
	byte [] ficheros = new byte [longitud];
	buf.get(ficheros);
	
	return ficheros;
}
	/*
	 * TODO: Crear métodos buildXXXXRequestMessage/buildXXXXResponseMessage para
	 * construir mensajes de petición/respuesta
	 */
	// public static byte[] buildXXXXXXXResponseMessage(byte[] responseData)
	/*
	 * TODO: Crear métodos processXXXXRequestMessage/processXXXXResponseMessage para
	 * parsear el mensaje recibido y devolver un objeto según el tipo de dato que
	 * contiene, o boolean si es únicamente éxito fracaso.
	 */
	// public static boolean processXXXXXXXResponseMessage(byte[] responseData)

	}
