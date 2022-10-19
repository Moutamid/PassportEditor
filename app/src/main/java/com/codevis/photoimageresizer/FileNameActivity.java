package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.codevis.photoimageresizer.databinding.ActivityFileNameBinding;
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
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class FileNameActivity extends AppCompatActivity implements OnUserEarnedRewardListener {
    private static final String TAG = "FileNameActivity";

    ActivityFileNameBinding binding;
    String oldFilePathStr;
    File newFile;
    File oldFile;
    File pdfFile;
    String fileName;
    Uri oldUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: started");

        Objects.requireNonNull(getSupportActionBar()).setTitle("Pdf Converter");
        boolean compress = Stash.getBoolean(Utils.COMPRESS);
        if (compress) return;
        Intent intent = getIntent();
        fileName = intent.getStringExtra("fileName");
        oldFilePathStr = intent.getStringExtra("filePath");

        binding.seekBar2.incrementProgressBy(10);
        oldFile = new File(oldFilePathStr);
        newFile = new File("");
        oldUri = Uri.fromFile(oldFile);

        binding.imageView2.setImageURI(oldUri);
        binding.tvFileName.setText(fileName);

        createPdfFile(oldUri);
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


        binding.compressSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCreate: compressSwitch: " + isChecked);
            if (isChecked) {
                binding.seekBar2.setVisibility(View.VISIBLE);
                binding.compressValue.setVisibility(View.VISIBLE);
                compressAndCreatePdf(binding.seekBar2.getProgress());
            } else {
                binding.seekBar2.setVisibility(View.GONE);
                binding.compressValue.setVisibility(View.GONE);
                createPdfFile(oldUri);
            }
        });

        binding.seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                progress = ((int) Math.round(progress / 10)) * 10;
                seekBar.setProgress(progress);

                binding.compressValue.setText("Quality: " + progress + "%");

                if (progress == 100)
                    createPdfFile(oldUri);
                else
                    compressAndCreatePdf(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.convertBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: convertBtn");
//            loadRewardedInterstitial();
            loadAdmobBanner();
            startActivity(new Intent(FileNameActivity.this, GeneratePdfActivity.class)
                    .putExtra(Utils.OLD_FILE, oldFile.toString())
                    .putExtra(Utils.NEW_FILE, newFile.toString())
                    .putExtra(Utils.PDF_FILE, pdfFile.toString())
                    .putExtra(Utils.FILE_NAME, fileName)
            );

        });
        addFileName();
    }

    private void addFileName() {
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

    private void loadRewardedInterstitial() {
        RewardedInterstitialAd.load(FileNameActivity.this, getString(R.string.admob_rewarded_interstitial),
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
                        rewardedInterstitialAd.show(FileNameActivity.this,
                                FileNameActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        rewardedInterstitialAd = null;
                        loadAdmobBanner();
                    }
                });
    }

    private void compressAndCreatePdf(int quality) {
        newFile = new CompressHelper.Builder(this)
                .setQuality(quality)
                .setFileName(fileName + "-compressed")
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(getFilesDir().toString())
                .build()
                .compressToFile(oldFile);

        Log.d(TAG, "compressAndCreatePdf: ");

        createPdfFile(Uri.fromFile(newFile));
    }

    private void createPdfFile(Uri imageUri) {
//        new Thread(() -> {
        Log.d(TAG, "createPdfFile: ");
        String[] filePath = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(imageUri, filePath, null, null, null);

        String myPath;
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePath[0]);
            myPath = cursor.getString(columnIndex);
            cursor.close();
        } else {
            myPath = imageUri.getPath();
        }
        Log.d(TAG, "createPdfFile: myPath: " + myPath);

        Bitmap bitmap = BitmapFactory.decodeFile(myPath);
        Bitmap finalBitmap = bitmap;
        runOnUiThread(() -> {
            binding.imageView2.setImageBitmap(finalBitmap);
        });

        //save the bitmap image
        File root = new File(commonDocumentDirPath("PdfGenerator", FileNameActivity.this).toString());
        if (!root.exists()) {
            root.mkdir();
        }
        pdfFile = new File(root, fileName + ".pdf");

        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(new PDRectangle(bitmap.getWidth(), bitmap.getHeight()));
//            PDRectangle pdRectangle = new PDRectangle(9, 9);
//            page.setCropBox(new PDRectangle());
            document.addPage(page);

            // Define a content stream for adding to the PDF
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Here you have great control of the compression rate and DPI on your image.
            // Update 2017/11/22: The DPI param actually is useless as of current version v1.8.9.1 if you take a look into the source code. Compression rate is enough to achieve a much smaller file size.
            PDImageXObject ximage = JPEGFactory.createFromImage(document, bitmap, 0.75f, 72);
            // You may want to call PDPage.getCropBox() in order to place your image
            // somewhere inside this page rect with (x, y) and (width, height).
            contentStream.drawImage(ximage, 0, 0);

            // Make sure that the content stream is closed:
            contentStream.close();

            document.save(pdfFile);
            document.close();
        } catch (Exception e) {
            Log.d(TAG, "createPdfFile: ERROR: " + e.getMessage());
            binding.tvFileSize.setText(e.getMessage());
            e.getStackTrace();
        }
        runOnUiThread(() -> {
            binding.tvFileSize.setText("File size: " + Utils.getFileSize(pdfFile.length()));
        });

        refreshGallery(pdfFile);
//        }).start();
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
    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {

    }
}