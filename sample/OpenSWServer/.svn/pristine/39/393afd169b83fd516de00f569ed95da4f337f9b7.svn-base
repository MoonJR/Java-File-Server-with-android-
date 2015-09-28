package com.moonjr.openswserver.activity;

import java.net.Socket;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.moonjr.openswserver.R;
import com.moonjr.openswserver.libs.ServerModule;
import com.moonjr.openswserver.libs.ServerModuleListener;

public class MainActivity extends Activity implements ServerModuleListener,
		OnClickListener {

	private ServerModule module;

	private Button mButtonServerOn, mButtonServerOff;
	private EditText mEditTextPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setLayout();
	}

	private void setLayout() {
		mButtonServerOn = (Button) findViewById(R.id.buttonServerOn);
		mButtonServerOn.setOnClickListener(this);

		mButtonServerOff = (Button) findViewById(R.id.buttonServerOff);
		mButtonServerOff.setOnClickListener(this);

		mEditTextPort = (EditText) findViewById(R.id.editTextPort);

	}

	@Override
	protected void onDestroy() {
		if (module != null) {
			module.stop();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConnectClient(final Socket client) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(
						getApplicationContext(),
						client.getLocalAddress().getHostAddress()
								+ "\n클라이언트 접속", Toast.LENGTH_SHORT).show();

			}
		});
	}

	@Override
	public void onSendingData(int contentSize, int available, String data) {

	}

	@Override
	public void onFinishedSendData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.buttonServerOn:
			if (module != null && module.isStarted()) {
				Toast.makeText(this, "이미" + module.getPort() + "포트로 실행중...",
						Toast.LENGTH_SHORT).show();
			} else {
				try {
					int port = Integer.parseInt(mEditTextPort.getText()
							.toString());

					module = new ServerModule(port, Environment
							.getExternalStorageDirectory().getAbsolutePath());
					module.setOnServerModuleListener(this);
					module.start();
					Toast.makeText(this, module.getPort() + "포트로 서버 정상 시작...",
							Toast.LENGTH_SHORT).show();
				} catch (NumberFormatException e) {
					Toast.makeText(this, "포트번호는 숫자만 입력하세요~", Toast.LENGTH_SHORT)
							.show();
				}
			}
			break;

		case R.id.buttonServerOff:

			if (module != null) {
				module.stop();
				Toast.makeText(this, module.getPort() + "포트 서버 정상 종료",
						Toast.LENGTH_SHORT).show();
				module = null;
			} else {
				Toast.makeText(this, "생성된 서버가 없습니다.", Toast.LENGTH_SHORT)
						.show();
			}

			break;
		}

	}
}
