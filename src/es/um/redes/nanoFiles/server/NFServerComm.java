package es.um.redes.nanoFiles.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {

	public static final int LIMIT_HASH = 3;
	private static final int LENGTH_FILE_BLOCK = 4096;
	private static final String DELIMITER = "::";

	public static void serveFilesToClient(Socket socket) {
		
		// Bucle para atender mensajes del cliente
			
			/*
			 * TODO: Crear dis/dos a partir del socket
			 */
			try {
				boolean clientConnected = true;
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				String op = "";
				String dataFromCliente = "";
				PeerMessage messageFromClient = null;
				//BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			
			while (clientConnected) { // Bucle principal del servidor
				// TODO: Leer un mensaje de socket y convertirlo a un objeto PeerMessage
				
				try {
				
				dataFromCliente = dis.readUTF();

				messageFromClient = PeerMessage.fromString(dataFromCliente);
				//System.out.println ("EN COMM::\nop: " + messageFromClient.getOperation() + "\n fileHash: " + messageFromClient.getFileHash());
				op = messageFromClient.getOperation();

				/*
				 * TODO: Actuar en función del tipo de mensaje recibido. Se pueden crear
				 * métodos en esta clase, cada uno encargado de procesar/responder un tipo de petición.
				 */
				if (clientConnected){
					switch (op) {
						case PeerMessageOps.OP_DOWNLOAD:
							sendFile (messageFromClient.getFileHash (), dos);
							break;
						default:
							System.out.println("Unknown message type received");
					}
				}
				 
				} catch (IOException e0) {
					System.out.println("Client disconnected");
					clientConnected = false;
				}

			}
			socket.close();
			System.out.println ("Closed connection from "+ socket.getRemoteSocketAddress().toString());
			
		} catch (IOException e2) {
			System.err.println("*Error with the comunnication whith cliente*");
		}
	}

     public static void sendFile (String hash, DataOutputStream dos) throws IOException{

		// Tneemos que ver si el fichero con ese hash esta en el servidor o no.
		// Si esta, lo enviamos al cliente.
		// Si no esta, enviamos un mensaje de error al cliente.
        
		FileInfo[] files = FileInfo.lookupHashSubstring( NanoFiles.db.getFiles(), hash);

		// Sino existe el fichero, entonces enviamos un mensaje de error al cliente.
		if (files.length == 0){

			PeerMessage peerMessage = new PeerMessage(PeerMessageOps.OP_FILENOTFOUND);
			dos.writeUTF(peerMessage.toEncodedString());

		}else{

			// Si existe el fichero, entonces enviamos el fichero al cliente.
			// El formato sera la operacion de FileData y luego el campo FileData tendrá el contenido del fichero. Y un 
			

			FileInfo fichero = files[0];
			long longitudFichero = fichero.fileSize;
			
			FileInputStream fis = new FileInputStream (fichero.filePath);
			
			byte data[];
			String fin = "0";
			String fileData = "";
			
			while (longitudFichero > LENGTH_FILE_BLOCK) {
				
				data = new byte [LENGTH_FILE_BLOCK]; // Actualizamos para que no haya datos antiguos
				
				fis.read(data);
				
				fileData = java.util.Base64.getEncoder().encodeToString(data);
					
				PeerMessage peerMessage = new PeerMessage(PeerMessageOps.OP_FILEDATA, fileData, fin);
				dos.writeUTF(peerMessage.toEncodedString());
				
				longitudFichero = longitudFichero - LENGTH_FILE_BLOCK;

			}
			
			fin = "1";
			
			data = new byte [(int) longitudFichero]; // Actualizamos para que no haya datos antiguos
			
			fis.read(data);
			
			fileData = java.util.Base64.getEncoder().encodeToString(data);
				
			PeerMessage peerMessage = new PeerMessage(PeerMessageOps.OP_FILEDATA, fileData, fin);
			dos.writeUTF(peerMessage.toEncodedString());
			
			fis.close();

	 	}
	}
}
