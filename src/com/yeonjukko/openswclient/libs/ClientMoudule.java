package com.yeonjukko.openswclient.libs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ClientMoudule implements ClientMouduleListener {

	private Key publicKey;
	private SecretKey secureKey;
	private Cipher decryptCipher;

	private String defaultPath;
	private String ip;

	private ClientMouduleListener mClientMouduleListener;

	private int port;

	private Socket client;
	private DataInputStream mDataInputStream;
	private DataOutputStream mDataOutputStream;

	public ClientMoudule(String ip, String defaultPath) {
		this.mClientMouduleListener = this;
		this.defaultPath = defaultPath;
		this.ip = ip;
		this.port = 7050;
	}

	public ClientMoudule(String ip, int port, String defaultPath) {
		this.mClientMouduleListener = this;
		this.defaultPath = defaultPath;
		this.ip = ip;
		this.port = port;
	}

	public void connect() {
		new Thread() {
			@Override
			public void run() {
				try {
					client = new Socket(ip, port);
					mDataInputStream = new DataInputStream(
							client.getInputStream());
					mDataOutputStream = new DataOutputStream(
							client.getOutputStream());
					receiveRSAPublicKey();
					sendSecurekey();
					mClientMouduleListener.onConnectServer(client,
							isEnabledSecurityMode());
				} catch (Exception e) {
					mClientMouduleListener.onFailedServerConnect(e);
				}
			}
		}.start();

	}

	private void receiveRSAPublicKey() throws IOException {
		String publicKeyTmp = mDataInputStream.readUTF();
		if (!publicKeyTmp.equals("null")) {
			try {
				publicKey = KeyFactory.getInstance("RSA").generatePublic(
						new X509EncodedKeySpec(Base64.decode(publicKeyTmp)));
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void createSecureKey() {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(128);
			secureKey = generator.generateKey();
			decryptCipher = Cipher.getInstance("AES");
			decryptCipher.init(Cipher.DECRYPT_MODE, secureKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendSecurekey() throws Exception {
		if (isEnabledSecurityMode()) {

			createSecureKey();
			System.out.println(Base64.encodeBytes(secureKey.getEncoded()));
			Cipher mCipher = null;
			if (System.getProperty("os.name").contains("Linux")) {
				mCipher = Cipher.getInstance("RSA/None/PKCS1Padding");
			} else {
				mCipher = Cipher.getInstance("RSA");
			}
			mCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String test = Base64.encodeBytes(mCipher.doFinal(secureKey
					.getEncoded()));
			mDataOutputStream.writeUTF(test);
			System.out.println(test);

		} else {
			mDataOutputStream.writeUTF("null");
		}

	}

	public boolean isEnabledSecurityMode() {
		if (publicKey == null) {
			return false;
		} else {
			return true;
		}
	}

	public void recevieData(String command) {
		new Receive(command).start();
	}

	public void setOnClientMouduleListener(
			ClientMouduleListener mClientMouduleListener) {
		if (mClientMouduleListener == null) {
			mClientMouduleListener = this;
		} else {
			this.mClientMouduleListener = mClientMouduleListener;
		}
	}

	public static void moveFile(File fromFile, File toFile) {
		fromFile.renameTo(toFile);
	}

	private class Receive extends Thread {

		private String command;

		private Receive(String command) {
			this.command = command;

		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try {
				long prePersent = 0;
				long preTime = System.currentTimeMillis();

				ArrayList<HashMap<String, String>> path = null;

				mDataOutputStream.writeUTF(command);
				FileOutputStream mFileOutputStream = null;
				while (true) {
					JSONObject jsonObjectMain = (JSONObject) JSONValue
							.parse(mDataInputStream.readUTF());

					String type = (String) jsonObjectMain
							.get(ServeReFlag.FLAG_CONTENT_TYPE);
					int length = Integer.parseInt((String) jsonObjectMain
							.get(ServeReFlag.FLAG_CONTENT_LENGTH));
					int available = Integer.parseInt((String) jsonObjectMain
							.get(ServeReFlag.FLAG_CONTENT_AVAILABLE));

					if (type.equals("path")) {
						if (path == null) {
							path = (ArrayList<HashMap<String, String>>) jsonObjectMain
									.get(ServeReFlag.FLAG_CONTENTS);
						} else {
							path.addAll((ArrayList<HashMap<String, String>>) jsonObjectMain
									.get(ServeReFlag.FLAG_CONTENTS));
						}

					} else if (type.equals("file")) {

						if (mFileOutputStream == null) {
							mFileOutputStream = new FileOutputStream(
									defaultPath + "/"
											+ new File(command).getName());
						}

						byte[] data = Base64.decode((String) jsonObjectMain
								.get(ServeReFlag.FLAG_CONTENT));

						mFileOutputStream.write(data);

					} else if (type.equals("file_secure")) {
						if (mFileOutputStream == null) {
							mFileOutputStream = new FileOutputStream(
									defaultPath + "/"
											+ new File(command).getName());
						}
						byte[] data = Base64.decode((String) jsonObjectMain
								.get(ServeReFlag.FLAG_CONTENT));

						mFileOutputStream.write(decryptCipher.doFinal(data));

					}
					long persent = (99 - ((long) available * 100 / length));
					System.out.println(available);
					if (prePersent != persent) {
						prePersent = persent;

						int speed = (int) ((length / 100) / (System
								.currentTimeMillis() - preTime));
						mClientMouduleListener
								.onReceiving((int) persent, speed);
						preTime = System.currentTimeMillis();
					}

					if (available == 0) {
						if (mFileOutputStream != null) {
							mClientMouduleListener
									.onFinishedReceiveFile(new File(defaultPath
											+ "/" + new File(command).getName()));
							mFileOutputStream.close();
						}

						if (path != null) {
							mClientMouduleListener.onFinishReceivePath(path);
							path = null;
						}

						break;
					}

				}

			} catch (Exception e) {
				mClientMouduleListener.onFailedServerConnect(e);
			}

		}
	}

	@Override
	public void onConnectServer(Socket server, boolean isEnabledSecurityMode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFinishedReceiveFile(File receiveFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFailedServerConnect(Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFinishReceivePath(ArrayList<HashMap<String, String>> path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReceiving(int persent, int speed) {
		// TODO Auto-generated method stub

	}
}
