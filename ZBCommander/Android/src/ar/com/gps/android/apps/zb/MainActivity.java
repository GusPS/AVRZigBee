package ar.com.gps.android.apps.zb;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity
{

	private static final String ACTION_USB_PERMISSION = "ar.com.gps.android.apps.zb.USB_PERMISSION";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		EditText cmdTxt = (EditText) findViewById(R.id.cmdTxt);
		cmdTxt.setOnEditorActionListener(new OnEditorActionListener()
		{
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (event != null && event.getAction() != KeyEvent.ACTION_DOWN)
				{
					return false;
				}

				if (actionId != EditorInfo.IME_ACTION_NEXT && actionId != EditorInfo.IME_NULL)
				{
					return false;
				}

				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				{
					EditText cmdTxt = (EditText) findViewById(R.id.cmdTxt);
					Integer zbAddr = Integer.valueOf(Integer.parseInt(((EditText) findViewById(R.id.nodeID)).getText()
							.toString(), 16));
					new ZBCmdTask().execute(zbAddr, cmdTxt.getText().toString());

					return true;
				}
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);

		return true;
	}

	private class ZBCmdTask extends AsyncTask<Object, Integer, RdoCmd>
	{
		@Override
		protected RdoCmd doInBackground(Object... params)
		{
			Integer zbAddr = (Integer) params[0];
			String cmd = (String) params[1];
			ZBCmd cmdr = new ZBCmd(getApplicationContext(), ACTION_USB_PERMISSION, zbAddr, cmd);
			return cmdr.run(cmd);
		}

		@Override
		protected void onPostExecute(RdoCmd result)
		{
			if (result != null)
				if (result.getResult() == RdoCmd.CMD_OK)
				{
					String prtn = "";
					for (int i = 0; i < result.getData().length; i++)
					{
						if (prtn.length() > 0)
							prtn += "-";
						prtn += String.format("0x%02X", result.getData()[i] & 0xFF);
					}
					Log.d("RDO", prtn);

					TextView cmdHistory = (TextView) findViewById(R.id.cmdHistory);
					cmdHistory.setText(result.getNodeId() + "-" + result.getCmd() + ": " + prtn + "\n"
							+ cmdHistory.getText());
				} else
					Log.d("RDO", "ERROR");
		}
	}

}
