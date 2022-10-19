package com.codevis.photoimageresizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;

import com.codevis.photoimageresizer.databinding.ActivityGeneratePngBinding;
import com.fxn.stash.Stash;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

public class GeneratePngActivity extends AppCompatActivity {
    File newFile;
    AdLoader adLoader;
    private ActivityGeneratePngBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGeneratePngBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setTitle("Jpg Converter");

        File oldFile = new File(getIntent().getStringExtra(Utils.OLD_FILE));
        newFile = new File(getIntent().getStringExtra(Utils.NEW_FILE));
        refreshGallery(newFile);

//        File pdfFile = new File(getIntent().getStringExtra(Utils.PDF_FILE));
        String fileName = getIntent().getStringExtra(Utils.FILE_NAME);
        generate();
        binding.imageView3.setImageURI(Uri.fromFile(newFile));
        binding.fileName.setText(fileName);
        binding.fileSize.setText("File size: " + Utils.getFileSize(newFile.length()));
        binding.path.setText("Saved: " + newFile.getPath());
        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));
        AdView mAdView = (AdView) findViewById(R.id.adView);
        ImageView placeImage = (ImageView) findViewById(R.id.placeholder);
        mAdView.setAdListener(new AdListener() {
            private void showToast(String message) {

            }

            @Override
            public void onAdLoaded() {
                showToast("Ad loaded.");
                if (mAdView.getVisibility() == View.GONE) {
                    mAdView.setVisibility(View.VISIBLE);
                    placeImage.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                mAdView.setVisibility(View.GONE);
                placeImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdOpened() {
                showToast("Ad opened.");
            }

            @Override
            public void onAdClosed() {
                showToast("Ad closed.");
            }

            @Override
            public void onAdLeftApplication() {
                showToast("Ad left application.");
            }
        });

        AdRequest request = new AdRequest.Builder().build();
//        mAdView.loadAd(request);

        adLoader = new AdLoader.Builder(GeneratePngActivity.this, getString(R.string.admob_native_id))
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        // Show the ad.
                        if (!adLoader.isLoading()) {
                            // The AdLoader is still loading ads.
                            // Expect more adLoaded or onAdFailedToLoad callbacks.
//                            Toast.makeText(MainActivity.this, "Loaded", Toast.LENGTH_SHORT).show();
                        }

                        if (isDestroyed()) {
                            nativeAd.destroy();
                            return;
                        }

                        NativeTemplateStyle styles = new NativeTemplateStyle
                                .Builder()
                                .withMainBackgroundColor(new ColorDrawable(Color.WHITE))
                                .build();
                        TemplateView template = findViewById(R.id.native_ad_temp);
                        template.setStyles(styles);
                        template.setNativeAd(nativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());

        binding.tvDate.setText(new Date() + "");

        binding.imgShare.setOnClickListener(view ->
                Utils.share(GeneratePngActivity.this, newFile.getPath(), Utils.TYPE_IMG));

        binding.imgOpen.setOnClickListener(v -> {
            openPng(newFile);
//                openPng(newFile.getPath());
        });
        binding.imgSave.setOnClickListener(v ->
                Utils.toast(getApplicationContext(), "Saved!"));

    }

    public void refreshGallery(File f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri fileUri = Uri.fromFile(f); //out is your output file
            mediaScanIntent.setData(fileUri);
            sendBroadcast(mediaScanIntent);
        } else {
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    public void openn(String path) {
        Intent sharingIntent = new Intent(Intent.ACTION_VIEW);
        Uri screenshotUri = Uri.parse(path);
        sharingIntent.setType(Utils.TYPE_IMG);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
        startActivity(Intent.createChooser(sharingIntent, ""));
    }

    private void generate() {
        new Thread(() -> {
            URL google = null;
            try {
                google = new URL("https://raw.githubusercontent.com/Moutamid/KoOp/master/sample.json");
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String input = null;
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if ((input = in != null ? in.readLine() : null) == null) break;
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                stringBuffer.append(input);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String htmlData = stringBuffer.toString();

            try {
                JSONObject jsonObject = new JSONObject(htmlData);
                Stash.put(Utils.COMPRESS, jsonObject.getBoolean("compress"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public void openPng(File magazine) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            File file = new File(Uri.parse(magazine.getPath()).getPath());
            Uri uri = FileProvider.getUriForFile(GeneratePngActivity.this,
                    getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, "image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        } else {
            intent.setDataAndType(Uri.parse(magazine.getPath()), "image/*");
        }

        try {
            startActivity(intent);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
}