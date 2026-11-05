package com.tubepipe.app.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.tubepipe.app.R;
import com.tubepipe.app.data.settings.SettingsManager;
import com.tubepipe.app.util.AppUtils;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private RadioButton rbPhone;
    private RadioButton rbSd;
    private TextView tvPhonePath;
    private TextView tvPhoneFree;
    private TextView tvSdPath;
    private TextView tvSdFree;
    private MaterialCardView cardPhone;
    private MaterialCardView cardSd;
    private MaterialButton btnChooseCustom;

    private SettingsManager settingsManager;

    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                getContentResolver().takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                );
                            }
                            String path = treeUri.getPath();
                            settingsManager.setCustomStoragePath(path);
                            settingsManager.setStorageLocation(SettingsManager.StorageLocation.SD_CARD);
                            updateStorageUi();
                            Toast.makeText(this, "Custom download folder set", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Could not set folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = SettingsManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rbPhone = findViewById(R.id.rb_storage_phone);
        rbSd = findViewById(R.id.rb_storage_sd);
        tvPhonePath = findViewById(R.id.tv_phone_path);
        tvPhoneFree = findViewById(R.id.tv_phone_free_space);
        tvSdPath = findViewById(R.id.tv_sd_path);
        tvSdFree = findViewById(R.id.tv_sd_free_space);
        cardPhone = findViewById(R.id.card_storage_phone);
        cardSd = findViewById(R.id.card_storage_sd);
        btnChooseCustom = findViewById(R.id.btn_choose_custom_folder);

        cardPhone.setOnClickListener(v -> {
            settingsManager.setStorageLocation(SettingsManager.StorageLocation.PHONE_INTERNAL);
            updateStorageUi();
        });

        cardSd.setOnClickListener(v -> {
            if (!settingsManager.isSdCardAvailable() && settingsManager.getCustomStoragePath() == null) {
                Toast.makeText(this, "No SD Card detected. Pick a custom folder instead.", Toast.LENGTH_SHORT).show();
            } else {
                settingsManager.setStorageLocation(SettingsManager.StorageLocation.SD_CARD);
                updateStorageUi();
            }
        });

        btnChooseCustom.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                folderPickerLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Custom directory picking requires Android 5.0+", Toast.LENGTH_SHORT).show();
            }
        });

        updateStorageUi();
    }

    private void updateStorageUi() {
        SettingsManager.StorageLocation loc = settingsManager.getStorageLocation();
        rbPhone.setChecked(loc == SettingsManager.StorageLocation.PHONE_INTERNAL);
        rbSd.setChecked(loc == SettingsManager.StorageLocation.SD_CARD);

        File internalDir = settingsManager.getEffectiveDownloadDirectory();
        tvPhonePath.setText(internalDir != null ? internalDir.getAbsolutePath() : "/Download/TubePipe");
        tvPhoneFree.setText("Free: " + AppUtils.formatFileSize(settingsManager.getInternalFreeSpaceBytes()));

        boolean hasSd = settingsManager.isSdCardAvailable();
        File sdDir = settingsManager.getSdCardDirectory();
        if (sdDir != null) {
            tvSdPath.setText(sdDir.getAbsolutePath());
            tvSdFree.setText("Free: " + AppUtils.formatFileSize(settingsManager.getSdCardFreeSpaceBytes()));
        } else if (hasSd) {
            tvSdPath.setText("Removable MicroSD detected");
            tvSdFree.setText("Free: " + AppUtils.formatFileSize(settingsManager.getSdCardFreeSpaceBytes()));
        } else {
            tvSdPath.setText("No physical SD Card detected");
            tvSdFree.setText("Tap 'Choose Custom Download Folder' to select an external directory");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
