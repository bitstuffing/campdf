package com.github.bitstuffing.campdf;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.github.bitstuffing.campdf.adapter.PDFElementAdapter;
import com.github.bitstuffing.campdf.databinding.ActivityMainBinding;
import com.github.bitstuffing.campdf.fragment.OnBoardingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity{

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    protected static Handler messageHandler;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static ProgressDialog loadingDialog;
    private static AlertDialog.Builder alertDialog;

    //pdf size (600 dpi - A4 sized), enough quality at this moment. TODO put in settings
    private static final int A4_WIDTH = 2480;
    private static final int A4_HEIGHT = 3508;

    protected Bitmap bitmap = null;
    protected PDPageContentStream contentStream = null;
    protected PDDocument document = null;
    protected PDPage page = null;
    protected File file = null;

    public static void setAlertDialog(AlertDialog.Builder alert){
        alertDialog = alert;
    }

    private void init(){
        // handler
        messageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                int code = bundle.getInt("code",0);
                String message = bundle.getString("message","");
                switch(code){
                    case ISignals.LOADING:
                        if(loadingDialog == null){
                            loadingDialog = new ProgressDialog(MainActivity.this);
                            loadingDialog.setMessage("Loading..");
                        }
                        loadingDialog.setTitle(message);
                        loadingDialog.setIndeterminate(true);
                        loadingDialog.setCancelable(false);
                        loadingDialog.show();
                        break;
                    case ISignals.LOADED:
                        loadingDialog.dismiss();
                        loadingDialog = null;
                        break;
                    case ISignals.SHOW_ALERT:
                        if(alertDialog != null){
                            alertDialog.show();
                        }
                        break;
                    case ISignals.ERROR_MESSAGE:
                        logger.log(Level.WARNING, message);
                        if(loadingDialog !=null){
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "ERROR: "+code+" | "+message, Toast.LENGTH_SHORT).show();
                        break;
                    case ISignals.REFRESH_LISTVIEW:
//TODO, check if it's necessary (this signal), if yes provide listview in getListView()
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                getListView().getAdapter().notifyDataSetChanged();
//                            }
//                        });
                        break;
                    case ISignals.INFO_MESSAGE:
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "code: "+code+" | "+message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        //init pdf
        PDFBoxResourceLoader.init(getApplicationContext());

        //init layout
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

    }

    public static void sendMessage(int code,String message) {
        Message messageToBeSent = new Message();
        Bundle bundle = new Bundle();
        bundle.putInt("code",code);
        bundle.putString("message",message);
        messageToBeSent.setData(bundle);
        messageHandler.sendMessage(messageToBeSent);
    }

    public static void sendMessage(int code){
        sendMessage(code,"");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Utils.setTheme(sharedPreferences);

        if (!sharedPreferences.getBoolean(OnBoardingFragment.COMPLETED_ONBOARDING_PREF_NAME, false)) {
            startActivity(new Intent(this, WelcomeActivity.class));
        }
        init();
        //set button event
        ((FloatingActionButton)binding.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callToIntent();
            }
        });
        if (!sharedPreferences.getBoolean(OnBoardingFragment.COMPLETED_TUTORIAL, false)) {
            drawGuide();
        }
        ((BottomNavigationItemView)findViewById(R.id.searchButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                EditText v = ((EditText)findViewById(R.id.filterText));
                ListView pdfListView = ((ListView) findViewById(R.id.pdfListView));
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)pdfListView.getLayoutParams();
                layoutParams.setMargins(0,(int) Utils.convertDpToPixel(56,MainActivity.this),0,0);
                pdfListView.setLayoutParams(layoutParams);
                ((FrameLayout) findViewById(R.id.searchFrameLayout)).setVisibility(View.VISIBLE);
                v.requestFocusFromTouch();
                v.requestFocus();
            }
        });
        ((ImageView) findViewById(R.id.filterCloseImage)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((FrameLayout) findViewById(R.id.searchFrameLayout)).setVisibility(View.INVISIBLE);
                ListView pdfListView = ((ListView) findViewById(R.id.pdfListView));
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)pdfListView.getLayoutParams();
                layoutParams.setMargins(0,0,0,0);
                pdfListView.setLayoutParams(layoutParams);
            }
        });

        ((BottomNavigationItemView) findViewById(R.id.settingsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });
    }

    private void drawGuide() {
        TapTargetView.showFor(this,
            TapTarget.forView(findViewById(R.id.fab), "Make a photo", "Create your PDF documents starting here")
                .outerCircleColor(R.color.red)
                .outerCircleAlpha(0.96f)
                .targetCircleColor(R.color.white)
                .titleTextSize(24)
                .titleTextColor(R.color.white)
                .descriptionTextSize(16)
                .descriptionTextColor(R.color.red)
                .textColor(R.color.blue)
                .textTypeface(Typeface.SANS_SERIF)
                .dimColor(R.color.black)
                .drawShadow(true)
                .cancelable(false)
                .tintTarget(true)
                .transparentTarget(false)
                .icon( getResources().getDrawable(android.R.drawable.ic_menu_camera))
                .targetRadius(60),
            new TapTargetView.Listener() {
                @Override
                public void onTargetClick(TapTargetView view) {
                    super.onTargetClick(view);
                    TapTargetView.showFor(MainActivity.this,
                        TapTarget.forView(findViewById(R.id.settingsButton), "Customize", "Choose your quality scanned pages")
                            .outerCircleColor(R.color.green)
                            .outerCircleAlpha(0.96f)
                            .targetCircleColor(R.color.white)
                            .titleTextSize(24)
                            .titleTextColor(R.color.white)
                            .descriptionTextSize(16)
                            .descriptionTextColor(R.color.black)
                            .textColor(R.color.blue)
                            .textTypeface(Typeface.SANS_SERIF)
                            .dimColor(R.color.black)
                            .drawShadow(true)
                            .cancelable(false)
                            .tintTarget(true)
                            .transparentTarget(false)
                            .icon( getResources().getDrawable(R.drawable.ic_baseline_settings_24))
                            .targetRadius(60),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                SharedPreferences preferences = getDefaultSharedPreferences(MainActivity.this);
                                preferences.edit().putBoolean(OnBoardingFragment.COMPLETED_TUTORIAL,true).apply();
                            }
                        });
                }
            });
    }


    protected void callToIntent() {
        Intent intent = new Intent(MainActivity.this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, ScanConstants.OPEN_CAMERA);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
        startActivityForResult(intent, ScanConstants.START_CAMERA_REQUEST_CODE, options.toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ISignals.REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                if(document == null){
                    //define first and unique dialog workflow
                    alertDialog = new AlertDialog.Builder(this);
                    alertDialog.setTitle("Do you want to add more pages?");
                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            document = new PDDocument();
                            try {
                                addPageToDocument(data);
                                callToIntent();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(getApplicationContext().getPackageName()+ "_preferences", Context.MODE_PRIVATE);
                            String filePrefix = sharedPreferences.getString(SettingsActivity.FILE_PREF_NAME,getString(R.string.prefix_default));
                            drawPdf(data,filePrefix+"-"+System.currentTimeMillis()+".pdf");
//                            sendMessage(ISignals.REFRESH_LISTVIEW); //TODO check
                            PDFElementAdapter.sendMessage(ISignals.REFRESH,"");
                        }
                    });
                    sendMessage(ISignals.SHOW_ALERT);
                }else{
                    //define recursive dialog workflow
                    alertDialog = new AlertDialog.Builder(this);
                    alertDialog.setTitle("Added, do you want to add more pages?");
                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                addPageToDocument(data);
                                callToIntent();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                addPageToDocument(data);
                                SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(getApplicationContext().getPackageName()+ "_preferences", Context.MODE_PRIVATE);
                                String filePrefix = sharedPreferences.getString(SettingsActivity.FILE_PREF_NAME,getString(R.string.prefix_default));
                                askToSaveDocument(filePrefix+"-"+System.currentTimeMillis()+".pdf");
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally{
                                document = null;
//                                sendMessage(ISignals.REFRESH_LISTVIEW);
//                                PDFElementAdapter.sendMessage(ISignals.REFRESH,""); //TODO check if this line is mandatory or not, I think with the new thread not but needs some checks for unknown ways in the workflow
                            }
                        }
                    });
                    sendMessage(ISignals.SHOW_ALERT);
                }
            }
        }
    }

    private void drawPdf(Intent intent,String fileName) {
        try {
            //init
            sendMessage(ISignals.LOADING,"creating document...");
            document = new PDDocument();
            addPageToDocument(intent);
            //storage part
            askToSaveDocument(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            document = null;
        }
    }

    private void addPageToDocument(Intent intent) throws IOException {
        //get bitmap url
        Uri uri = intent.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
        page = new PDPage();
        document.addPage(page);

        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        contentStream = new PDPageContentStream(document, page);

        SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(getApplicationContext().getPackageName()+ "_preferences", Context.MODE_PRIVATE);
        String qualityPreference = sharedPreferences.getString(SettingsActivity.QUALITY,getString(R.string.prefix_default));
        int width = qualityPreference.equals("high_quality")? A4_WIDTH: A4_WIDTH / 3;
        int height = qualityPreference.equals("high_quality")? A4_HEIGHT: A4_HEIGHT / 3;

        bitmap = Bitmap.createScaledBitmap(bitmap, width,height, true);
        PDImageXObject alphaXimage = /*LosslessFactory*/JPEGFactory.createFromImage(document,bitmap);
        contentStream.drawImage(alphaXimage, 0, 0 , page.getBBox().getWidth(), page.getBBox().getHeight());

        contentStream.close();

        page = null;
    }

    private void askToSaveDocument(String fileName) throws IOException {
        String path = getFilesDir().getAbsolutePath();
        file = new File(path+File.separatorChar+fileName);
        logger.log(Level.INFO,file.getAbsolutePath());
        if (file.exists()){ //ask to replace
            alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Do you want to replace old file?");
            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        file.delete();
                        saveDocument();
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMessage(ISignals.ERROR_MESSAGE,"Exception at: "+file.getAbsolutePath());
                    }
                }
            });
            alertDialog.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //EMPTY
                        }
                    }
            );
            sendMessage(ISignals.SHOW_ALERT);
        }else{
            saveDocument();
        }
    }

    private void saveDocument() throws IOException{
        if(file.createNewFile()) {
            sendMessage(ISignals.LOADING,"saving...");
            document.save(file.getAbsoluteFile());
            document.close();
            sendMessage(ISignals.LOADED);
            sendMessage(ISignals.INFO_MESSAGE,"Generated: "+file.getName());
        }else{
            sendMessage(ISignals.ERROR_MESSAGE,"Failed at: "+file.getAbsolutePath());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bottom_nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.settingsButton) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onRestart() {
        this.recreate();
        super.onRestart();
    }

}