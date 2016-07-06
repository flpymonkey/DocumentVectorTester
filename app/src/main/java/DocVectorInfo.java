import com.example.DocMagnitudeTreeMap;

import java.awt.dnd.InvalidDnDOperationException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by nuplavikar on 2/14/16.
 */
public class DocVectorInfo {

    //This should be returned and is important. It stores the "fileId" as key and another treemap<String, float> for
    //<global terms, weights>, where weights is a combination of TF and IDF, as value
    public TreeMap<String, DocMagnitudeTreeMap> docTFIDFVectorTreeMap; // populated after indexer use and
    // filename and vector

    //This stores the "fileId" as key and
    //vector magnitude
    public DocMagnitudeTreeMap docMagnitudeTreeMap;

    public DocVectorInfo()
    {
        docTFIDFVectorTreeMap = new TreeMap<String, DocMagnitudeTreeMap>();
        docMagnitudeTreeMap = new DocMagnitudeTreeMap();
    }

    public int getNumOfDocs()
    {
        return docTFIDFVectorTreeMap.size();
    }

    int updateDocVector(String fileName, DocMagnitudeTreeMap docTFIDFTermVector)
    {
        docTFIDFVectorTreeMap.put(fileName, docTFIDFTermVector);
        return 0;
    }

    int updateDocVectorMagnitude(String fileName,Double docMagnitude)
    {
        docMagnitudeTreeMap.put(fileName, Math.sqrt(docMagnitude));
        return 0;
    }

    public void printDocTFIDFVectorTreeMapAndDocMagnitudeTreeMap()
    {
        Set<String> fileNames= docTFIDFVectorTreeMap.keySet();
        String fileName;
        if( fileNames.equals(docMagnitudeTreeMap.keySet()) == false )
        {
            InvalidDnDOperationException invalidDnDOperationException = new InvalidDnDOperationException("File name integrity wrong!!");
            invalidDnDOperationException.printStackTrace();
            System.exit(1);
        }
        Iterator<String> it = fileNames.iterator();
        int count = 0;
        while( it.hasNext() ) {
            fileName = it.next();
            double magnitude = docMagnitudeTreeMap.get(fileName);
            System.out.println("#"+(++count)+" fileName:"+fileName+" Magnitude:"+magnitude+" terms=weight:"+docTFIDFVectorTreeMap.get(fileName));
        }
    }

    /*
    * We are not using docMagnitudeTreeMap for getting magnitude of the document because we already know it during vector creation stage
    * hence no need to find it for each document - Optimization step*/
    public int normalizeAndScaleUpVector(String fileName, TreeMap<String, Double> docTFIDFTermVector, Set<String> globalTermsSet, double magnitude, int scaleUpFactor)
    {
        Iterator<String> strGlobTermIt = globalTermsSet.iterator();
        double newMagnitude = 0;

        while(strGlobTermIt.hasNext())
        {
            String currentTerm = strGlobTermIt.next();
            double oldTermWeight = docTFIDFTermVector.get(currentTerm);
            double normTermWeight = scaleUpFactor * (oldTermWeight/magnitude);
            docTFIDFTermVector.put(currentTerm, normTermWeight);
            newMagnitude = newMagnitude + (normTermWeight*normTermWeight);

        }
        //Calculate the new magnitude of normalized, scaled-up vector vector. It should be (scaleUpFactor*1)
        newMagnitude = Math.sqrt(newMagnitude);
        docMagnitudeTreeMap.put(fileName, newMagnitude);
        return 0;
    }
}
