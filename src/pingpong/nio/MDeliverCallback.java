package pingpong.nio;

import java.nio.ByteBuffer;

import nio.engine.DeliverCallback;
import nio.engine.NioChannel;

public class MDeliverCallback implements DeliverCallback{

	@Override
	public void deliver(NioChannel arg0, ByteBuffer arg1) {
		// TODO Auto-generated method stub
		System.out.println("Message reçu entierement : " + arg1);
		
	}

}
