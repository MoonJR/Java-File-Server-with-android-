package com.moonjr.openswserver.libs;

import java.net.Socket;

public interface ServerModuleListener {

	public void onConnectClient(Socket client); // 클라이언트 접속시 실행

	public void onSendingData(int contentSize, int available, String data); // 데이터 전송시 남은 데이터량을 보여줌

	public void onFinishedSendData(); // 데이터 송신이 끝났을때 실행
}
