package ar.com.gps.android.apps.zbasp;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;

import java.io.File;
import java.util.List;

import org.xjava.delegates.MethodDelegate;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity
{

	static final String TAG = MainActivity.class.getSimpleName();
	private static final int _ReqChooseFile = 0;
	private static final String ACTION_USB_PERMISSION = "ar.com.gps.android.apps.avrdudroid.USB_PERMISSION";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.fileselec).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
				intent.putExtra(FileChooserActivity._RegexFilenameFilter, ".*\\.(hex)$");
				startActivityForResult(intent, _ReqChooseFile);
			}
		});

		findViewById(R.id.startOTA).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Integer zbAddr = null;
				File hexFile = null;
				try
				{
					zbAddr = Integer.valueOf(Integer.parseInt(((EditText) findViewById(R.id.zbaddr)).getText()
							.toString(), 16));

					String filename = ((EditText) findViewById(R.id.hexfile)).getText().toString();
					hexFile = new File(filename);
					if (!hexFile.exists() || "".equals(filename))
					{
						Toast.makeText(getApplicationContext(), "El archivo especificado no puede leerse.",
								Toast.LENGTH_SHORT).show();
						hexFile = null;
					}
				} catch (NumberFormatException nfe)
				{
					Toast.makeText(getApplicationContext(), "Debe especificarse la dirección del Nodo ZigBee.",
							Toast.LENGTH_SHORT).show();
				}

				if (zbAddr != null && hexFile != null)
					new OTATask().execute(zbAddr, hexFile);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case _ReqChooseFile:
				if (resultCode == RESULT_OK)
				{
					List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
					((EditText) findViewById(R.id.hexfile)).setText(files.get(0).getAbsolutePath());
				}
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private class OTATask extends AsyncTask<Object, Integer, Integer>
	{
		@SuppressWarnings("unused")
		public void pubProgress(Integer p)
		{
			publishProgress(p);
		}

		@Override
		protected Integer doInBackground(Object... params)
		{
			Integer zbAddr = (Integer) params[0];
			File hexFile = (File) params[1];
			OTAProgrammer otap = new OTAProgrammer(getApplicationContext(), ACTION_USB_PERMISSION, zbAddr, hexFile,
					((ProgressBar) findViewById(R.id.progressBar)));
			otap.setPublishProgress(MethodDelegate.createWithArgs(this, "pubProgress"));
			return otap.program();
		}

		@Override
		protected void onProgressUpdate(Integer... progress)
		{
			((ProgressBar) findViewById(R.id.progressBar)).setProgress(progress[0].intValue());
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (result.equals(0))
				Toast.makeText(getApplicationContext(), "OTA programming finished ok.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "OTA programming error:" + result, Toast.LENGTH_SHORT).show();
			((ProgressBar) findViewById(R.id.progressBar)).setProgress(0);
		}
	}
}
