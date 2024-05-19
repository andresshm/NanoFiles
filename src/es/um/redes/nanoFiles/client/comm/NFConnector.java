package es.um.redes.nanoFiles.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.server.NFServerComm;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NFConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NFConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		/*
		 * TODO Se crea el socket a partir de la dirección del servidor (IP, puerto). La
		 * creación exitosa del socket significa que la conexión TCP ha sido
		 * establecida.
		 */
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		/*
		 * TODO Se crean los DataInputStream/DataOutputStream a partir de los streams de
		 * entrada/salida del socket creado. Se usarán para enviar (dos) y recibir (dis)
		 * datos del servidor mediante los métodos readUTF y writeUTF (mensajes
		 * formateados como cadenas de caracteres codificadas en UTF8)
		 */
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	/**
	 * Método para cerrar el socket y liberar los recursos asociados a él.
	 * 
	 * @throws IOException
	 */
	public void disconnectedSocket() throws IOException {
		socket.close();
	}

	/**
	 * Método que utiliza el Shell para ver si hay datos en el flujo de entrada.
	 * Permite "sondear" el socket con el fin evitar realizar una lectura bloqueante
	 * y así poder realizar otras tareas mientras no se ha recibido ningún mensaje.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	/**
	 * Método para descargar un fichero a través del socket mediante el que estamos
	 * conectados con un peer servidor.
	 * 
	 * @param targetFileHashSubstr El hash del fichero a descargar
	 * @param file                 El objeto File que referencia el nuevo fichero
	 *                             creado en el cual se escriben los datos
	 *                             descargados del servidor (contenido del fichero
	 *                             remoto)
	 * @throws IOException Si se produce algún error al leer/escribir del socket.
	 */
	public void download(String targetFileHashSubstr, File file) throws IOException {
		/*
		 * TODO: Construir objeto PeerMessage que modela un mensaje de solicitud de
		 * descarga de fichero (indicando el fichero a descargar), convertirlo a su
		 * codificación en String (mediante toEncodedString) y enviarlo al servidor.
		 */
		PeerMessage peerMessage = new PeerMessage(PeerMessageOps.OP_DOWNLOAD, targetFileHashSubstr);
		dos.writeUTF(peerMessage.toEncodedString());
		/*
		 * TODO: Recibir mensajes del servidor codificados como cadena de caracteres,
		 * convertirlos a PeerMessage (mediante "fromString"), y actuar en función del
		 * tipo de mensaje recibido.
		 */
		String message = dis.readUTF();
		PeerMessage peerMessage2 = PeerMessage.fromString(message);

		switch (peerMessage2.getOperation()) {
		case PeerMessageOps.OP_FILEDATA:
			System.out.println ("File downloaded");
			break;
		case PeerMessageOps.OP_FILENOTFOUND:
			System.out.println ("*File not found.*");
			if (!file.delete()) System.out.println ("*Error deleting File*");
			return;
		default:
			System.out.println("Unknown message type received");
			return;
		}

		
		/*
		 * TODO: Crear un FileOutputStream a partir de "file" para escribir cada
		 * fragmento recibido en el fichero. Cerrar el FileOutputStream una vez se han
		 * escrito todos los fragmentos.
		 */

		 // Debemos de poner true para que no sobreescriba el fichero


		FileOutputStream fos = new FileOutputStream(file, true);
		
		boolean comsumir_ficheros = true;
		
		while (comsumir_ficheros) {
			
			String contenidoFichero = peerMessage2.getFiledata();

			String ultimoByte = peerMessage2.getUltimoByte();
			
			byte [] bloque = java.util.Base64.getDecoder().decode(contenidoFichero);
			
			fos.write(bloque);
			
			if (ultimoByte.equals("1")) {
				comsumir_ficheros = false;
			}else {
				message = dis.readUTF();
				peerMessage2 = PeerMessage.fromString(message);
			}
		}

		fos.close(); // Cerrar el FileOutputStream una vez se han escrito todos los fragmentos

		/*
		 * TODO: Comprobar la integridad del fichero creado, calculando su hash y
		 * comparándolo con el hash del fichero solicitado.
		 */

		byte [] hash  = FileDigest.computeFileChecksum(file.getPath());
		String hashString = FileDigest.byteArrayToHexString(hash);

		if (hashString.contains(targetFileHashSubstr)) {

			System.out.println("File downloaded successfully");

		} else {
			System.out.println("*File downloaded with errors*");
		}
		NanoFiles.db.addFile(file, hashString);
	}
}
