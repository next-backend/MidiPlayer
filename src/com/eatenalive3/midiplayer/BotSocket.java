package com.eatenalive3.midiplayer;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class BotSocket extends WebSocketClient {
	static long serverTimeOffset = 0;
	
	public BotSocket(String roomName) throws URISyntaxException {
		super(new URI("ws://www.multiplayerpiano.com"));

		System.out.println("before connect");

		connect();

		Play.socket = this;

		while (!isOpen()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("after connect");
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					send("[{\"m\":\"t\",\"e\":" + System.currentTimeMillis() + "}]");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();

		send("[{\"m\":\"hi\"}]");
		send("[{\"m\":\"m\",\"x\":0,\"y\":0}]");
		send("[{\"m\":\"ch\",\"_id\":\"" + roomName + "\"}]");
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		System.out.println("handshake");
		System.out.println(handshakedata.getHttpStatusMessage());
	}

	@Override
	public void onMessage(String message) {
		if (message.contains("m\":\"t")) {
			String t = message.substring(message.indexOf("t\":")+3);
			String e = t.substring(t.indexOf("e\":")+3);
			t = t.substring(0, t.indexOf(","));
			e = e.substring(0, e.indexOf("}"));
			//System.out.println(Long.parseLong(t) - Long.parseLong(e));
			serverTimeOffset = Long.parseLong(t) - Long.parseLong(e);
		}
		//MidiPlayer.log(System.currentTimeMillis()+ ": server message: " + message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		MidiPlayer.log("[BUG] NOTIFY BOSS: websocket connection closed: " + reason + ", " + code + ", " + remote);
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	public void sendChat(String msg) {
		MidiPlayer.log(msg);
	}
}