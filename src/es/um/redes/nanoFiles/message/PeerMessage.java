package es.um.redes.nanoFiles.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases NFServerComm y NFConnector, y se
 * codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class PeerMessage {
	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	/*
	 * TODO: Definir de manera simbólica los nombres de todos los campos que pueden
	 * aparecer en los mensajes de este protocolo (formato campo:valor)
	 */
	 private static final String FIELDNAME_FILEDATA = "filedata";
	 private static final String FIELDNAME_FILEHASH = "hash";
	 private static final String FIELDNAME_ISLAST = "islast";
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private static String operation;
	/*
	 * TODO: Crear un atributo correspondiente a cada uno de los campos de los
	 * diferentes mensajes de este protocolo.
	 */
	private static String fileblocks;
	private static String hash;
	private static String ultimoByte;
	
	/*
	 * TODO: Crear diferentes constructores adecuados para construir mensajes de
	 * diferentes tipos con sus correspondientes argumentos (campos del mensaje)
	 */

	// Mensaje de solicutud de fichero para descargar
	 public PeerMessage(String op, String file) {
		assert(op.equals(PeerMessageOps.OP_DOWNLOAD));

		//System.out.println ("Estamos en el constructor de PeerMessage");
		//System.out.println ("op: " + op + " file: " + file);
		//System.out.println ("PeerMessageOps.OP_DOWNLOAD:" + PeerMessageOps.OP_DOWNLOAD + "\nop:" + op);
		operation = op;
		hash = file;
	}

	public PeerMessage (String op, String file, String ultByte){
		assert(op.equals(PeerMessageOps.OP_FILEDATA));
		
		operation = op;
		fileblocks = file;
		ultimoByte = ultByte;
	}

	// Mensaje de control.
	public PeerMessage(String op) {
		assert(op == PeerMessageOps.OP_FILENOTFOUND);
		operation = op;
	}

	// Getters

	public String getOperation() {
		return operation;
	}

	public String getFiledata() {
		return fileblocks;
	}

	public String getFileHash() {
		return hash;
	}

	public String getUltimoByte (){
		return ultimoByte;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static PeerMessage fromString(String message) {
		/*
		 * TODO: Usar un bucle para parsear el mensaje línea a línea, extrayendo para
		 * cada línea el nombre del campo y el valor, usando el delimitador DELIMITER, y
		 * guardarlo en variables locales.
		 */

		if (message.equals("")) return null;

		// Obtenemos los campos de los mensajes
		String [] lines = message.split (String.valueOf (END_LINE));
		String firstLine = lines [0];
		PeerMessage receivedMessage = null;
		String op = "";
		String messageField;
		String messageValue;
		String ultimoB;
		
		int indice = lines[0].indexOf(DELIMITER);
		if (indice != -1 && indice != 0 && indice != firstLine.length() -1){
			messageField = lines[0].substring(0, indice).toLowerCase();
			messageValue = lines[0].substring(indice +1).trim();
			// El campo operation siempre va a estar.
			if (messageField.equals(FIELDNAME_OPERATION)){
				op = messageValue;
			}else {
				System.out.println ("*Message Format Error*");
				return null;
			}

		}else {
			System.out.println ("*Message Format Error*");
			return null;
		}

		assert (op == PeerMessageOps.OP_DOWNLOAD ||
				op == PeerMessageOps.OP_FILEDATA ||
				op == PeerMessageOps.OP_FILENOTFOUND);
		
		switch (op){

			case PeerMessageOps.OP_DOWNLOAD:
				if (lines.length == 2){
					indice = lines [1].indexOf (DELIMITER);
					if (indice != -1 && indice != 0 && indice != lines[1].length() -1){
						messageField = lines[1].substring(0, indice).toLowerCase();
						messageValue = lines[1].substring(indice +1).trim();
						//System.out.println ("Vemos que hay en messageValue: "+messageValue+"\n");
						// El campo operation siempre va a estar.
						if (messageField.equals(FIELDNAME_FILEHASH)){
							//System.out.println ("ANTES de construir:\nop: "+op+"\nhash: "+messageValue);
							receivedMessage = new PeerMessage(op, messageValue);
							//System.out.println ("Despues de construir:\nop: "+receivedMessage.getOperation()+"\nhash: "+receivedMessage.getFileHash());
						}else {
							System.out.println ("*Message Download Format Error*");
							return null;
						}
					}else {
						System.out.println ("*Message Download Format Error*");
						return null;
					}
				}else {
					System.out.println ("*Message Download Format Error*");
					return null;
				}

			
			break;

			case PeerMessageOps.OP_FILENOTFOUND:
				if (lines.length == 1){
				receivedMessage = new PeerMessage(op);
				}else {
					System.out.println ("*Message ControlMessage Format Error*");
					return null;
				}
			break;

			case PeerMessageOps.OP_FILEDATA:
				if (lines.length == 3){
					indice = lines[1].indexOf (DELIMITER);
					if (indice != -1 && indice != 0 && indice != lines[1].length() -1){
						messageField = lines[1].substring(0, indice).toLowerCase();
						messageValue = lines[1].substring(indice +1).trim();
						// El campo operation siempre va a estar.
						if (messageField.equals(FIELDNAME_FILEDATA)){

							indice = lines[2].indexOf (DELIMITER);
							if (indice != -1 && indice != 0 && indice != lines[2].length() -1){
								messageField = lines[2].substring(0, indice).toLowerCase();
								ultimoB = lines[2].substring(indice +1).trim();
								//System.out.println ("MessageField: "+messageField+"\n");
								if (messageField.equals(FIELDNAME_ISLAST)){
									//System.out.println ("ANTES de construir:\nop: "+op+"\nfiledata: "+messageValue+"\nultimoByte: "+ultimoB);
									receivedMessage = new PeerMessage(op, messageValue, ultimoB);
								}else {
									System.out.println ("*Message FielData Format Error*");
									return null;
								}

							}else {
								System.out.println ("*Message FielData Format Error*");
								return null;
							}
					}else {
						System.out.println ("*Message FielData Format Error*");
						return null;
					}
				}else {
					System.out.println ("*Message FielData Format Error*");
					return null;
				}
				}else {
					System.out.println ("*Message FielData Format Error*");
					return null;
				}
				break;

			default:
				System.out.println ("*Message Format Error*");
				return null;
			}

/* byte[] fileData = java.util.Base64.getDecoder().decode(str);

		 * TODO: En función del tipo del mensaje, llamar a uno u otro constructor con
		 * los argumentos apropiados, para establecer los atributos correpondiente, y
		 * devolver el objeto creado. Se debe detectar que sólo aparezcan los campos
		 * esperados para cada tipo de mensaje.
		 */

		 return receivedMessage;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toEncodedString() {
		/*
		 * TODO: En función del tipo de mensaje, crear una cadena con el tipo y
		 * concatenar el resto de campos necesarios usando los valores de los atributos
		 * del objeto.
		 */
		StringBuffer sb = new StringBuffer();

		switch (operation){
			// Si queremos una descarga de fichero
			case PeerMessageOps.OP_DOWNLOAD:
				sb.append(FIELDNAME_OPERATION + DELIMITER + PeerMessageOps.OP_DOWNLOAD + END_LINE);
				sb.append(FIELDNAME_FILEHASH + DELIMITER + hash + END_LINE);
				sb.append(END_LINE);
				break;
			case PeerMessageOps.OP_FILENOTFOUND:
				sb.append(FIELDNAME_OPERATION + DELIMITER + PeerMessageOps.OP_FILENOTFOUND + END_LINE);
				sb.append(END_LINE);
				break;
			case PeerMessageOps.OP_FILEDATA:
				sb.append(FIELDNAME_OPERATION + DELIMITER + PeerMessageOps.OP_FILEDATA + END_LINE);
				sb.append(FIELDNAME_FILEDATA + DELIMITER + fileblocks + END_LINE);
				sb.append(FIELDNAME_ISLAST + DELIMITER + ultimoByte + END_LINE);
				sb.append(END_LINE);
				break;
			default:
				System.out.println ("*Error format message*");
		}

		return sb.toString();
	}
}
