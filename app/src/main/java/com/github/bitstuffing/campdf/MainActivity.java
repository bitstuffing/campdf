package com.github.bitstuffing.campdf;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.github.bitstuffing.campdf.databinding.ActivityMainBinding;
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

public class MainActivity extends AppCompatActivity implements ISignals{

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    protected static Handler messageHandler;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    ProgressDialog loadingDialog;
    AlertDialog.Builder alertDialog;

    //pdf size (600 dpi - A4 sized), enough quality at this moment. TODO put in settings
    private static final int A4_WIDTH = 2480;
    private static final int A4_HEIGHT = 3508;

    protected Bitmap bitmap = null;
    protected PDPageContentStream contentStream = null;
    protected PDDocument document = null;
    protected PDPage page = null;
    protected File file = null;

    private void init(){
        // handler
        messageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                int code = bundle.getInt("code",0);
                String message = bundle.getString("message","");
                switch(code){
                    case LOADING:
                        if(loadingDialog == null){
                            loadingDialog = new ProgressDialog(MainActivity.this);
                            loadingDialog.setMessage("Loading..");
                        }
                        loadingDialog.setTitle(message);
                        loadingDialog.setIndeterminate(true);
                        loadingDialog.setCancelable(false);
                        loadingDialog.show();
                        break;
                    case LOADED:
                        loadingDialog.dismiss();
                        loadingDialog = null;
                        break;
                    case SHOW_ALERT:
                        if(alertDialog != null){
                            alertDialog.show();
                        }
                        break;
                    case ERROR_MESSAGE:
                        logger.log(Level.WARNING, message);
                        if(loadingDialog !=null){
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "ERROR: "+code+" | "+message, Toast.LENGTH_SHORT).show();
                        break;
                    case REFRESH_LISTVIEW:
                        //TODO
                        break;
                    case INFO_MESSAGE:
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

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
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
        if (!sharedPreferences.getBoolean(OnBoardingFragment.COMPLETED_ONBOARDING_PREF_NAME, false)) {
            startActivity(new Intent(this, WelcomeActivity.class));
        }

        //Utils.checkPermissions(this);
        init();

        //set button event
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callToIntent();
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
        if (requestCode == REQUEST_CODE) {
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
                            drawPdf(data,"scanned-"+System.currentTimeMillis()+".pdf");
                            sendMessage(REFRESH_LISTVIEW); //TODO check
                        }
                    });
                    sendMessage(SHOW_ALERT);
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
                                askToSaveDocument("scanned-"+System.currentTimeMillis()+".pdf");
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally{
                                document = null;
                                sendMessage(REFRESH_LISTVIEW);
                            }
                        }
                    });
                    sendMessage(SHOW_ALERT);
                }
            }
        }
    }

    private void drawPdf(Intent intent,String fileName) {
        try {
            //init
            sendMessage(LOADING,"creating document...");
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
        bitmap = Bitmap.createScaledBitmap(bitmap, A4_WIDTH,A4_HEIGHT, true);
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
                        sendMessage(ERROR_MESSAGE,"Exception at: "+file.getAbsolutePath());
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
            sendMessage(SHOW_ALERT);
        }else{
            saveDocument();
        }
    }

    private void saveDocument() throws IOException{
        if(file.createNewFile()) {
            sendMessage(LOADING,"saving...");
            document.save(file.getAbsoluteFile());
            document.close();
            sendMessage(LOADED);
            sendMessage(INFO_MESSAGE,"saved at: "+file.getAbsolutePath());
        }else{
            sendMessage(ERROR_MESSAGE,"fail at: "+file.getAbsolutePath());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

}