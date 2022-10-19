package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.codevis.photoimageresizer.databinding.ActivityPngFileNameBinding;
import com.fxn.stash.Stash;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.nanchen.compresshelper.CompressHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class PngFileNameActivity extends AppCompatActivity implements OnUserEarnedRewardListener {
    private static final String TAG = "PngFileNameActivity";

    String oldFilePathStr;
    File newFile;
    File oldFile;
    //    File pdfFile;
    String fileName;
    Uri oldUri;
    private ActivityPngFileNameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPngFileNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        fileName = intent.getStringExtra("fileName");
        oldFilePathStr = intent.getStringExtra("filePath");
        Objects.requireNonNull(getSupportActionBar()).setTitle("Jpg Converter");
        boolean compress = Stash.getBoolean(Utils.COMPRESS);
        if (compress) return;
        binding.seekBar2.incrementProgressBy(10);
        oldFile = new File(oldFilePathStr);
        newFile = new File("");
        oldUri = Uri.fromFile(oldFile);

        binding.imageView2.setImageURI(oldUri);
        binding.tvFileName.setText(fileName);

        binding.imageView2.setImageURI(oldUri);

        binding.tvFileSize.setText("File size: " + Utils.getFileSize(oldFile.length()));
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
        mAdView.loadAd(request);
        compress22();
        binding.compressSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCreate: compressSwitch: " + isChecked);
            if (isChecked) {
                binding.seekBar2.setVisibility(View.VISIBLE);
                binding.compressValue.setVisibility(View.VISIBLE);
                compress(binding.seekBar2.getProgress());
            } else {
                binding.seekBar2.setVisibility(View.GONE);
                binding.compressValue.setVisibility(View.GONE);
                compress(100);
            }
        });

        binding.seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                progress = ((int) Math.round(progress / 10)) * 10;
                seekBar.setProgress(progress);

                binding.compressValue.setText("Quality: " + progress + "%");

                compress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.convertBtn.setOnClickListener(v -> {
//            loadRewardedInterstitial();
            loadAdmobBanner();
            Log.d(TAG, "onCreate: convertBtn");
            startActivity(new Intent(PngFileNameActivity.this, GeneratePngActivity.class)
                    .putExtra(Utils.OLD_FILE, oldFile.toString())
                    .putExtra(Utils.NEW_FILE, newFile.toString())
                    .putExtra(Utils.FILE_NAME, fileName)
            );

        });

        compress(100);
    }

    private void compress22() {
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

    private void loadAdmobBanner() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        InterstitialAd mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.admob_interstitial_id));
        AdRequest ad = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(ad);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.

                Log.e("ads", String.valueOf(errorCode));
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
            }
        });
    }

    RewardedInterstitialAd rewardedInterstitialAd;
    private ProgressDialog progressDialog;

    private void loadRewardedInterstitial() {
        progressDialog = new ProgressDialog(PngFileNameActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
//        progressDialog.show();
        RewardedInterstitialAd.load(PngFileNameActivity.this, getString(R.string.admob_rewarded_interstitial),
                new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        Log.d(TAG, "Ad was loaded.");
                        rewardedInterstitialAd = ad;
                        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                rewardedInterstitialAd = null;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                rewardedInterstitialAd = null;
                                loadAdmobBanner();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });
                        rewardedInterstitialAd.show(PngFileNameActivity.this,
                                PngFileNameActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        rewardedInterstitialAd = null;
                        loadAdmobBanner();
                    }
                });
    }

    private void compress(int quality) {
        newFile = new CompressHelper.Builder(this)
                .setQuality(quality)
                .setFileName(fileName + "-png-to-jpg")
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(commonDocumentDirPath("PngToJpg", PngFileNameActivity.this).toString())
                .build()
                .compressToFile(oldFile);

        binding.tvFileSize.setText("File size: " + Utils.getFileSize(newFile.length()));

        Log.d(TAG, "compressAndCreatePdf: ");

    }


    @Override
    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
        Log.i(TAG, "User earned reward.");
    }
}