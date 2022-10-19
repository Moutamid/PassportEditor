package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.PngToJpgConverterActivity.GALLERY_PICTURE;
import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.codevis.photoimageresizer.databinding.ActivityCompressBinding;
import com.fxn.stash.Stash;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.nanchen.compresshelper.CompressHelper;
import com.theartofdev.edmodo.cropper.CropImage;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnCompressListener;

public class CompressActivity extends AppCompatActivity implements OnUserEarnedRewardListener {
    private static final String TAG = "CompressActivity";
    ActivityCompressBinding b;

    @Override
    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {

    }

    enum SELECTION {QUALITY, FILE_SIZE, RESOLUTION}

    enum COMPRESS_VALUE {KB, MB}

    private SELECTION selection = SELECTION.QUALITY;
    private COMPRESS_VALUE compressValue = COMPRESS_VALUE.KB;

    String fileName;
    Bitmap bitmap;

    File oldFile, newFile;
    Uri oldUri;

    private ProgressDialog progressDialog;
    AdLoader adLoader;
    TemplateView template;
    AdView mAdView;
    AdRequest request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCompressBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setTitle("Image Resizer");
        selection = SELECTION.QUALITY;

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");

        b.qualityBtn.setOnClickListener(v -> {
            selection = SELECTION.QUALITY;
            toggleLayouts();
        });
        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        mAdView = (AdView) findViewById(R.id.adView);
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

        request = new AdRequest.Builder().build();
        decreaseSize();
        // NATIVE AD
        adLoader = new AdLoader.Builder(CompressActivity.this,
                getString(R.string.admob_native_id))
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        // Show the ad.
                        if (!adLoader.isLoading()) {
                            // The AdLoader is still loading ads.
                            // Expect more adLoaded or onAdFailedToLoad callbacks.
                        }

                        if (isDestroyed()) {
                            nativeAd.destroy();
                            return;
                        }

                        NativeTemplateStyle styles = new NativeTemplateStyle
                                .Builder()
                                .withMainBackgroundColor(new ColorDrawable(Color.WHITE))
                                .build();
                        template = findViewById(R.id.native_ad_temp);
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

        b.fileSizeBtn.setOnClickListener(v -> {
            selection = SELECTION.FILE_SIZE;
            toggleLayouts();
        });

        b.resolutionBtn.setOnClickListener(v -> {
            selection = SELECTION.RESOLUTION;
            toggleLayouts();
        });

