package com.example.android.documentvectortester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.DocMagnitudeTreeMap;
import com.example.DocVectorInfo;
import com.example.GenerateTFIDFVector;
import com.example.Indexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    //take indexDir place to store index files, dataDir - is place of exisiting doc
    private String indexDir = "indices";
    private String dataDir = "assets";
    // change to correct index values within assets folder
    private String queryDocName = "???";
    private ListView lv = (ListView) findViewById(R.id.main_list);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //send in map of file name
        //print file name then terms and tfidf (frequncy)
        try {
            //create indexer (main function as constructor)
            //take indexDir place to store index files, dataDir - is place of exisiting doc
            Indexer.main(indexDir, dataDir); // change to correct index values within assets folder
        } catch (Exception e) {
            e.printStackTrace(); // punt, show exception trace
        }


        GenerateTFIDFVector vGen = new GenerateTFIDFVector();
        DocVectorInfo vInfo = new DocVectorInfo();
        try {
            vInfo = vGen.getDocTFIDFVectors(indexDir,queryDocName);
        } catch (IOException e) {
            e.printStackTrace(); // punt, show exception trace
        }

        // get the treemap that is populated with document vector data
        TreeMap<String, DocMagnitudeTreeMap> fileNameVec = vInfo.docTFIDFVectorTreeMap;

        Iterator it1 = fileNameVec.keySet().iterator();
        ArrayList<String> filenames = new ArrayList<String>();
        while (it1.hasNext()){
            filenames.add((String)it1.next()); // will always be Strings
        }

        //Adaptors are used to prepare information to be displayed on the specified view
        // middle term is text view within list
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filenames);

        //simple adapter for headers
        lv.setAdapter(arrayAdapter);
        // term weight for word num weight times inverse document frequency scaled up to 10^4 (row int value)
    }
}
