package fourdots.tmo;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;


public class GCActivity extends Activity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

	private static final int RC_SIGN_IN = 0;
	private GoogleApiClient mGoogleApiClient;
	private boolean mIntentInProgress;

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
			finish();
		}
	}

	@Override
	public void onConnected(Bundle bundle)
	{
		TextView tv = (TextView) findViewById(R.id.string_username);
		tv.setText(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getDisplayName());
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
			} catch (IntentSender.SendIntentException e)
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
}
