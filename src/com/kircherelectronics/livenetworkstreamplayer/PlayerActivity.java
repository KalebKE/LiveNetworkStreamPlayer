package com.kircherelectronics.livenetworkstreamplayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.util.DebugTextViewHelper;
import com.google.android.exoplayer.util.Util;
import com.kircherelectronics.livenetworkstreamplayer.player.Player;
import com.kircherelectronics.livenetworkstreamplayer.player.Player.RendererBuilder;
import com.kircherelectronics.livenetworkstreamplayer.player.UdpExtractorRendererBuilder;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback,
		OnClickListener, Player.Listener
{

	private boolean playerNeedsPrepare;

	private long playerPosition;

	private AspectRatioFrameLayout videoFrame;

	private Button retryButton;

	private DebugTextViewHelper debugViewHelper;

	private EventLogger eventLogger;

	private Player player;

	private SurfaceView surfaceView;
	private TextView debugTextView;
	private TextView playerStateTextView;

	private View debugRootView;
	private View shutterView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.player_activity);

		shutterView = findViewById(R.id.shutter);
		debugRootView = findViewById(R.id.controls_root);

		videoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
		surfaceView = (SurfaceView) findViewById(R.id.surface_view);
		surfaceView.getHolder().addCallback(this);
		debugTextView = (TextView) findViewById(R.id.debug_text_view);

		playerStateTextView = (TextView) findViewById(R.id.player_state_view);

		retryButton = (Button) findViewById(R.id.retry_button);
		retryButton.setOnClickListener(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		releasePlayer();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		releasePlayer();

		shutterView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		preparePlayer();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		if (player != null)
		{
			player.setSurface(holder.getSurface());
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height)
	{
		// Do nothing.
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		if (player != null)
		{
			player.blockingClearSurface();
		}
	}

	@Override
	public void onClick(View view)
	{
		if (view == retryButton)
		{
			preparePlayer();
		}
	}

	@Override
	public void onStateChanged(boolean playWhenReady, int playbackState)
	{
		String text = "playWhenReady=" + playWhenReady + ", playbackState=";
		switch (playbackState)
		{
		case ExoPlayer.STATE_BUFFERING:
			text += "buffering";
			break;
		case ExoPlayer.STATE_ENDED:
			text += "ended";
			break;
		case ExoPlayer.STATE_IDLE:
			text += "idle";
			break;
		case ExoPlayer.STATE_PREPARING:
			text += "preparing";
			break;
		case ExoPlayer.STATE_READY:
			text += "ready";
			break;
		default:
			text += "unknown";
			break;
		}
		playerStateTextView.setText(text);
	}

	@Override
	public void onError(Exception e)
	{
		playerNeedsPrepare = true;
	}

	@Override
	public void onVideoSizeChanged(int width, int height,
			float pixelWidthAspectRatio)
	{
		shutterView.setVisibility(View.GONE);
		videoFrame.setAspectRatio(height == 0 ? 1
				: (width * pixelWidthAspectRatio) / height);
	}

	private RendererBuilder getRendererBuilder()
	{
		String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");

		return new UdpExtractorRendererBuilder(this, userAgent, Uri.parse(""));
	}

	private void preparePlayer()
	{
		if (player == null)
		{
			player = new Player(getRendererBuilder());
			player.addListener(this);
			player.seekTo(playerPosition);
			playerNeedsPrepare = true;
			eventLogger = new EventLogger();
			eventLogger.startSession();
			player.addListener(eventLogger);
			player.setInfoListener(eventLogger);
			player.setInternalErrorListener(eventLogger);
			debugViewHelper = new DebugTextViewHelper(player, debugTextView);
			debugViewHelper.start();
		}
		if (playerNeedsPrepare)
		{
			player.prepare();
			playerNeedsPrepare = false;
		}
		player.setSurface(surfaceView.getHolder().getSurface());
		player.setPlayWhenReady(true);
	}

	private void releasePlayer()
	{
		if (player != null)
		{
			debugViewHelper.stop();
			debugViewHelper = null;
			playerPosition = player.getCurrentPosition();
			player.release();
			player = null;
			eventLogger.endSession();
			eventLogger = null;
		}
	}

}
