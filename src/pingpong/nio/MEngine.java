package pingpong.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Hashtable;
import java.util.Iterator;
import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioEngine;
import nio.engine.NioServer;

public class MEngine extends NioEngine {
	
	private Selector selector; 
	private String name;
	private InetAddress remoteAdress;
	private int remotePort;
	private Hashtable<ServerSocketChannel, MNioServer> servers;
	private Hashtable<SocketChannel, MChannel> channels;	// 

	public MEngine() throws Exception {
		super();
		selector = SelectorProvider.provider().openSelector();
		channels = new Hashtable<SocketChannel, MChannel>();
		servers = new Hashtable<ServerSocketChannel,MNioServer>();
	}

	@Override
	public void connect(InetAddress arg0, int arg1, ConnectCallback arg2)
			throws UnknownHostException, SecurityException, IOException {
		this.remoteAdress = arg0;
		this.remotePort = arg1;
		
		// Creation SocketChannel non bloquant
		System.out.println("SocketChannel etablie");
		SocketChannel clientChannel = SocketChannel.open();
		clientChannel.configureBlocking(false);

		// Enregistrement de l'interet pour les evenements de type CONNECT
		clientChannel.register(selector, SelectionKey.OP_CONNECT); 
		clientChannel.connect(new InetSocketAddress(arg0, arg1));
		
		
		MChannel niochan = new MChannel(this, clientChannel, arg2);
		channels.put(clientChannel, niochan);
		
	}

	@Override
public NioServer listen(int arg0, AcceptCallback arg1) throws IOException {		
		// Creation serverSocketChannel non bloquant
		ServerSocketChannel serverChannel = ServerSocketChannel.open(); 
		serverChannel.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress(arg0);
		serverChannel.socket().bind(isa);
		// Enregistrement de l'interet pour les evenements de type ACCEPT
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);		
		MNioServer server = new MNioServer(serverChannel, arg0, arg1);
		servers.put(serverChannel, server);
		System.out.println("En attente du client");
		return server;
	}

	@Override
	public void mainloop() {
		System.out.println("On est dans la mainloop");
		while (true) {
//			System.out.println("LOOP");
			try {
				selector.select();
				Iterator<?> selectedKeys = selector.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					
					if (key.isAcceptable()){ 
						SocketChannel socketChannel = null;
						ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel(); 
						
						try {
							socketChannel = serverSocketChannel.accept();
							socketChannel.configureBlocking(false);
						} 
						catch (IOException e) {
							
						}
						try {
							socketChannel.register(selector, SelectionKey.OP_READ);
						} catch (ClosedChannelException e) {
							
						}
						
						AcceptCallback acb = servers.get(serverSocketChannel).getAcceptCallback();
						MChannel channel = new MChannel(this,socketChannel);
						
						channels.put(socketChannel, channel);		
						acb.accepted(servers.get(serverSocketChannel), channel);
					}
					
					else if (key.isReadable()){ 
						channels.get((SocketChannel) key.channel()).onReadable();
					}
					
					else if (key.isWritable()) {
						channels.get((SocketChannel) key.channel()).onWritable();
					}
					
					else if (key.isConnectable()){
						SocketChannel socketChannel = (SocketChannel) key.channel();	// Recuperation du channel concerne par key

						boolean error=false;
						try {
							socketChannel.finishConnect();
						} catch (IOException e) { // Suppression automatique de l'interet pour CONNECT (il faut le remettre)
							error=true;
							
							reconnectAttempt(channels.get(socketChannel).getConnectCallback());
						}
						
						if (!error)
						{
							// Enregistrement de notre interet pour les evenements de type READ (ne jamais l'enlever!)
							key.interestOps(SelectionKey.OP_READ);
							
							// On averti notre callback que l'on vient de se connecter
							channels.get(socketChannel).connected();
						}
					}
					
					else{ 
						System.out.println("Key non reconnue");
					}
					selectedKeys.remove();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}

	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
	public void addWriteInterest(SocketChannel channel){
		SelectionKey key = channel.keyFor(selector);
		
		// Enregistrement de notre interet pour les evenements de type WRITE en plus du type READ
		// Un evenement de type WRITE sera emis lorsque notre message sera pret a etre envoye
		key.interestOps(SelectionKey.OP_WRITE);
		
		selector.wakeup();
	}
	
	public void removeChannelFromSelector(SocketChannel channel)
	{
		channel.keyFor(selector).cancel();
	}
	
	public void reconnectAttempt(ConnectCallback cb)
	{	
		System.out.println("Tentative de reconnexion...");
		try {
			connect(remoteAdress,remotePort,cb);
		} catch (Exception e) {
			//exit
		}

	}

	
	public void removeWriteInterest(SocketChannel channel)
	{
		SelectionKey key = channel.keyFor(selector);
		
		// Enregistrement de notre interet uniquement pour les evenements de type READ
		// Un evenement de type READ sera emis lorsque des donnees auront ete recues sur le canal
		key.interestOps(SelectionKey.OP_READ);
	}

}
