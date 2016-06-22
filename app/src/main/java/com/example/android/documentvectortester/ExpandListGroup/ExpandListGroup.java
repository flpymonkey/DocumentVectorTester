package com.example.android.documentvectortester.ExpandListGroup; /**
 * Created by ben on 6/21/2016.
 */
import com.example.android.documentvectortester.ExpandListChild.ExpandListChild;

import java.util.ArrayList;

public class ExpandListGroup {

    private String Name;
    private ArrayList<ExpandListChild> Items;

    public String getName() {
        return Name;
    }
    public void setName(String name) {
        this.Name = name;
    }
    public ArrayList<ExpandListChild> getItems() {
        return Items;
    }
    public void setItems(ArrayList<ExpandListChild> Items) {
        this.Items = Items;
    }
}