        b.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean compress = Stash.getBoolean(Utils.COMPRESS);
                if (compress) return;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_PICTURE);
                Stash.put(Utils.SHOW_AD, false);

            }
        });

        b.addImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean compress = Stash.getBoolean(Utils.COMPRESS);
                if (compress) return;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_PICTURE);
                Stash.put(Utils.SHOW_AD, false);

            }
        });
        b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                progress = ((int) Math.round(progress / 10)) * 10;

                if (progress == 0)
                    progress = 10;
                boolean compress = Stash.getBoolean(Utils.COMPRESS);
                if (compress) return;
                seekBar.setProgress(progress);

                b.qualityValueTv.setText("Quality: " + progress + "%");

                compressImage(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        b.dropDownKbLayout.setOnClickListener(v -> {
            boolean compress = Stash.getBoolean(Utils.COMPRESS);
            if (compress) return;
            PopupMenu popupMenu = new PopupMenu(CompressActivity.this, v);
            popupMenu.getMenuInflater().inflate(
                    R.menu.popup_menu_kb_mb,
                    popupMenu.getMenu()
            );
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.item_kb) {
                    compressValue = COMPRESS_VALUE.KB;
                    b.kbTextview.setText("KB");
                    compressImageToKB(Integer.parseInt(b.fileSizeEdittext.getText().toString()));
                }
                if (menuItem.getItemId() == R.id.item_mb) {
                    compressValue = COMPRESS_VALUE.MB;
                    b.kbTextview.setText("MB");
                    int value = Integer.parseInt(b.fileSizeEdittext.getText().toString());
                    compressImageToKB(value * 1000);
                }

                return true;
            });
            popupMenu.show();
        });

        b.saveBtn.setOnClickListener(v -> {
//            progressDialog.show();
//            loadRewardedInterstitial();
            loadAdmobBanner();
        });

        b.dropDownResolutionLayout.setOnClickListener(v -> {
            boolean compress = Stash.getBoolean(Utils.COMPRESS);
            if (compress) return;
            PopupMenu popupMenu = new PopupMenu(CompressActivity.this, v);
            popupMenu.getMenuInflater().inflate(
                    R.menu.popup_menu_resolution,
                    popupMenu.getMenu()
            );
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    b.resolutionTextView.setText(menuItem.getTitle());

                    if (menuItem.getItemId() == R.id.document_large) {
                        setWxH("768", "1024");
                    }
                    if (menuItem.getItemId() == R.id.document_small) {
                        setWxH("600", "800");
                    }
                    if (menuItem.getItemId() == R.id.web_large) {
                        setWxH("480", "640");
                    }
                    if (menuItem.getItemId() == R.id.web_small) {
                        setWxH("336", "448");
                    }
                    if (menuItem.getItemId() == R.id.email_large) {
                        setWxH("235", "314");
                    }
                    if (menuItem.getItemId() == R.id.email_small) {
                        setWxH("160", "160");
                    }

                    return true;
                }

                private void setWxH(String w, String h) {
                    b.widthEdittext.setText(w);
                    b.heightEdittext.setText(h);
                }
            });
            popupMenu.show();
        });
    }

    private void compressImageToKB(int value) {
        if (value > (oldFile.length() / 1024)) {
            Utils.toast(getApplicationContext(), "File size cannot be larger than original size");
            return;
        }

        Luban.compress(CompressActivity.this, oldFile)
                .setMaxSize(value) // IN KB
                .putGear(Luban.CUSTOM_GEAR)
                .launch(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.d(TAG, "onSuccess: before: " + file.length() / 1024);
//                        saveFile(file, new File(commonDocumentDirPath("Compressed"), fileName + "-compressed-size.jpg"));
                        copy(file, new File(commonDocumentDirPath("Compressed", CompressActivity.this), fileName + "-compressed-size.jpg"));

                    }

                    @Override
                    public void onError(Throwable e) {
//                        binding.path.setText(e.getMessage());
                    }
                });

    }

    public void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "copy: ERROR: " + e.getMessage());
        }

        src.delete();

        Uri uri = Uri.fromFile(dst);
        Bitmap newBitmap = null;
        try {
            newBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            Log.d(TAG, "copy: ERROR 2:" + e.getMessage());
            e.printStackTrace();
        }

        b.fileSizeTv.setText("File Size: " + Utils.getFileSize(dst.length()));
        b.path.setText("Saved: " + dst.getPath());
        b.imageView.setImageBitmap(newBitmap);

        refreshGallery(dst);

        Utils.toast(getApplicationContext(), "Done");

        //TODO Utils.share(CompressActivity.this, dst.getPath(), Utils.TYPE_IMG);

        Log.d(TAG, "saveFile: after: " + dst.length() / 1024);
    }

    private void saveFile(Bitmap newBitmap, File toFile) {
        try {
            b.imageView.setImageBitmap(newBitmap);

            FileOutputStream fileOutputStream = new FileOutputStream(toFile);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            b.fileSizeTv.setText("File Size: " + Utils.getFileSize(toFile.length()));
            b.currentResolutionTextView.setText("Current Resolution: " + newBitmap.getWidth() + "x" + newBitmap.getHeight());

            refreshGallery(toFile);
            b.path.setText("Saved: " + toFile.getPath());

            // TODO Utils.share(CompressActivity.this, toFile.getPath(), Utils.TYPE_IMG);

            Log.d(TAG, "saveFile: after: " + toFile.length() / 1024);

        } catch (Exception e) {
            e.printStackTrace();
//            binding.path.setText(e.getMessage());
        }
    }

    private void compressImage(int quality) {
        newFile = new CompressHelper.Builder(this)
                .setQuality(quality)
                .setFileName(fileName + "-compressed-quality")
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(commonDocumentDirPath("Compressed", CompressActivity.this).toString())
                .build()
                .compressToFile(oldFile);

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(newFile));
            b.imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        b.fileSizeTv.setText("File Size: " + Utils.getFileSize(newFile.length()));

    }

    public Bitmap resizeBitmap(File photoFile, int targetW, int targetH) {
        Bitmap bitmap1 = null;

        try {
            bitmap1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(photoFile));
        } catch (IOException e) {
            Log.d(TAG, "resizeBitmap: ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return Bitmap.createScaledBitmap(bitmap1, targetW, targetH, false);
    }

    private void toggleLayouts() {
        b.path.setText("");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), oldUri);
            b.imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        b.fileSizeTv.setText("File Size: " + Utils.getFileSize(oldFile.length()));

        if (selection.equals(SELECTION.QUALITY)) {
            b.qualityBtn.setBackgroundColor(getResources().getColor(R.color.default_red));
            b.fileSizeBtn.setBackgroundColor(getResources().getColor(R.color.black));
            b.resolutionBtn.setBackgroundColor(getResources().getColor(R.color.black));

            b.qualityLayout.setVisibility(View.VISIBLE);
            b.fileSizeLayout.setVisibility(View.GONE);
            b.resolutionLayout.setVisibility(View.GONE);
        }

        if (selection.equals(SELECTION.FILE_SIZE)) {
            b.fileSizeBtn.setBackgroundColor(getResources().getColor(R.color.default_red));
            b.qualityBtn.setBackgroundColor(getResources().getColor(R.color.black));
            b.resolutionBtn.setBackgroundColor(getResources().getColor(R.color.black));

            b.fileSizeLayout.setVisibility(View.VISIBLE);
            b.qualityLayout.setVisibility(View.GONE);
            b.resolutionLayout.setVisibility(View.GONE);
        }

        if (selection.equals(SELECTION.RESOLUTION)) {
            b.resolutionBtn.setBackgroundColor(getResources().getColor(R.color.default_red));
            b.qualityBtn.setBackgroundColor(getResources().getColor(R.color.black));
            b.fileSizeBtn.setBackgroundColor(getResources().getColor(R.color.black));

            b.resolutionLayout.setVisibility(View.VISIBLE);
            b.qualityLayout.setVisibility(View.GONE);
            b.fileSizeLayout.setVisibility(View.GONE);
        }


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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mAdView.setVisibility(View.VISIBLE);
                mAdView.loadAd(request);

