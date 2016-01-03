package pingpong.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;


public class MChannel extends NioChannel{
	
	private SocketChannel channel;
	private MEngine nioEngine; 			
	private DeliverCallback dCallback;
	private ConnectCallback cCallback;
	private ByteBuffer BufferSortie, BufferEntree,lenBufferSortie, lenBufferEntree; // Longueur du message contenu dans outBuffer et inBuffer
	private ByteBuffer temp;
	private LinkedList<ByteBuffer> BuffersSortie;
	
    // Etats de l'automate READ_AUTOMATA et WRITE_AUTOMATA
    private enum READ {READING_LENGTH,READING_MSG,READING_DONE};
    private enum WRITE {WRITING_LENGTH,WRITING_MSG,WRITING_CHECKSUM,WRITING_DONE};
    
    // Etats courant des automates READ_AUTOMATA et WRITE_AUTOMATA
    private READ EtatCourantR;
    private WRITE EtatCourantW;
    private boolean sendingCheckSum=false;
    private boolean readingCheckSum=false;
    private long checksum;
    
    public MChannel(MEngine nioE, SocketChannel channel, ConnectCallback cb){
		this.channel=channel;
		this.nioEngine=nioE;
		this.cCallback = cb;		
		// Initialisation du buffer en lecture
		lenBufferEntree = ByteBuffer.allocate(4);		
		BuffersSortie = new LinkedList<ByteBuffer>();		
		// Initialisation de l'automate en lecture
		EtatCourantR = READ.READING_LENGTH;	// On s'apprete a lire la longueur du message
	}
    
    public MChannel(MEngine nioE, SocketChannel channel)
	{
		this.channel=channel;
		this.nioEngine=nioE;
		// Initialisation du buffer en lecture
		lenBufferEntree = ByteBuffer.allocate(4);
		
		BuffersSortie = new LinkedList<ByteBuffer>();
		
		// Initialisation de l'automate en lecture
		EtatCourantR = READ.READING_LENGTH;	// Pret a lire la longueur d'un message
	}

