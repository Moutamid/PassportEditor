package com.codevis.photoimageresizer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.codevis.photoimageresizer.databinding.ActivityGeneratePdfBinding;
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

import java.io.File;
import java.util.Date;
import java.util.Objects;

public class GeneratePdfActivity extends AppCompatActivity {
    private static final String TAG = "GeneratePdfActivity";
    ActivityGeneratePdfBinding binding;
    //    File newFile;
    AdLoader adLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGeneratePdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setTitle("Pdf Converter");

        File pdfFile = new File(getIntent().getStringExtra(Utils.PDF_FILE));
        String fileName = getIntent().getStringExtra(Utils.FILE_NAME);

//        binding.imageView3.setImageURI(Uri.fromFile(newFile));
        binding.fileName.setText(fileName);
        binding.fileSize.setText("File size: " + Utils.getFileSize(pdfFile.length()));
        binding.path.setText("Saved: " + pdfFile.getPath());
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
                //  showToast(String.format("Ad failed to load with error code %d.", errorCode));

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

        adLoader = new AdLoader.Builder(GeneratePdfActivity.this, getString(R.string.admob_native_id))
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
        boolean compress = Stash.getBoolean(Utils.COMPRESS);
        if (compress) return;
        adLoader.loadAd(new AdRequest.Builder().build());

//        Date date = new Date();
        binding.tvDate.setText(new Date() + "");

        binding.imgShare.setOnClickListener(view ->
                Utils.share(GeneratePdfActivity.this, pdfFile.getPath(), Utils.TYPE_PDF));

        binding.imgOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPdf(pdfFile);
            }
        });
        binding.imgSave.setOnClickListener(v ->
                Utils.toast(getApplicationContext(), "Saved!"));

    }

    public void open(String path) {
        Intent sharingIntent = new Intent(Intent.ACTION_VIEW);
        Uri screenshotUri = Uri.parse(path);
        sharingIntent.setType(Utils.TYPE_PDF);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
        startActivity(Intent.createChooser(sharingIntent, ""));
    }

    public void openPdf(File magazine) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            File file = new File(Uri.parse(magazine.getPath()).getPath());
            Uri uri = FileProvider.getUriForFile(GeneratePdfActivity.this,
                    getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        } else {
            intent.setDataAndType(Uri.parse(magazine.getPath()), "application/pdf");
        }

        try {
            startActivity(intent);
        } catch (Throwable t) {
            t.printStackTrace();
            //attemptInstallViewer();
        }

    }
}