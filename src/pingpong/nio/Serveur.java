package pingpong.nio;

import java.io.IOException;
import java.nio.ByteBuffer;


import nio.engine.AcceptCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioServer;

public class Serveur implements Runnable, AcceptCallback, DeliverCallback{

	private int port;
	
	public Serveur(int p){
		this.port = p;
	}
	

	public void deliver(NioChannel arg0, ByteBuffer arg1) {
		String msg = new String(arg1.array());
		System.out.println("Message recu côté Server: " + msg);
		System.out.println("\n");
		String msgBis = "Coucou Client, j'ai bien reçu ton message";
		((MChannel)arg0).send_pretreatments(msgBis);
	}


	public void accepted(NioServer arg0, NioChannel arg1) {
		System.out.println("Server :::::: AcceptCallback accepted");
		arg1.setDeliverCallback(this);
	}

	
	public void closed(NioChannel arg0) {
		System.out.println("Serveur fermé");
		this.closed(arg0);
	}
	
	public void run() {
		MEngine engine = null;
		System.out.println("Le serveur est lancé");
		try {
			engine = new MEngine();
			engine.setName("Server");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erreur de lancement");
		}
		try {
			engine.listen(port, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		engine.mainloop();		
	}
	
	public static void main(String[] args) {
		
		new Thread(new Serveur(3535)).start();
	}

}