	@Override
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public SocketChannel getChannel() {
		return channel;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		InetSocketAddress Adr = null;
		try {
			Adr = (InetSocketAddress) channel.getRemoteAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Adr;
	}

	@Override
	public void send(ByteBuffer arg0) {
		// TODO Auto-generated method stub
		try {
			channel.write(arg0);
		} catch (IOException e) {
			System.out.println("Erreur d'envoi");
			e.printStackTrace();
		}		
	}
	@Override
	public void send(byte[] arg0, int arg1, int arg2) {
		ByteBuffer b = ByteBuffer.wrap(arg0, arg1, arg2);
		try {
			channel.write(new ByteBuffer[]{b}, arg1, arg2);
		} catch (IOException e) {
			System.out.println("Erreur d'envoi");
			e.printStackTrace();
		}
	}

	@Override
	public void setDeliverCallback(DeliverCallback arg0) {
		this.dCallback = arg0;
	}
	
	public void setConnectCallback(ConnectCallback arg0) {
		this.cCallback = arg0;
	}
	
	public ConnectCallback getConnectCallback(){
		return this.cCallback;
	}
	
	public DeliverCallback getDeliverCallback(){
		return this.dCallback;
	}
	public void connected(){
		cCallback.connected(this);
	}
	public void InsertMessage(ByteBuffer arg0){
		BuffersSortie.addLast(arg0);
	}
	
	// FONCTION A REVOIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIR
	public long calculateCheckSum(byte[] data)
	{
		Checksum checksum = new CRC32();		
		// update the current checksum with the specified array of bytes
		checksum.update(data, 0, data.length);		 
		// get the current checksum value
		return checksum.getValue();
	}
	
	public void onWritable()
	{
		while(BuffersSortie.size()>0){
			
			if (EtatCourantW==WRITE.WRITING_LENGTH){		
				BufferSortie = BuffersSortie.removeFirst();	
				BufferSortie.position(0);
				lenBufferSortie = ByteBuffer.allocate(4).putInt(BufferSortie.capacity()); // Insertion de la longueur du message dans le buffer
				lenBufferSortie.position(0); // On se replace au début du buffer pour envoyer tout son contenu a partir de l'indice 0
				send(lenBufferSortie);
				
				if (lenBufferSortie.remaining()==0) {// Tout le buffer (longueur du message) a ete envoye
					EtatCourantW=WRITE.WRITING_MSG;		
				}
			}
			
			// Ecriture contenu du message
			if (EtatCourantW==WRITE.WRITING_MSG){
				System.out.println("Sending from " +this.nioEngine.getName()+" : " + new String(BufferSortie.array()));
				send(BufferSortie); // Envoi des donnees restantes definies dans le outBuffer
				
				if (BufferSortie.remaining()==0) // Si tout le message a ete parcouru => le message a ete correctement envoye (en entier)
				{
					nioEngine.removeWriteInterest(channel);
					
					EtatCourantW=WRITE.WRITING_DONE;
					EtatCourantR=READ.READING_LENGTH;
					if (!sendingCheckSum){	// Envoi du checksum (longueur + numero)
						lenBufferSortie.position(0); // On se place au debut du buffer
						int length = lenBufferSortie.getInt(); // Lecture de 4 octets et formation du int correspondant
						
						byte[] data = new byte[length];
						BufferSortie.position(0);		
						BufferSortie.get(data, 0, length);
						
						long checksum = calculateCheckSum(data);
						BuffersSortie.addFirst(ByteBuffer.wrap(String.valueOf(checksum).getBytes()));
						nioEngine.addWriteInterest(channel);		
						EtatCourantW=WRITE.WRITING_LENGTH;
						//send_pretreatments(String.valueOf(checksum));
						sendingCheckSum=true;
					}
					else{
						sendingCheckSum=false;
						EtatCourantW=WRITE.WRITING_LENGTH;
					}
				}
			}
		}
			
	}
	
	public void onReadable()
	{		
		// Lecture de la longueur du message
		//System.out.println(this.nioEngine.getName()+" : onReadable()");
		if (EtatCourantR==READ.READING_LENGTH)
		{		
			lenBufferEntree.position(0);
			try {
				channel.read(lenBufferEntree); // A chaque fois que des donnees sont lues dans le buffer, la position de la tete de lecture est modifiee
			} catch (IOException e) {
				System.out.println("ERROR Lecture longueur message");
				recovery_strategy(0);
			} 	
			if (lenBufferEntree.remaining()==0) // Si tout le buffer (les 4 octets) a ete lu
			{
				lenBufferEntree.position(0); // On se place au debut du buffer
				int length = lenBufferEntree.getInt(); // Lecture de 4 octets et formation du int correspondant

				BufferEntree = ByteBuffer.allocate(length); // Allocation du buffer pour recevoir le message
				
				EtatCourantR = READ.READING_MSG;
			}
		}
		
		// Lecture du contenu du message
		if (EtatCourantR==READ.READING_MSG)
		{			
			try {
				channel.read(BufferEntree);
			} catch (IOException e) {
				System.out.println("ERROR Lecture contenu message");
				recovery_strategy(0);
			}
			
			if (BufferEntree.remaining()==0) // Si tout le buffer a ete rempli => le message a ete correctement recu (en entier)
			{
				EtatCourantR = READ.READING_DONE;
				
				lenBufferEntree.position(0); // On se place au debut du buffer
				int length = lenBufferEntree.getInt(); // Lecture de 4 octets et formation du int correspondant
				
				byte[] data = new byte[length];
				BufferEntree.position(0);		
				BufferEntree.get(data, 0, length);				 
				
				if (!readingCheckSum)
				{
					checksum = calculateCheckSum(data);
					EtatCourantR = READ.READING_LENGTH;
					
					temp = ByteBuffer.allocate(length);
					temp.position(0);
					temp = clone(BufferEntree);
					readingCheckSum=true;
				}
				else
				{					
					String s = new String(data);
					String s1 = String.valueOf(checksum);
					
					if(dCallback==null)
						System.out.println("NULL");
					if (s.equals(s1))
						dCallback.deliver(this, temp);	// Le message a ete recu en entier => on le delivre
					readingCheckSum=false;
					EtatCourantR = READ.READING_LENGTH;
				}
			}
		}
	}
	
	
	public static ByteBuffer clone(ByteBuffer original) {
	       ByteBuffer clone = ByteBuffer.allocate(original.capacity());
	       original.rewind();//copy from the beginning
	       clone.put(original);
	       original.rewind();
	       clone.flip();
	       return clone;
	}

	private void recovery_strategy(int i) {
		switch (i)
		{
			case 0 :	// Tentative de reconnexion
				close();
				nioEngine.reconnectAttempt(this.cCallback);
				break;
				
			default: break;
		}
	}
	public void send_pretreatments(String msg)
	{
		// Initialisation des buffers associes a ce channel
		InsertMessage(ByteBuffer.wrap(msg.getBytes()));
		nioEngine.addWriteInterest(channel);		
		EtatCourantW=WRITE.WRITING_LENGTH;	// Pret a ecrire la longueur du message a envoyer si l'evenement OP_WRITE est recu sur ce channel 
	}

	

}
