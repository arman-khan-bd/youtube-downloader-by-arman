package com.tubepipe.app.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tubepipe.app.R;
import com.tubepipe.app.data.extractor.VideoExtractorService;
import com.tubepipe.app.data.model.VideoItem;
import com.tubepipe.app.ui.adapter.VideoAdapter;
import com.tubepipe.app.ui.dialog.DownloadDialog;
import com.tubepipe.app.ui.player.PlayerActivity;

import org.schabi.newpipe.extractor.Page;

import java.util.List;

public class SearchFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private EditText etSearch;
    private ImageView btnClear;
    private ImageButton btnDoSearch;
    private RecyclerView recyclerResults;
    private ProgressBar searchProgress;
    private TextView tvEmpty;
    private VideoAdapter adapter;

    private String currentQuery = "";
    private Page nextPage = null;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search);
        btnClear = view.findViewById(R.id.btn_clear_search);
        btnDoSearch = view.findViewById(R.id.btn_do_search);
        recyclerResults = view.findViewById(R.id.recycler_search_results);
        searchProgress = view.findViewById(R.id.search_progress);
        tvEmpty = view.findViewById(R.id.tv_search_empty);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerResults.setLayoutManager(layoutManager);
        adapter = new VideoAdapter(this);
        recyclerResults.setAdapter(adapter);

        // Infinite pagination scroll listener for search
        recyclerResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && !isLoading && hasMorePages) {
                    int visible = layoutManager.getChildCount();
                    int total = layoutManager.getItemCount();
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();

                    if ((visible + firstVisible) >= total - 3) {
                        loadMoreSearch();
                    }
                }
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performInitialSearch();
                return true;
            }
            return false;
        });

        btnDoSearch.setOnClickListener(v -> performInitialSearch());

        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            adapter.setVideos(null);
            tvEmpty.setVisibility(View.VISIBLE);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performInitialSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        currentQuery = query;
        nextPage = null;
        hasMorePages = true;
        isLoading = true;

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        searchProgress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        VideoExtractorService.getInstance().searchYouTube(currentQuery, null, new VideoExtractorService.PaginatedCallback() {
            @Override
            public void onSuccess(List<VideoItem> videos, Page next, boolean hasMore) {
                if (!isAdded()) return;
                isLoading = false;
                searchProgress.setVisibility(View.GONE);
                nextPage = next;
                hasMorePages = hasMore;

                if (videos.isEmpty()) {
                    tvEmpty.setText("No results found for \"" + currentQuery + "\"");
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
                adapter.setVideos(videos);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isAdded()) return;
                isLoading = false;
                searchProgress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Search error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMoreSearch() {
        if (isLoading || !hasMorePages || nextPage == null || currentQuery.isEmpty()) return;
        isLoading = true;
        adapter.setLoadingFooter(true);

        VideoExtractorService.getInstance().searchYouTube(currentQuery, nextPage, new VideoExtractorService.PaginatedCallback() {
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
        DownloadDialog.newInstance(video).show(getChildFragmentManager(), "search_download_dialog");
    }
}
