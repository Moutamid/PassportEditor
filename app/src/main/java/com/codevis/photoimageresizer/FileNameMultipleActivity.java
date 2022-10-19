package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.codevis.photoimageresizer.databinding.ActivityFileNameMultipleBinding;
import com.fxn.stash.Stash;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FileNameMultipleActivity extends AppCompatActivity {
    private static final String TAG = "FileNameMultiple";
    String oldFilePathStr;
    //    File newFile;
//    File oldFile;
    File pdfFile;
    String fileName;
//    Uri oldUri;

    private ActivityFileNameMultipleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileNameMultipleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: started");

        Objects.requireNonNull(getSupportActionBar()).setTitle("Images to Pdf Converter");

        fileName = "ImagesToPdf-" + System.currentTimeMillis();

        binding.seekBar2.incrementProgressBy(10);
        binding.tvFileName.setText(fileName);

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
            } else {
                binding.seekBar2.setVisibility(View.GONE);
                binding.compressValue.setVisibility(View.GONE);
            }
        });

        binding.seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = ((int) Math.round(progress / 10)) * 10;
                seekBar.setProgress(progress);

                compressValueInt = progress;

                binding.compressValue.setText("Quality: " + progress + "%");

                Log.d(TAG, "onProgressChanged: " + progress);
                Log.d(TAG, "createPdfFile: quality: " + compressValueInt);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                compressValueInt = seekBar.getProgress();
                Log.d(TAG, "onStopTrackingTouch: progress: " + seekBar.getProgress());
            }
        });

        binding.convertBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: convertBtn");
            createPdfFile();
        });

    }

    float compressValueInt = 100.00f;

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

    ProgressDialog progressDialog;


    private void createPdfFile() {
        progressDialog = new ProgressDialog(FileNameMultipleActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        //save the bitmap image
        File root = new File(commonDocumentDirPath("PdfGenerator", FileNameMultipleActivity.this).toString());
        if (!root.exists()) {
            root.mkdir();
        }
        boolean compress = Stash.getBoolean(Utils.COMPRESS);
        if (compress) return;
        pdfFile = new File(root, fileName + ".pdf");
        ArrayList<String> imagesList = Stash.getArrayList(Utils.PARAMS, String.class);
        PDDocument document = new PDDocument();
        PDPageContentStream contentStream = null;
        Log.d(TAG, "createPdfFile: ");

        try {

            for (int i = 0; i < imagesList.size(); i++) {
                Uri imageUri = Uri.parse(imagesList.get(i));
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                PDPage page = new PDPage(new PDRectangle(bitmap.getWidth(), bitmap.getHeight()));
//            PDRectangle pdRectangle = new PDRectangle(9, 9);
//            page.setCropBox(new PDRectangle());
                document.addPage(page);

                // Define a content stream for adding to the PDF
                contentStream = new PDPageContentStream(document, page);
                Log.e(TAG, "createPdfFile: quality: " + compressValueInt);
                // Here you have great control of the compression rate and DPI on your image.
                // Update 2017/11/22: The DPI param actually is useless as of current version v1.8.9.1 if you take a look into the source code. Compression rate is enough to achieve a much smaller file size.
                PDImageXObject ximage = JPEGFactory
                        .createFromImage(document, bitmap, compressValueInt, 72);
//                            .createFromImage(document, bitmap, 0.75f, 72);
                // You may want to call PDPage.getCropBox() in order to place your image
                // somewhere inside this page rect with (x, y) and (width, height).
                contentStream.drawImage(ximage, 0, 0);
            }
            // Make sure that the content stream is closed:
            document.save(pdfFile);


        } catch (Exception e) {

            e.printStackTrace();
            binding.tvFileName.setText(e.getMessage());
            Log.d(TAG, "createPdfFile: ERROR2: " + e.getMessage());
        }

        progressDialog.dismiss();

        refreshGallery(pdfFile);

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

}