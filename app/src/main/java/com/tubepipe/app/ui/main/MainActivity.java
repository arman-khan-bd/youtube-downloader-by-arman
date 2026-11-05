package com.tubepipe.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tubepipe.app.R;
import com.tubepipe.app.data.player.BackgroundAudioService;
import com.tubepipe.app.ui.fragments.DownloadsFragment;
import com.tubepipe.app.ui.fragments.HomeFragment;
import com.tubepipe.app.ui.fragments.SearchFragment;
import com.tubepipe.app.ui.player.AudioPlayerBottomSheet;
import com.tubepipe.app.ui.settings.SettingsActivity;
import com.tubepipe.app.ui.share.ShareReceiverActivity;
import com.tubepipe.app.util.PermissionUtils;

public class MainActivity extends AppCompatActivity implements BackgroundAudioService.AudioPlaybackListener {

    private BottomNavigationView bottomNav;
    private final HomeFragment homeFragment = new HomeFragment();
    private final SearchFragment searchFragment = new SearchFragment();
    private final DownloadsFragment downloadsFragment = new DownloadsFragment();
    private Fragment activeFragment = homeFragment;

    // Mini-Player Views
    private View layoutMiniPlayer;
    private ImageView ivMiniThumb;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        bottomNav = findViewById(R.id.bottom_navigation);

        initMiniPlayer();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, downloadsFragment, "downloads").hide(downloadsFragment)
                .add(R.id.fragment_container, searchFragment, "search").hide(searchFragment)
                .add(R.id.fragment_container, homeFragment, "home")
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_search) {
                switchFragment(searchFragment);
                return true;
            } else if (itemId == R.id.nav_downloads) {
                switchFragment(downloadsFragment);
                return true;
            }
            return false;
        });

        // Request storage/notification permissions if not yet granted
        if (!PermissionUtils.hasStoragePermission(this)) {
            PermissionUtils.requestStoragePermission(this);
        }

        BackgroundAudioService.addListener(this);

        handleExternalShareIntent(getIntent());
    }

    private void initMiniPlayer() {
        layoutMiniPlayer = findViewById(R.id.layout_mini_player);
        ivMiniThumb = findViewById(R.id.iv_mini_thumb);
        tvMiniTitle = findViewById(R.id.tv_mini_title);
        tvMiniArtist = findViewById(R.id.tv_mini_artist);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        btnMiniClose = findViewById(R.id.btn_mini_close);

        layoutMiniPlayer.setOnClickListener(v -> {
            AudioPlayerBottomSheet.newInstance().show(getSupportFragmentManager(), "main_audio_sheet");
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackgroundAudioService.class);
            intent.setAction(BackgroundAudioService.ACTION_TOGGLE);
            startService(intent);
        });

        btnMiniClose.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackgroundAudioService.class);
            intent.setAction(BackgroundAudioService.ACTION_STOP);
            startService(intent);
            layoutMiniPlayer.setVisibility(View.GONE);
        });

        updateMiniPlayerState();
    }

    private void updateMiniPlayerState() {
        BackgroundAudioService service = BackgroundAudioService.getInstance();
        if (service != null && service.isPlaying()) {
            layoutMiniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(service.getCurrentTitle());
            tvMiniArtist.setText(service.getCurrentArtist());
            btnMiniPlayPause.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            Glide.with(this)
                    .load(service.getCurrentArtUrl())
                    .placeholder(R.drawable.bg_thumbnail_placeholder)
                    .into(ivMiniThumb);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayerState();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        layoutMiniPlayer.setVisibility(View.VISIBLE);
        btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void onTrackChanged(String title, String artist, String artUrl) {
        layoutMiniPlayer.setVisibility(View.VISIBLE);
        tvMiniTitle.setText(title);
        tvMiniArtist.setText(artist);
        Glide.with(this).load(artUrl).placeholder(R.drawable.bg_thumbnail_placeholder).into(ivMiniThumb);
    }

    @Override
    public void onProgress(long positionMs, long durationMs) {}

    @Override
    public void onStopped() {
        layoutMiniPlayer.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BackgroundAudioService.removeListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleExternalShareIntent(intent);
    }

    private void handleExternalShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Intent forward = new Intent(this, ShareReceiverActivity.class);
            forward.setAction(action);
            forward.setType(intent.getType());
            forward.setData(intent.getData());
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                forward.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT));
            }
            startActivity(forward);
        }
    }

    private void switchFragment(Fragment target) {
        if (activeFragment != target) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .commit();
            activeFragment = target;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            bottomNav.setSelectedItemId(R.id.nav_search);
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
