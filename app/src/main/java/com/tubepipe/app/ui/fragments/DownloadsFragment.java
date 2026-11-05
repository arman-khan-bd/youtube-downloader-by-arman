package com.tubepipe.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.tubepipe.app.R;
import com.tubepipe.app.data.downloader.DownloadManager;
import com.tubepipe.app.data.model.DownloadItem;
import com.tubepipe.app.data.player.BackgroundAudioService;
import com.tubepipe.app.ui.adapter.DownloadAdapter;
import com.tubepipe.app.ui.player.AudioPlayerBottomSheet;
import com.tubepipe.app.ui.player.PlayerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadsFragment extends Fragment implements DownloadAdapter.OnDownloadActionListener, DownloadManager.DownloadListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private View layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private DownloadAdapter adapter;

    private final List<DownloadItem> allDownloads = new ArrayList<>();
    private int selectedTab = 0; // 0 = Videos, 1 = Audios

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_downloads);
        recyclerView = view.findViewById(R.id.recycler_downloads);
        layoutEmpty = view.findViewById(R.id.layout_empty_downloads);
        ivEmptyIcon = view.findViewById(R.id.iv_empty_icon);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_empty_desc);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadAdapter(this);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                filterAndDisplay();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        DownloadManager.getInstance(requireContext()).addListener(this);
        loadDownloads();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null) {
            DownloadManager.getInstance(requireContext()).removeListener(this);
        }
    }

    private void loadDownloads() {
        if (getContext() == null) return;
        allDownloads.clear();
        allDownloads.addAll(DownloadManager.getInstance(requireContext()).getDownloads());
        updateTabCounts();
        filterAndDisplay();
    }

    private void updateTabCounts() {
        int videoCount = 0;
        int audioCount = 0;
        for (DownloadItem item : allDownloads) {
            if (item.isAudioOnly()) audioCount++;
            else videoCount++;
        }

        TabLayout.Tab videoTab = tabLayout.getTabAt(0);
        if (videoTab != null) {
            videoTab.setText("Videos (" + videoCount + ")");
        }
        TabLayout.Tab audioTab = tabLayout.getTabAt(1);
        if (audioTab != null) {
            audioTab.setText("Audios (" + audioCount + ")");
        }
    }

    private void filterAndDisplay() {
        List<DownloadItem> filtered = new ArrayList<>();
        boolean wantAudio = selectedTab == 1;

        for (DownloadItem item : allDownloads) {
            if (wantAudio && item.isAudioOnly()) {
                filtered.add(item);
            } else if (!wantAudio && !item.isAudioOnly()) {
                filtered.add(item);
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (wantAudio) {
                ivEmptyIcon.setImageResource(R.drawable.ic_audio);
                tvEmptyTitle.setText("No Downloaded Audios");
                tvEmptyDesc.setText("Select \"Audio Quality\" when downloading to save MP3 or M4A music files!");
            } else {
                ivEmptyIcon.setImageResource(R.drawable.ic_video);
                tvEmptyTitle.setText("No Downloaded Videos");
                tvEmptyDesc.setText("Download videos in 1080p, 720p or 480p to watch offline anytime!");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPlayOffline(DownloadItem item) {
        if (item.getTargetFilePath() == null) return;
        File file = new File(item.getTargetFilePath());
        if (!file.exists()) {
            Toast.makeText(getContext(), "Downloaded file not found on storage", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.isAudioOnly()) {
            // Play with Background Audio Service & open Audio Sheet
            Intent serviceIntent = new Intent(getContext(), BackgroundAudioService.class);
            serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY_ITEM);
            serviceIntent.putExtra(BackgroundAudioService.EXTRA_URI, file.getAbsolutePath());
            serviceIntent.putExtra(BackgroundAudioService.EXTRA_TITLE, item.getTitle());
            serviceIntent.putExtra(BackgroundAudioService.EXTRA_ARTIST, "Offline Audio");
            serviceIntent.putExtra(BackgroundAudioService.EXTRA_ART_URL, item.getThumbnailUrl());
            requireContext().startService(serviceIntent);

            AudioPlayerBottomSheet.newInstance().show(getParentFragmentManager(), "audio_player_sheet");
        } else {
            // Play video in ExoPlayer PlayerActivity
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_OFFLINE_PATH, file.getAbsolutePath());
            intent.putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, item.getTitle());
            startActivity(intent);
        }
    }

    @Override
    public void onDeleteDownload(DownloadItem item) {
        if (getContext() != null) {
            if (item.getTargetFilePath() != null) {
                File file = new File(item.getTargetFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            DownloadManager.getInstance(requireContext()).removeDownload(item);
        }
    }

    @Override
    public void onDownloadUpdated(DownloadItem item) {
        adapter.updateItem(item);
    }

    @Override
    public void onDownloadCompleted(DownloadItem item) {
        loadDownloads();
    }

    @Override
    public void onDownloadFailed(DownloadItem item) {
        adapter.updateItem(item);
    }

    @Override
    public void onListChanged() {
        loadDownloads();
    }
}
