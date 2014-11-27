package fourdots.tmo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;


public class GCActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

	private static final int RC_SIGN_IN = 0;
	private GoogleApiClient mGoogleApiClient;
	private boolean mIntentInProgress;
	String accessToken;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gc);

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_LOGIN)
				.build();
	}

	public void onStart()
	{
		super.onStart();
		mGoogleApiClient.connect();
		findViewById(R.id.button_logout).setOnClickListener(this);
	}

	public void onStop()
	{
		super.onStop();
		if (mGoogleApiClient.isConnected())
		{
			mGoogleApiClient.disconnect();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_gc, menu);
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

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.button_logout)
		{
			if (mGoogleApiClient.isConnected())
			{
				Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
				Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
						.setResultCallback(new ResultCallback<Status>()
						{

							@Override
							public void onResult(Status status)
							{
								onStop();
							}
						});
			}
			else
			{
				onStop();
			}
		}
	}

	@Override
	public void onConnected(Bundle bundle)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				showLikes();
				while (accessToken == null || accessToken == "")
				{
					try
					{
						getServToken();
						synchronized (this)
						{
							wait(15000);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				doHTTPRequest(accessToken);
			}
		}).start();
	}

	@Override
	public void onConnectionSuspended(int i)
	{
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result)
	{
		if (!mIntentInProgress && result.hasResolution())
		{
			try
			{
				mIntentInProgress = true;
				startIntentSenderForResult(result.getResolution().getIntentSender(),
						RC_SIGN_IN, null, 0, 0, 0);
			}
			catch (IntentSender.SendIntentException e)
			{
				// The intent was canceled before it was sent.  Return to the default
				// state and attempt to connect to get an updated ConnectionResult.
				mIntentInProgress = false;
				mGoogleApiClient.connect();
			}
		}
	}

	protected void onActivityResult(int requestCode, int responseCode, Intent intent)
	{
		if (requestCode == RC_SIGN_IN)
		{
			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting())
			{
				mGoogleApiClient.connect();
			}
		}
	}

	private void showLikes()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				RelativeLayout mLayout = (RelativeLayout) findViewById(R.id.rel_layout);
				mLayout.addView(createNewTextView(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getUrl()));
			}
		});
	}

	private View createNewTextView(String t)
	{
		final TextView textView = new TextView(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.BELOW, R.id.string_username);
		textView.setLayoutParams(params);
		textView.setText(t);
		return textView;
	}

	private String getServToken()
	{
		accessToken = null;
		Bundle appActivities = new Bundle();
		appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, "GCActivity");
		String scopes = "https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/plus.me https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";
		try
		{
			Log.e("ServTKN", "PRE TKN RQT");
			accessToken = GoogleAuthUtil.getToken
			(
					this,
					Plus.AccountApi.getAccountName(mGoogleApiClient),
					"oauth2:server:client_id:885039176328-ngt3bk080t47iv2firvl2a9qr3dvj9si.apps.googleusercontent.com:api_scope:" + scopes
			);
			Log.e("ServTKN", "POST TKN RQT");
			Log.e("ServTKN", accessToken);
		}
		catch (IOException transientEx)
		{
			// network or server error, the call is expected to succeed if you try again later.
			// Don't attempt to call again immediately - the request is likely to
			// fail, you'll hit quotas or back-off.
			throw new RuntimeException(transientEx);
		}
		catch (UserRecoverableAuthException e)
		{
			// Requesting an authorization code will always throw
			// UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
			// because the user must consent to offline access to their data.  After
			// consent is granted control is returned to your activity in onActivityResult
			// and the second call to GoogleAuthUtil.getToken will succeed.
			Log.e("Token", "UR Token Exception", e);
			startActivityForResult(e.getIntent(), RC_SIGN_IN);
		}
		catch (GoogleAuthException authEx)
		{
			Log.e("Token", "GAuthToken Exception", authEx);
			throw new RuntimeException(authEx);
		}
		catch (Exception e)
		{
			Log.e("Token", "Token Exception", e);
			throw new RuntimeException(e);
		}
		return accessToken;
	}

	private void doHTTPRequest(String tokenURL)
	{
		ConnectivityManager connMgr = (ConnectivityManager)	getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
		{
			try
			{
				InputStream is = null;
				URL url = new URL("http://tmo.herokuapp.com/?oauth=" + tokenURL);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000);
				conn.setConnectTimeout(15000);
				conn.setDoInput(true);
				conn.connect();
				int resp = conn.getResponseCode();
				Log.d("DBG","The response code is:" + resp);
				is = conn.getInputStream();

				String response = getStringBR(new BufferedReader(new InputStreamReader(is)));
				Log.e("DBG TKN",response);
				Intent i = new Intent(this,ComprarBActivity.class);
				i.putExtra("json",response);
				startActivity(i);
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
