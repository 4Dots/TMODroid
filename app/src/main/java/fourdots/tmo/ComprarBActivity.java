package fourdots.tmo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class ComprarBActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener
{

	String json;
	String[] tiendas;
	private int selected;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comprar_b);
		json = getIntent().getExtras().getString("json");
	}

	public void onStart()
	{
		super.onStart();
		parseProducts();
		findViewById(R.id.button_send_gc).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_comprar_b, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void parseProducts()
	{
		tiendas = json.split("\",\"");
		tiendas[0] = tiendas[0].substring(2);
		int l = tiendas.length - 1;
		tiendas[l] = tiendas[l].substring(0, l - 2);

		ArrayAdapter<String> aa = new ArrayAdapter<String>(ComprarBActivity.this, R.layout.support_simple_spinner_dropdown_item, tiendas);
		aa.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		Spinner s = (Spinner) findViewById(R.id.spinner_tiendas);
		s.setAdapter(aa);
		s.setOnItemSelectedListener(this);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		selected = position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
		Toast.makeText(getApplicationContext(), "Try again, Twinsen", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onClick(View v)
	{
		final String tienda = tiendas[selected];
		final String valor = ((EditText) findViewById(R.id.txtfield_price)).getText().toString();
		final String email = ((EditText) findViewById(R.id.txtfield_email)).getText().toString();
		Log.e("DBG GCB", "T= " + tienda + " V= " + valor + "E= " + email);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				doHTTPRequest(tienda, email, valor);
			}
		}).start();
	}

	private void doHTTPRequest(String tienda, String email, String valor)
	{
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
		{
			try
			{
				InputStream is = null;
				URL url = new URL("http://tmo.herokuapp.com/?tienda=" + tienda + "&valor=" + valor + "&email=" + email);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000);
				conn.setConnectTimeout(15000);
				conn.setDoInput(true);
				conn.connect();
				int resp = conn.getResponseCode();
				Log.d("DBG", "The response code is:" + resp);
				is = conn.getInputStream();

				String response = getStringBR(new BufferedReader(new InputStreamReader(is)));
				Log.e("DBG SLD", response);
				if (response.equalsIgnoreCase("true"))
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(getApplicationContext(), "Solicitud exitosa, el bono fue enviado", Toast.LENGTH_LONG).show();
							finish();
						}
					});
				}
				else
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(getApplicationContext(), "Solicitud fallida, intente de nuevo", Toast.LENGTH_LONG).show();
						}
					});
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
			}
		}
		else
		{
			Toast.makeText(this, "La solicitud no se puede procesar", Toast.LENGTH_LONG).show();
		}
	}

	private String getStringBR(BufferedReader bufferedReader)
	{
		StringBuilder sb = new StringBuilder();
		String line;
		try
		{
			if (bufferedReader != null)
			{
				while ((line = bufferedReader.readLine()) != null)
				{
					sb.append(line);
				}
				bufferedReader.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return sb.toString();
	}
}