//        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
                b.specsLayout.setVisibility(View.VISIBLE);
                b.saveBtn.setVisibility(View.VISIBLE);
                b.addImageBtn.setVisibility(View.GONE);
                findViewById(R.id.native_ad_temp).setVisibility(View.GONE);
//            final Uri resultUri = UCrop.getOutput(data);
                oldUri = resultUri;

                fileName = Utils.getFileName(CompressActivity.this, resultUri);

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                oldFile = new File(resultUri.getPath());
                b.imageView.setImageBitmap(bitmap);

                b.fileSizeTv.setText("File Size: " + Utils.getFileSize(oldFile.length()));
                b.fileSizeEdittext.setText((oldFile.length() / 1024) + "");

                b.widthEdittext.setText(bitmap.getWidth() + "");
                b.heightEdittext.setText(bitmap.getHeight() + "");
                b.currentResolutionTextView.setText("Current Resolution: " + bitmap.getWidth() + "x" + bitmap.getHeight());
//                new Handler().postDelayed(() ->
                compressImage(100);
                Stash.put(Utils.SHOW_AD, true);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        600
                );
                params.setMargins(0, 20, 0, 0);
//                params.gravity = Gravity.CENTER_HORIZONTAL;
                b.imageView.setLayoutParams(params);

//                        , 500);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }

        }
        if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }

        if (requestCode == GALLERY_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();

            CropImage.activity(selectedImage)
                    .start(this);
        }
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
                progressDialog.dismiss();
                mInterstitialAd.show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.e("ads", String.valueOf(errorCode));
                progressDialog.dismiss();
                saveFinalFile();
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
                saveFinalFile();
            }
        });
    }

    private void decreaseSize() {
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

    RewardedInterstitialAd rewardedInterstitialAd;

    private void loadRewardedInterstitial() {
        RewardedInterstitialAd.load(CompressActivity.this, getString(R.string.admob_rewarded_interstitial),
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
                                saveFinalFile();

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
                        rewardedInterstitialAd.show(CompressActivity.this,
                                CompressActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        rewardedInterstitialAd = null;
                        loadAdmobBanner();
                    }
                });
    }


    private void saveFinalFile() {
        if (selection.equals(SELECTION.QUALITY)) {
            if (newFile != null) {
                b.path.setText("Saved: " + newFile.getPath());
                refreshGallery(newFile);
                Utils.toast(getApplicationContext(), "Saved");
                // TODO Utils.share(CompressActivity.this, newFile.getPath(), Utils.TYPE_IMG);
            } else {

                b.path.setText("Saved: " + oldFile.getPath());
                refreshGallery(oldFile);
                Utils.toast(getApplicationContext(), "Saved");
                // TODO Utils.share(CompressActivity.this, oldFile.getPath(), Utils.TYPE_IMG);
            }
        }

        if (selection.equals(SELECTION.FILE_SIZE)) {
            Utils.toast(getApplicationContext(), "Loading...");
            if (compressValue.equals(COMPRESS_VALUE.KB)) {
                Log.d(TAG, "onTextChanged: kb");
                compressImageToKB(Integer.parseInt(b.fileSizeEdittext.getText().toString()));
            } else {
                Log.d(TAG, "onTextChanged: mb");
                int value = Integer.parseInt(b.fileSizeEdittext.getText().toString());
                compressImageToKB(value * 1000);
            }
        }

        if (selection.equals(SELECTION.RESOLUTION)) {
            bitmap = resizeBitmap(
                    oldFile,
                    Integer.parseInt(b.widthEdittext.getText().toString()),
                    Integer.parseInt(b.heightEdittext.getText().toString()));

            saveFile(bitmap, new File(commonDocumentDirPath("Compressed", CompressActivity.this), fileName + "-resolution.jpg"));
        }
    }

}