package com.tubepipe.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tubepipe.app.R;
import com.tubepipe.app.data.model.StreamQuality;
import com.tubepipe.app.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class QualityOptionAdapter extends RecyclerView.Adapter<QualityOptionAdapter.QualityViewHolder> {

    public interface OnQualitySelectedListener {
        void onSelected(StreamQuality quality);
    }

    private final List<StreamQuality> qualities = new ArrayList<>();
    private int selectedPosition = 0;
    private final OnQualitySelectedListener listener;

    public QualityOptionAdapter(OnQualitySelectedListener listener) {
        this.listener = listener;
    }

    public void setQualities(List<StreamQuality> list) {
        qualities.clear();
        if (list != null) {
            qualities.addAll(list);
        }
        selectedPosition = 0;
        notifyDataSetChanged();
        if (!qualities.isEmpty() && listener != null) {
            listener.onSelected(qualities.get(0));
        }
    }

    public StreamQuality getSelectedQuality() {
        if (selectedPosition >= 0 && selectedPosition < qualities.size()) {
            return qualities.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public QualityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quality_option, parent, false);
        return new QualityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QualityViewHolder holder, int position) {
        StreamQuality quality = qualities.get(position);
        holder.bind(quality, position == selectedPosition, () -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSelected(quality);
            }
        });
    }

    @Override
    public int getItemCount() {
        return qualities.size();
    }

    static class QualityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;
        private final TextView tvDetails;
        private final TextView tvSize;
        private final RadioButton rbSelected;

        public QualityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_quality_icon);
            tvName = itemView.findViewById(R.id.tv_quality_name);
            tvDetails = itemView.findViewById(R.id.tv_quality_details);
            tvSize = itemView.findViewById(R.id.tv_quality_size);
            rbSelected = itemView.findViewById(R.id.rb_selected);
        }

        public void bind(StreamQuality quality, boolean isSelected, Runnable onSelect) {
            tvName.setText(quality.getResolution());

            StringBuilder details = new StringBuilder();
            details.append(quality.isAudioOnly() ? "Audio (" : "Video (");
            details.append(quality.getExtension().toUpperCase()).append(")");
            if (quality.getBitrate() > 0) {
                details.append(" • ").append(quality.getBitrate()).append(" kbps");
            }
            tvDetails.setText(details.toString());

            // Bind file size badge
            if (quality.getApproxSizeBytes() > 0) {
                tvSize.setVisibility(View.VISIBLE);
                tvSize.setText(AppUtils.formatFileSize(quality.getApproxSizeBytes()));
            } else {
                tvSize.setText("Stream");
            }

            ivIcon.setImageResource(quality.isAudioOnly() ? R.drawable.ic_audio : R.drawable.ic_video);
            rbSelected.setChecked(isSelected);

            itemView.setOnClickListener(v -> onSelect.run());
            rbSelected.setOnClickListener(v -> onSelect.run());
        }
    }
}
