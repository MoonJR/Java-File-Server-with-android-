package com.yeonjukko.openswclient.libs;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public interface ClientMouduleListener {

	public void onConnectServer(Socket server, boolean isEnabledSecurityMode);

	public void onFinishReceivePath(ArrayList<HashMap<String, String>> path);

	public void onReceiving(int persent, int speed);

	public void onFinishedReceiveFile(File receiveFile);

	public void onFailedServerConnect(Exception e);
}
