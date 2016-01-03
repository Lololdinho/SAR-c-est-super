package pingpong.nio;

import java.io.IOException;

import nio.engine.AcceptCallback;
import nio.engine.NioChannel;
import nio.engine.NioServer;

public class MAcceptCallback implements AcceptCallback {

	@SuppressWarnings("static-access")
	@Override
	public void accepted(NioServer arg0, NioChannel arg1) {
		// TODO Auto-generated method stub
		try {
			arg1.getChannel().open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Connection acceptee");
		
	}

	@Override
	public void closed(NioChannel arg0) {
		// TODO Auto-generated method stub
		
		try {
			arg0.getChannel().close();
			System.out.println("Connection fermee");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
