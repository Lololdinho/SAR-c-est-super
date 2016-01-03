package pingpong.nio;

import nio.engine.ConnectCallback;
import nio.engine.NioChannel;

public class MConnectCallback implements ConnectCallback {

	@Override
	public void closed(NioChannel arg0) {
		// TODO Auto-generated method stub
		System.out.println("Channel closed");
		
	}

	@Override
	public void connected(NioChannel arg0) {
		// TODO Auto-generated method stub
		System.out.println("connection réussie");
		
	}

}
