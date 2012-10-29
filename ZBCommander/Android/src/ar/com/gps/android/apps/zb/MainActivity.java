package ar.com.gps.android.apps.zb;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity
{

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
					TextView cmdHistory = (TextView) findViewById(R.id.cmdHistory);
					cmdHistory.setText(cmdTxt.getText() + "\n" + cmdHistory.getText());
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

}
