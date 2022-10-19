package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codevis.photoimageresizer.databinding.ActivityPdfConverterBinding;
import com.fxn.stash.Stash;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.theartofdev.edmodo.cropper.CropImage;
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
import java.util.ArrayList;
import java.util.Objects;

public class PdfConverterActivity extends AppCompatActivity {
    private static final String TAG = "PdfConverterActivity";
    ActivityPdfConverterBinding binding;
    ArrayList<Uri> imagesList = new ArrayList<>();
    ArrayList<Bitmap> imagesList2 = new ArrayList<>();
//    ArrayList<String> imagesList2 = new ArrayList<>();

    File pdfFile;

    public static final int GALLERY_PICTURE = 1;
    boolean boolean_permission;
    //    Bitmap bitmap;
    String fileName;
    public static final int REQUEST_PERMISSIONS = 1;
    String filePath_;
    float compressValueInt = 0.75f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfConverterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setTitle("Pdf Converter");

        fn_permission();

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean compress = Stash.getBoolean(Utils.COMPRESS);
                if (compress) return;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Pictures"),
                        GALLERY_PICTURE);
                Stash.put(Utils.SHOW_AD, false);

            }
        });
        pdf();
        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                loadAdmobBanner();
                boolean compress = Stash.getBoolean(Utils.COMPRESS);
                if (compress) return;
                if (isMultiple) {
                    binding.firstLayout.setVisibility(View.GONE);
                    binding.secondLayout.setVisibility(View.VISIBLE);
                } else {
                    Intent intent1 = new Intent(getApplicationContext(), FileNameActivity.class);
                    intent1.putExtra("fileName", fileName);
                    intent1.putExtra("filePath", filePath_);
                    intent1.putExtra(Utils.PARAMS, isMultiple);
                    startActivity(intent1);
                }
            }

        });

        //----------------------------------------------------------------------
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
//                compressAndCreatePdf(binding.seekBar2.getProgress());
            } else {
                binding.seekBar2.setVisibility(View.GONE);
                binding.compressValue.setVisibility(View.GONE);
//                createPdfFile(oldUri);
            }
        });

        binding.seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress);
                progress = (Math.round(progress / 10)) * 10;
                compressValueInt = (Math.round(progress / 10)) / 10;
                seekBar.setProgress(progress);
                binding.compressValue.setText("Quality: " + progress + "%");

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
            try {
                createPdfFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                imagesList.add(resultUri);
                try {
                    Bitmap bitmapppp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    imagesList2.add(bitmapppp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                initRecyclerView();
                fileName = Utils.getFileName(PdfConverterActivity.this, resultUri);


                filePath_ = resultUri.getPath();
                Hide();

                binding.button.setVisibility(View.VISIBLE);
                Stash.put(Utils.SHOW_AD, true);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

        if (requestCode == GALLERY_PICTURE) {
            Log.d(TAG, "onActivityResult: if (requestCode == GALLERY_PICTURE) {");
            imagesList.clear();
            imagesList2.clear();
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult: if (resultCode == Activity.RESULT_OK) {");
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    Log.e(TAG, "onActivityResult: loop ran with count: " + count);
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imagesList.add(imageUri);
                        try {
                            Bitmap bitmappp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            imagesList2.add(bitmappp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "onActivityResult: loop: " + imageUri.getPath());
                    }
                    isMultiple = true;
                    Hide();
                    initRecyclerView();
                    binding.button.setVisibility(View.VISIBLE);
                    Log.d(TAG, "onActivityResult: end");
                } else if (data.getData() != null) {
//                    String imagePath = data.getData().getPath();
                    CropImage.activity(data.getData())
                            .start(this);
                }
            }
        }
    }

    private boolean isMultiple = false;

    private RecyclerView imagesRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView() {
        imagesRecyclerView = binding.imagesRecyclerView;
        adapter = new RecyclerViewAdapterMessages();
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        imagesRecyclerView.setHasFixedSize(true);
        imagesRecyclerView.setNestedScrollingEnabled(false);

        imagesRecyclerView.setAdapter(adapter);

    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_images, parent, false);
            return new ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int position) {
            holder.image.setImageURI(imagesList.get(position));
        }

        @Override
        public int getItemCount() {
            if (imagesList == null)
                return 0;
            return imagesList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {
            ImageView image;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                image = v.findViewById(R.id.img);
            }
        }

    }

    private void fn_permission() {
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

            if ((ActivityCompat.shouldShowRequestPermissionRationale(PdfConverterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE))) {
            } else {
                ActivityCompat.requestPermissions(PdfConverterActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);

            }

            if ((ActivityCompat.shouldShowRequestPermissionRationale(PdfConverterActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            } else {
                ActivityCompat.requestPermissions(PdfConverterActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);

            }
        } else {
            boolean_permission = true;


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                boolean_permission = true;
            }
        }
    }

    public void Hide() {
//        if (bitmap != null) {
        binding.imageView5.setVisibility(View.GONE);
        binding.textView3.setVisibility(View.GONE);
        binding.textView4.setVisibility(View.GONE);
//        }
    }

    //--------------------------------------------------------------------------------------------------

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

    private void createPdfFile() throws IOException {
        progressDialog = new ProgressDialog(PdfConverterActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        //save the bitmap image
        File root = new File(commonDocumentDirPath("PdfGenerator", PdfConverterActivity.this).toString());
        if (!root.exists()) {
            root.mkdir();
        }
        pdfFile = new File(root, fileName + ".pdf");
//        ArrayList<String> imagesList = Stash.getArrayList(Utils.PARAMS, String.class);
        PDDocument document = new PDDocument();
        PDPageContentStream contentStream = null;
        //   ArrayList<String> arrayList = Stash.getArrayList(Utils.PARAMS, String.class);
        try {

            for (int i = 0; i < imagesList2.size(); i++) {
                Bitmap bitmap = imagesList2.get(i);
                PDPage page = new PDPage(new PDRectangle(bitmap.getWidth(), bitmap.getHeight()));
                Log.d(TAG, "createPdfFile: PDPage page = new PDPage(new PDRectangle(bitmap.getWidth(), bitmap.getHeight()));");
//            PDRectangle pdRectangle = new PDRectangle(9, 9);
//            page.setCropBox(new PDRectangle());
                document.addPage(page);
                Log.d(TAG, "createPdfFile: document.addPage(page);");
                // Define a content stream for adding to the PDF
                contentStream = new PDPageContentStream(document, page);
                Log.d(TAG, "createPdfFile:  PDPageContentStream contentStream");
                Log.e(TAG, "createPdfFile: quality: " + compressValueInt);
                // Here you have great control of the compression rate and DPI on your image.
                // Update 2017/11/22: The DPI param actually is useless as of current version v1.8.9.1 if you take a look into the source code. Compression rate is enough to achieve a much smaller file size.

                PDImageXObject ximage = JPEGFactory.createFromImage(document, bitmap,
                        compressValueInt, 72);
                // You may want to call PDPage.getCropBox() in order to place your image
                // somewhere inside this page rect with (x, y) and (width, height).
                Log.d(TAG, "createPdfFile: PDImageXObject ximage = JPEGFactory.createFromImage");
                contentStream.drawImage(ximage, 0, 0);
                Log.d(TAG, "createPdfFile: contentStream.drawImage(ximage, 0, 0);");

                // Make sure that the content stream is closed:
                contentStream.close();
            }
            // Make sure that the content stream is closed:
            document.save(pdfFile);
            document.close();
//            binding.tvFileName.setText("Saved");
//            Toast.makeText(PdfConverterActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PdfConverterActivity.this, GeneratePdfActivity.class)
                    .putExtra(Utils.PDF_FILE, pdfFile.toString())
                    .putExtra(Utils.FILE_NAME, fileName)
            );
        } catch (Exception e) {

            e.printStackTrace();
            binding.tvFileName.setText(e.getMessage());
            Log.d(TAG, "createPdfFile: ERROR2: " + e.getMessage());
        } finally {
            contentStream.close();
            document.close();
        }


        progressDialog.dismiss();

        refreshGallery(pdfFile);


    }
    private void pdf() {
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


