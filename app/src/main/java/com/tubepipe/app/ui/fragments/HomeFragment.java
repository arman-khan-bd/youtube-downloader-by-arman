package com.tubepipe.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.tubepipe.app.R;
import com.tubepipe.app.data.extractor.VideoExtractorService;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.ui.adapter.VideoAdapter;
import com.tubepipe.app.ui.dialog.DownloadDialog;
import com.tubepipe.app.ui.player.PlayerActivity;

import org.schabi.newpipe.extractor.Page;

import java.util.List;

public class HomeFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View layoutError;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;
    private VideoAdapter adapter;

    private Page nextPage = null;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutError = view.findViewById(R.id.layout_error);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnRetry = view.findViewById(R.id.btn_retry);

        swipeRefresh.setColorSchemeResources(R.color.yt_red);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new VideoAdapter(this);
        recyclerView.setAdapter(adapter);

        // Infinite Pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && !isLoading && hasMorePages) {
                    int visible = layoutManager.getChildCount();
                    int total = layoutManager.getItemCount();
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();

                    if ((visible + firstVisible) >= total - 3) {
                        loadMoreTrending();
                    }
                }
            }
        });

        swipeRefresh.setOnRefreshListener(this::loadInitialTrending);
        btnRetry.setOnClickListener(v -> loadInitialTrending());

        loadInitialTrending();
    }

    private void loadInitialTrending() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        layoutError.setVisibility(View.GONE);
        isLoading = true;
        nextPage = null;
        hasMorePages = true;

        VideoExtractorService.getInstance().getTrendingVideos(null, new VideoExtractorService.PaginatedCallback() {
            @Override
            public void onSuccess(List<VideoItem> videos, Page next, boolean hasMore) {
                if (!isAdded()) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                layoutError.setVisibility(View.GONE);

                nextPage = next;
                hasMorePages = hasMore;
                adapter.setVideos(videos);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvErrorMessage.setText("Could not load videos: " + throwable.getMessage());
                layoutError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadMoreTrending() {
        if (isLoading || !hasMorePages || nextPage == null) return;
        isLoading = true;
        adapter.setLoadingFooter(true);

        VideoExtractorService.getInstance().getTrendingVideos(nextPage, new VideoExtractorService.PaginatedCallback() {
            @Override
            public void onSuccess(List<VideoItem> videos, Page next, boolean hasMore) {
                if (!isAdded()) return;
                isLoading = false;
                adapter.setLoadingFooter(false);
                nextPage = next;
                hasMorePages = hasMore;
                adapter.addVideos(videos);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                isLoading = false;
                adapter.setLoadingFooter(false);
            }
        });
    }

    @Override
    public void onVideoClick(VideoItem video) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, video.getUrl());
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, video.getTitle());
        startActivity(intent);
    }

    @Override
    public void onQuickDownloadClick(VideoItem video) {
        DownloadDialog.newInstance(video).show(getChildFragmentManager(), "download_dialog");
    }
}
