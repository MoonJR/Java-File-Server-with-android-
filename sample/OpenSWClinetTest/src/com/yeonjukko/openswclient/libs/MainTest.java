package com.yeonjukko.openswclient.libs;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class MainTest {

	public static void main(String[] args) {

		ClientMoudule clientMoudule = new ClientMoudule("서버 아이피", 7777,
				"파일 다운로드 경로"); //7777 포
		clientMoudule.setOnClientMouduleListener(new ClientMouduleListener() {

			@Override
			public void onReceiving(int persent, int speed) {
				System.out.println("speed:" + speed + "k" + " persent:"
						+ persent + "%");
			}

			@Override
			public void onFinishReceivePath(
					ArrayList<HashMap<String, String>> path) {
				for (HashMap<String, String> map : path) {
					System.out.println(map.get("content"));
				}

				clientMoudule.recevieData(JOptionPane.showInputDialog(null));
			}

			@Override
			public void onFinishedReceiveFile(File receiveFile) {
				JFileChooser chooser = new JFileChooser();
				chooser.showOpenDialog(null);
				ClientMoudule.moveFile(receiveFile, chooser.getSelectedFile());

				clientMoudule.recevieData(JOptionPane.showInputDialog(null));
			}

			@Override
			public void onFailedServerConnect(Exception e) {
				e.printStackTrace();
				System.out.println("서버와 접속이 끊어졌습니다.");
			}

			@Override
			public void onConnectServer(Socket server,
					boolean isEnabledSecurityMode) {
				System.out.println("서버와 연결에 성공하였습니다.");

				if (isEnabledSecurityMode) {
					System.out.println("암호화 모드 사용 가능!");
				} else {
					System.out.println("암호화 모드 실패");
				}

				clientMoudule.recevieData(JOptionPane.showInputDialog(null));
			}
		});

		clientMoudule.connect();

	}
}
