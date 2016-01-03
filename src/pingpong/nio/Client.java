package pingpong.nio;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;

public class Client implements Runnable, ConnectCallback, DeliverCallback {

	private String ip;
	private int port;
	private NioChannel clientChannel;
	
	public Client(String ip,int port){
		this.ip = ip;
		this.port = port;
	}
	
	public void deliver(NioChannel arg0, ByteBuffer arg1) {
		String msg = new String(arg1.array());
		System.out.println("Message recu côté Client: " + msg);
		System.out.println("\n");
		try {
			Thread.sleep(1000);    
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		((MChannel)arg0).send_pretreatments(msg);
	}


	public void closed(NioChannel arg0) {
		System.out.println("Fermeture client");		
	}
	
	public void connected(NioChannel arg0) {
		System.out.println("Client connecté");
		clientChannel = arg0;
		clientChannel.setDeliverCallback(this);
		MChannel channel = ((MChannel)arg0);
		channel.send_pretreatments("Bonjour, c'est le client qui parle");
		
	}

	public void run() {
		System.out.println("Client lancé");
		MEngine engine = null;
		try{
			engine = new MEngine();
			engine.setName("Client");
		}catch (Exception e){
			e.printStackTrace();
			System.out.println("Erreur côté client");
			System.exit(-1);
		}
		try{
			engine.connect(InetAddress.getByName(ip), port, this);
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Erreur connection NioEngine");
			System.exit(-1);
		}
		engine.mainloop();
		
	}
	
	public static void main(String[] args) {
		
		//int id = (int)(Math.random() * ((100) + 1));
		new Thread(new Client("localhost", 3535)).start();
	}

}

