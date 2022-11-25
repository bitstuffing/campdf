package com.github.bitstuffing.campdf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.bitstuffing.campdf.databinding.FragmentMainBinding;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private SwipeActionAdapter adapter;
    private ListView pdfListView;
    private static MainFragment fragment;

    private CustomSwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyLayout;

    public static MainFragment newInstance() {
        fragment = new MainFragment();
        return fragment;
    }

    public MainFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        binding = FragmentMainBinding.inflate(inflater, container, false);
        FrameLayout view = binding.getRoot();
        pdfListView = (ListView) view.findViewById(R.id.pdfListView);

        //layout
        emptyLayout = (LinearLayout) view.findViewById(R.id.emptyContainer);

        fillListView();
        //pdfListView.setStackFromBottom(false); //push list elements starting from bottom

        swipeRefreshLayout = ( CustomSwipeRefreshLayout ) view.findViewById( R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                fillListView();
                swipeRefreshLayout.setRefreshing(false); //dismiss spinner draw
            }

        });

        swipeRefreshLayout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return false;
            }
        });

        return view;

    }

    public void fillListView() {
        List<File> list = new ArrayList<File>();
        File filePath = getActivity().getFilesDir();
        for(int i=0;i<filePath.listFiles().length;i++){
            list.add(filePath.listFiles()[i]);
        }
        BaseAdapter tempAdapter = new PDFElementAdapter(list, getActivity());
        adapter = new SwipeActionAdapter(tempAdapter);
        adapter.addBackground(SwipeDirection.DIRECTION_FAR_LEFT,R.layout.row_bg_left_far)
                .addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
                .addBackground(SwipeDirection.DIRECTION_FAR_RIGHT,R.layout.row_bg_right_far)
                .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);
        adapter.setSwipeActionListener(new SwipeActionAdapter.SwipeActionListener(){

                @Override
                public boolean hasActions(int position, SwipeDirection direction){
                    if(direction.isLeft()) return true;
                    if(direction.isRight()) return true;
                    return false;
                }

                @Override
                public boolean shouldDismiss(int position, SwipeDirection direction){
                    return direction == SwipeDirection.DIRECTION_NORMAL_LEFT;
                }

                @Override
                public void onSwipe(int[] positionList, SwipeDirection[] directionList) {
                    for(int i=0;i<positionList.length;i++) {
                        SwipeDirection direction = directionList[i];
                        int position = positionList[i];
                        String dir = "";

                        switch (direction) {
                            case DIRECTION_FAR_LEFT:
                                dir = "Far left";
                                removeElement(position);
                                break;
                            case DIRECTION_NORMAL_LEFT:
                                dir = "Left";
                                renameElement(position);
                                break;
                            case DIRECTION_FAR_RIGHT:
                                dir = "Far right";
                                shareElementWith(position);
                                break;
                            case DIRECTION_NORMAL_RIGHT:
                                dir = "Right";
                                copyFileToDownloads(position);
                                break;
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            })
            .setDimBackgrounds(true)
            .setListView(pdfListView);

        pdfListView.setAdapter(adapter);

        pdfListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        if(adapter.getCount() == 0){
            emptyLayout.setVisibility(View.VISIBLE);
        }else{
            emptyLayout.setVisibility(View.INVISIBLE);
        }
    }

    private void copyFileToDownloads(int i){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }
        final File dir = getActivity().getFilesDir();
        final File origin = dir.listFiles()[i];
        try{
            FileUtils.copyFile(origin,new File(path,origin.getName()));
            MainActivity.sendMessage(ISignals.INFO_MESSAGE,"Copied to downloads with name: "+origin.getName());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void renameElement(int i) {
        final File dir = getActivity().getFilesDir();
        final File target = dir.listFiles()[i];
        //final File target = new File(dir,list.get(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Rename");
        final EditText inputText = new EditText(getActivity());
        inputText.setText(target.getName());
        inputText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputText);
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String box = inputText.getText().toString();
                target.renameTo(new File(dir,box));
                fillListView();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void shareElementWith(int i) {
        File dir = getActivity().getFilesDir();
        final String fileName = dir.listFiles()[i].getName();
        //final String fileName = list.get(i);
        Utils.shareUri(fileName,getActivity());
    }

    private void removeElement(int i) {
        final File dir = getActivity().getFilesDir();
        final File target = dir.listFiles()[i];
        //final File target = new File(dir,list.get(i));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete");
        builder.setMessage("Do you want to delete "+target.getName()+" file?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                target.delete();
                fillListView();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        ((MainActivity)getActivity()).setAlertDialog(builder);
        ((MainActivity)getActivity()).sendMessage(ISignals.SHOW_ALERT);
        //builder.show();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(swipeRefreshLayout!=null){
            fillListView();
            //first launch load event
            swipeRefreshLayout.setRefreshing(true);
            //next dismiss spinner
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}