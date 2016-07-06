package com.example.android.documentvectortester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.example.android.documentvectortester.Adapter.ExpandListAdapter;
import com.example.android.documentvectortester.ExpandListChild.ExpandListChild;
import com.example.android.documentvectortester.ExpandListGroup.ExpandListGroup;

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
    //private ListView lv = (ListView) findViewById(R.id.main_list); //switced to expand list view

    private ExpandListAdapter ExpAdapter;
    private ArrayList<ExpandListGroup> ExpListItems;
    private ExpandableListView ExpandList;

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

        ExpandList = (ExpandableListView) findViewById(R.id.main_list);
        ExpListItems = SetStandardGroups();
        ExpAdapter = new ExpandListAdapter(MainActivity.this, ExpListItems);
        ExpandList.setAdapter(ExpAdapter);

        //Adaptors are used to prepare information to be displayed on the specified view
        // middle term is text view within list
        //ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filenames);

        //simple adapter for headers
        //lv.setAdapter(arrayAdapter);
        // term weight for word num weight times inverse document frequency scaled up to 10^4 (row int value)
    }

    // Example for UI
    public ArrayList<ExpandListGroup> SetStandardGroups() { //Uses external Adaptor, ExpandListChild, and ListGroup
        ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
        ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
        ExpandListGroup gru1 = new ExpandListGroup();
        gru1.setName("Comedy");
        ExpandListChild ch1_1 = new ExpandListChild();
        ch1_1.setName("A movie");
        ch1_1.setTag(null);
        list2.add(ch1_1);
        ExpandListChild ch1_2 = new ExpandListChild();
        ch1_2.setName("An other movie");
        ch1_2.setTag(null);
        list2.add(ch1_2);
        ExpandListChild ch1_3 = new ExpandListChild();
        ch1_3.setName("And an other movie");
        ch1_3.setTag(null);
        list2.add(ch1_3);
        gru1.setItems(list2);
        list2 = new ArrayList<ExpandListChild>();

        ExpandListGroup gru2 = new ExpandListGroup();
        gru2.setName("Action");
        ExpandListChild ch2_1 = new ExpandListChild();
        ch2_1.setName("A movie");
        ch2_1.setTag(null);
        list2.add(ch2_1);
        ExpandListChild ch2_2 = new ExpandListChild();
        ch2_2.setName("An other movie");
        ch2_2.setTag(null);
        list2.add(ch2_2);
        ExpandListChild ch2_3 = new ExpandListChild();
        ch2_3.setName("And an other movie");
        ch2_3.setTag(null);
        list2.add(ch2_3);
        gru2.setItems(list2);
        list.add(gru1);
        list.add(gru2);

        return list;
    }

}
