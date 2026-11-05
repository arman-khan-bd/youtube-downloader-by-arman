package com.tubepipe.app.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.tubepipe.app.R;
import com.tubepipe.app.data.player.BackgroundAudioService;
import com.tubepipe.app.util.AppUtils;

public class AudioPlayerBottomSheet extends BottomSheetDialogFragment implements BackgroundAudioService.AudioPlaybackListener {

    private ImageView ivArt;
    private TextView tvTitle;
    private TextView tvArtist;
    private SeekBar seekBar;
    private TextView tvPosition;
    private TextView tvDuration;
    private MaterialCardView cardPlayPause;
    private ImageView ivPlayPause;
    private ImageButton btnRewind;
    private ImageButton btnForward;

    private boolean isUserSeeking = false;

    public static AudioPlayerBottomSheet newInstance() {
        return new AudioPlayerBottomSheet();
    }

    @Override
    public int getTheme() {
        return R.style.Theme_TubePipe_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_audio_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivArt = view.findViewById(R.id.iv_audio_large_art);
        tvTitle = view.findViewById(R.id.tv_audio_title);
        tvArtist = view.findViewById(R.id.tv_audio_artist);
        seekBar = view.findViewById(R.id.audio_seek_bar);
        tvPosition = view.findViewById(R.id.tv_audio_position);
        tvDuration = view.findViewById(R.id.tv_audio_duration);
        cardPlayPause = view.findViewById(R.id.card_audio_play_pause);
        ivPlayPause = view.findViewById(R.id.iv_audio_play_pause);
        btnRewind = view.findViewById(R.id.btn_audio_rewind);
        btnForward = view.findViewById(R.id.btn_audio_forward);

        BackgroundAudioService service = BackgroundAudioService.getInstance();
        if (service != null) {
            tvTitle.setText(service.getCurrentTitle());
            tvArtist.setText(service.getCurrentArtist());
            ivPlayPause.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

            Glide.with(this)
                    .load(service.getCurrentArtUrl())
                    .placeholder(R.drawable.bg_thumbnail_placeholder)
                    .into(ivArt);

            long pos = service.getCurrentPosition();
            long dur = service.getDuration();
            if (dur > 0) {
                seekBar.setMax((int) dur);
                seekBar.setProgress((int) pos);
                tvPosition.setText(AppUtils.formatDuration(pos / 1000));
                tvDuration.setText(AppUtils.formatDuration(dur / 1000));
            }
        }

        BackgroundAudioService.addListener(this);

        cardPlayPause.setOnClickListener(v -> sendServiceCommand(BackgroundAudioService.ACTION_TOGGLE));
        btnRewind.setOnClickListener(v -> sendServiceCommand(BackgroundAudioService.ACTION_REWIND));
        btnForward.setOnClickListener(v -> sendServiceCommand(BackgroundAudioService.ACTION_FORWARD));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    tvPosition.setText(AppUtils.formatDuration(progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                BackgroundAudioService s = BackgroundAudioService.getInstance();
                if (s != null) {
                    s.seekTo(sb.getProgress());
                }
            }
        });
    }

    private void sendServiceCommand(String action) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), BackgroundAudioService.class);
            intent.setAction(action);
            getContext().startService(intent);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (ivPlayPause != null) {
            ivPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    @Override
    public void onTrackChanged(String title, String artist, String artUrl) {
        if (tvTitle != null) tvTitle.setText(title);
        if (tvArtist != null) tvArtist.setText(artist);
        if (ivArt != null) {
            Glide.with(this).load(artUrl).placeholder(R.drawable.bg_thumbnail_placeholder).into(ivArt);
        }
    }

    @Override
    public void onProgress(long positionMs, long durationMs) {
        if (!isUserSeeking && seekBar != null) {
            if (durationMs > 0) {
                seekBar.setMax((int) durationMs);
                seekBar.setProgress((int) positionMs);
                tvPosition.setText(AppUtils.formatDuration(positionMs / 1000));
                tvDuration.setText(AppUtils.formatDuration(durationMs / 1000));
            }
        }
    }

    @Override
    public void onStopped() {
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BackgroundAudioService.removeListener(this);
    }
}
