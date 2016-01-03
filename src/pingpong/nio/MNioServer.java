package pingpong.nio;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

import nio.engine.AcceptCallback;
import nio.engine.NioServer;

public class MNioServer extends NioServer{
	private int port;
	private ServerSocketChannel sChannel;
	private AcceptCallback aCallback;
	
	public MNioServer(ServerSocketChannel serverChannel, int port, AcceptCallback cb)
	{
		this.sChannel=serverChannel;
		this.port=port;
		this.aCallback = cb;
	}


	@Override
	public void close() {
		try {
			sChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public int getPort() {		
		return port;
	}
	
	public AcceptCallback getAcceptCallback(){
		return this.aCallback;
	}

}
