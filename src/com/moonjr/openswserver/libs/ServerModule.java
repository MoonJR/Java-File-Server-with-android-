package com.moonjr.openswserver.libs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ServerModule implements ServerModuleListener {

	private static final int RSA_KEY_LENGTH = 1024;

	public static final int BUFFER_SIZE = 32768;

	private Set<SendThread> clinetThreadSet;

	private String defaultPath;

	private ServerSocket server;
	private ServerModuleListener mServerModuleListener;
	private int port;

	private Key publicKey, privateKey;

	public ServerModule(String defaultPath) {
		this.defaultPath = defaultPath;
		this.mServerModuleListener = this;
		this.clinetThreadSet = new HashSet<SendThread>();
		port = 7050;
	}

	public ServerModule(int port, String defaultPath) {
		this.defaultPath = defaultPath;
		this.mServerModuleListener = this;
		this.clinetThreadSet = new HashSet<SendThread>();
		this.port = port;
	}

	public void start() {
		new Thread() {
			@Override
			public void run() {
				try {
					createRSAKey();
					server = new ServerSocket(port);
					while (true) {
						Socket client = server.accept();
						SendThread sendThread = new SendThread(client);
						clinetThreadSet.add(sendThread);
						sendThread.start();
						mServerModuleListener.onConnectClient(client);
					}

				} catch (Exception e) {
					if (server != null) {
						try {
							server.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					e.printStackTrace();
				}
			}
		}.start();

	}

	public void stop() {

		for (SendThread clinetThread : clinetThreadSet) {
			if (clinetThread != null) {
				if (clinetThread.client != null) {
					try {
						clinetThread.client.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (clinetThread.mDataInputStream != null) {

					try {
						clinetThread.mDataInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (clinetThread.mDataOutputStream != null) {
					try {
						clinetThread.mDataOutputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		if (server != null) {
			try {
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isStarted() {
		if (server != null) {
			return !server.isClosed();
		} else {
			return false;
		}
	}

	public void setOnServerModuleListener(ServerModuleListener mServerModuleListener) {

		if (mServerModuleListener == null) {
			mServerModuleListener = this;
		} else {
			this.mServerModuleListener = mServerModuleListener;
		}
	}

	private void createRSAKey() throws NoSuchAlgorithmException {
		KeyPairGenerator clsKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
		clsKeyPairGenerator.initialize(RSA_KEY_LENGTH);

		KeyPair clsKeyPair = clsKeyPairGenerator.genKeyPair();
		publicKey = clsKeyPair.getPublic();
		privateKey = clsKeyPair.getPrivate();
	}

	private class SendThread extends Thread {
		private SecretKey secureKey;

		private Socket client;

		private DataInputStream mDataInputStream;
		private DataOutputStream mDataOutputStream;

		private Cipher encryptCipher;

		private SendThread(Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			try {
				mDataInputStream = new DataInputStream(client.getInputStream());
				mDataOutputStream = new DataOutputStream(client.getOutputStream());
				sendRSAPublicKey();
				receiveSecurekey();
				while (true) {

					String path = mDataInputStream.readUTF();
					System.out.println(path);
					if (path.contains(ServeReFlag.FLAG_PATH)) {
						path = path.replace(ServeReFlag.FLAG_PATH, "");

						if (path.equals("")) {
							path = defaultPath;
						}

						sendPath(path);

					} else if (path.contains(ServeReFlag.FLAG_DOWNLOAD)) {
						path = path.replace(ServeReFlag.FLAG_DOWNLOAD, "");
						sendFile(path);
					} else if (path.contains(ServeReFlag.FlAG_DOWNLOAD_SECURE)) {
						path = path.replace(ServeReFlag.FlAG_DOWNLOAD_SECURE, "");
						sendFileSecure(path);
					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					client.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					mDataInputStream.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					mDataOutputStream.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		// @SuppressWarnings("unchecked")
		// private void sendPath(String path) throws Exception {
		// File[] childFile = new File(path).listFiles();
		// JSONObject jsonObjectMain = new JSONObject();
		// jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_TYPE, "path");
		// jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_LENGTH,
		// childFile.length + "");
		// jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_AVAILABLE, 0 + "");
		// JSONArray jsonArrayContents = new JSONArray();
		//
		// for (File file : childFile) {
		// JSONObject jsonObjectFile = new JSONObject();
		// jsonObjectFile.put(ServeReFlag.FLAG_CONTENT,
		// file.getAbsolutePath());
		// if (file.isDirectory()) {
		// jsonObjectFile.put(ServeReFlag.FLAG_TYPE, "directory");
		// } else {
		// jsonObjectFile.put(ServeReFlag.FLAG_TYPE, "file");
		// }
		// jsonArrayContents.add(jsonObjectFile);
		// }
		//
		// jsonObjectMain.put(ServeReFlag.FLAG_CONTENTS, jsonArrayContents);
		//
		// String sendData = jsonObjectMain.toJSONString();
		//
		// mDataOutputStream.writeUTF(sendData);
		// mServerModuleListener.onSendingData(childFile.length, 0, sendData);
		// mServerModuleListener.onFinishedSendData();
		// }

		@SuppressWarnings("unchecked")
		private void sendPath(String path) throws Exception {
			File[] childFile = new File(path).listFiles();
			int totalFileSize = childFile.length;
			int nowFileIndex = 0;

			while (true) {
				JSONObject jsonObjectMain = new JSONObject();
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_TYPE, "path");
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_LENGTH, totalFileSize + "");
				JSONArray jsonArrayContents = new JSONArray();

				int j = 0;

				for (int i = nowFileIndex; i < nowFileIndex + 300; i++, j++) {
					if (totalFileSize <= i) {
						break;
					}

					JSONObject jsonObjectFile = new JSONObject();
					jsonObjectFile.put(ServeReFlag.FLAG_CONTENT, childFile[i].getAbsolutePath());
					if (childFile[i].isDirectory()) {
						jsonObjectFile.put(ServeReFlag.FLAG_TYPE, "directory");
					} else {
						jsonObjectFile.put(ServeReFlag.FLAG_TYPE, "file");
					}
					jsonArrayContents.add(jsonObjectFile);
				}

				nowFileIndex += j;

				int available = totalFileSize - nowFileIndex;

				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_AVAILABLE, available + "");
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENTS, jsonArrayContents);

				String sendData = jsonObjectMain.toJSONString();

				mDataOutputStream.writeUTF(sendData);
				mServerModuleListener.onSendingData(childFile.length, available, sendData);
				if (available <= 0) {
					break;
				}

			}

			mServerModuleListener.onFinishedSendData();
		}

		@SuppressWarnings("unchecked")
		private void sendFile(String path) throws Exception {
			byte[] buffer = new byte[BUFFER_SIZE];

			FileInputStream fileInput = null;

			fileInput = new FileInputStream(path);

			int contentSize = fileInput.available();
			try {
				while (fileInput.read(buffer) != -1) {
					int contentAvailable = fileInput.available();
					JSONObject jsonObjectMain = new JSONObject();
					jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_TYPE, "file");
					jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_LENGTH, contentSize + "");
					jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_AVAILABLE, contentAvailable + "");
					jsonObjectMain.put(ServeReFlag.FLAG_CONTENT, Base64.encodeBytes(buffer));

					String sendData = jsonObjectMain.toJSONString();
					mDataOutputStream.writeUTF(sendData);
					mServerModuleListener.onSendingData(contentSize, contentAvailable, sendData);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mServerModuleListener.onFinishedSendData();
		}

		@SuppressWarnings("unchecked")
		private void sendFileSecure(String path) throws Exception {
			byte[] buffer = new byte[BUFFER_SIZE];

			FileInputStream fileInput = null;

			fileInput = new FileInputStream(path);

			int contentSize = fileInput.available();

			while (fileInput.read(buffer) != -1) {
				int contentAvailable = fileInput.available();

				JSONObject jsonObjectMain = new JSONObject();
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_TYPE, "file_secure");
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_LENGTH, contentSize + "");
				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT_AVAILABLE, contentAvailable + "");

				Cipher mCipher = Cipher.getInstance("AES");

				mCipher.init(Cipher.ENCRYPT_MODE, secureKey);

				jsonObjectMain.put(ServeReFlag.FLAG_CONTENT, Base64.encodeBytes(mCipher.doFinal(buffer)));
				String sendData = jsonObjectMain.toJSONString();

				mDataOutputStream.writeUTF(sendData);
				mServerModuleListener.onSendingData(contentSize, contentAvailable, sendData);
			}

			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mServerModuleListener.onFinishedSendData();
		}

		private void sendRSAPublicKey() throws IOException {

			if (publicKey == null) {
				mDataOutputStream.writeUTF("null");
			} else {
				mDataOutputStream.writeUTF(Base64.encodeBytes(publicKey.getEncoded()));
			}
		}

		private void receiveSecurekey() throws IOException {
			String securekeyTmp = mDataInputStream.readUTF();
			if (!securekeyTmp.equals("null")) {
				try {
					Cipher mCipher = null;
					if (System.getProperty("os.name").contains("Linux")) {
						mCipher = Cipher.getInstance("RSA/None/PKCS1Padding");
					} else {
						mCipher = Cipher.getInstance("RSA");
					}

					mCipher.init(Cipher.DECRYPT_MODE, privateKey);
					byte[] secureKeyBuffer = mCipher.doFinal(Base64.decode(securekeyTmp));
					secureKey = new SecretKeySpec(secureKeyBuffer, 0, secureKeyBuffer.length, "AES");

					this.encryptCipher = Cipher.getInstance("AES");
					this.encryptCipher.init(Cipher.ENCRYPT_MODE, secureKey);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public void onConnectClient(Socket client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSendingData(int contentSize, int available, String data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFinishedSendData() {
		// TODO Auto-generated method stub

	}

}
