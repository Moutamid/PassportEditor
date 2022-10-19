package com.codevis.photoimageresizer;

import static com.codevis.photoimageresizer.Utils.commonDocumentDirPath;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.codevis.photoimageresizer.databinding.ActivityAddTextBinding;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnCompressListener;


public class AddTextActivity extends AppCompatActivity {
    ActivityAddTextBinding binding;
    String fileName;
    String Date;
    String Name;
    Boolean state = false;
    Bitmap bitmap;
    AdLoader adLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTextBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Objects.requireNonNull(getSupportActionBar()).setTitle("Name And Date Joiner");
        boolean compress = Stash.getBoolean(Utils.COMPRESS);
        if (compress) return;
        Bundle b = getIntent().getExtras();
        fileName = b.getString("fileName", "nothing");
        Date = b.getString("Date");
        bitmap = loadPicture(fileName);
        binding.tvDate.setText(Date);
        binding.tvName.setText(b.getString("Name"));
        binding.imageView.setImageBitmap(bitmap);
        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        adLoader = new AdLoader.Builder(AddTextActivity.this, getString(R.string.admob_native_id))
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
        removeText();
        adLoader.loadAd(new AdRequest.Builder().build());

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });


    }

    public void saveImage() {
        binding.cardView.setDrawingCacheEnabled(true);
        binding.cardView.buildDrawingCache();
        binding.cardView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        Bitmap bitmap = binding.cardView.getDrawingCache();
        String data = fileName.replaceAll(":", ".") + ".jpg";
        File myFile;
        myFile = new File(commonDocumentDirPath("AddTexts", AddTextActivity.this), data);
        if (myFile.exists()) {
            fileName = fileName + "_1";
            data = fileName.replaceAll(":", ".") + ".jpg";
            myFile = new File(commonDocumentDirPath("AddTexts", AddTextActivity.this), data);
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(myFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            binding.imageView.setDrawingCacheEnabled(false);

            File finalMyFile = myFile;
            Luban.compress(AddTextActivity.this, myFile)
                    .setMaxSize(100)
                    .putGear(Luban.CUSTOM_GEAR)
                    .launch(new OnCompressListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onSuccess(File file) {
                            saveFile(file, finalMyFile);
                        }

                        @Override
                        public void onError(Throwable e) {
                            binding.path.setText(e.getMessage());
                        }
                    });

//            Utils.toast(getApplicationContext(), "Image Edited Succesfully");
        } catch (Exception ex) {
            binding.tvName.setText(ex + "");
        }

    }

    private void saveFile(File fromFile, File toFile) {
        try {
            Uri uri = Uri.fromFile(fromFile);
            Bitmap newBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            FileOutputStream fileOutputStream = new FileOutputStream(toFile);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            binding.path.setText("Saved: " + toFile);

//            fromFile.delete();

            refreshGallery(toFile);
            //TODO: Utils.share(AddTextActivity.this, toFile.getPath(), Utils.TYPE_IMG);

            binding.button.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
            binding.path.setText(e.getMessage());
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

    private void removeText() {
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

    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        }
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    public Bitmap loadPicture(String filename) {
        Bitmap b = null;

        try {
            FileInputStream fis = openFileInput(filename);
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(fis);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            b = BitmapFactory.decodeStream(ois);
            try {
                ois.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return b;
    }
}