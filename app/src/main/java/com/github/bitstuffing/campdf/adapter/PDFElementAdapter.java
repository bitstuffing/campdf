package com.github.bitstuffing.campdf.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.bitstuffing.campdf.R;
import com.github.bitstuffing.campdf.Utils;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDFElementAdapter extends BaseAdapter implements Filterable {

    private List<File> list;
    private Activity activity;
    private Map<String,View> thumbnails;

    int WIDTH = 150;
    int HEIGHT = 150;

    public PDFElementAdapter(List<File> list, Activity activity){
        super();
        this.list = list;
        this.activity = activity;
        this.thumbnails = new HashMap<String,View>();
    }

    @Override
    public int getCount() {
        return list !=null ? list.size(): 0;
    }

    @Override
    public Object getItem(int i) {
        return list != null ? list.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view !=null && thumbnails.containsKey(list.get(i).getName())){ //search in cached
            ((ViewGroup)view.getParent()).removeView(view); //needs this tip to avoid invalid view identifier
            View row = thumbnails.get(list.get(i).getName());
            return row;
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.custom,null);
        TextView title, detail;
        ImageView i1;
        title = (TextView) row.findViewById(R.id.title);
        detail = (TextView) row.findViewById(R.id.detail);
        title.setText(list.get(i).getName());
        detail.setText(list.get(i).getName());
        i1=(ImageView)row.findViewById(R.id.img);
        try{
            i1.setImageBitmap(getThumbnailFromPDF(list.get(i).getName())); //get thumbnail
        }catch(IOException e){
            e.printStackTrace(); //TODO log
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //File file = new File(((TextView)view.findViewById(R.id.detail)).getText().toString());
                try {
                    Utils.openPDFFile(((TextView)view.findViewById(R.id.detail)).getText().toString(),activity);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        /*
        row.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
                registerForContextMenu(view);
                //TODO OpenContextMenu
                return false;
            }
        });*/
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                return false;
            }
        });
        //TODO cached but is not a valid solution, needs more control (like renamed or deleted)
        thumbnails.put(list.get(i).getName(),row);
        return row;
    }

    private Bitmap getThumbnailFromPDF(String s) throws IOException {
        try{
            PDDocument pdf = PDDocument.load(new File(activity.getFilesDir().getAbsolutePath()+File.separatorChar+s));
            //PDPage pdpage = (PDPage) pdf.getDocumentCatalog().getPages().get(0);
            PDFRenderer renderer = new PDFRenderer(pdf);
            return  Bitmap.createScaledBitmap(renderer.renderImage(0), WIDTH, HEIGHT, true);
        }catch(Exception e){
            Bitmap bitmap = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.pdf)).getBitmap();
            Matrix matrix = new Matrix();
            //matrix.postScale(50,50);
            return Bitmap.createScaledBitmap(bitmap,WIDTH/3, HEIGHT/3 ,true);
        }
    }

    @Override
    public Filter getFilter(){
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<File> list = Utils.fillFileList(activity);

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();

                if (constraint != null && constraint.toString().length() > 0) {
                    List<File> founded = new ArrayList<File>();
                    for (File item : list) {
                        if (item.getName().toLowerCase().contains(constraint)) {
                            founded.add(item);
                        }
                    }

                    result.values = founded;
                    result.count = founded.size();
                } else {
                    result.values = list;
                    result.count = list.size();
                }
                return result;
            }


            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                list.clear();
                for (File item : (List<File>) results.values) {
                    list.add(item);
                }
                notifyDataSetChanged();
            }
        };
    }


    @Override
    public int getViewTypeCount() {
        if(getCount() > 0){
            return getCount();
        }else{
            return super.getViewTypeCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}
