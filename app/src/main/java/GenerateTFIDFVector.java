import com.example.DocVectorInfo;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Math.log;

public class GenerateTFIDFVector {

    //This treemap stores the IDF value for all the terms found in the collection
    public DocMagnitudeTreeMap globalTermIDFTreeMap;

    public DocVectorInfo docVectorInfo;

    public Set<String> globalTermsSet; // made global by BEN -------------------------------------------------

    public String contentsFieldName = "contents";
    public String fileNamesFieldName = "filename";
    private int scalingFactor;
	private String tempUnEncrVectFileName = "unEncrQ.txt";
    private String tempUnEncrBinVectFileName = "unEncrBinQ.txt";
    //TODO NOTE: This can be a common parameter for both nodes
    private int minTokenLength = 3;
    EncryptNativeC nativeCGMPCmbndLib;


	public GenerateTFIDFVector()
    {
        globalTermIDFTreeMap = new DocMagnitudeTreeMap();
        docVectorInfo = new DocVectorInfo();
        scalingFactor = 10000; // scaling vactor for final output TODO ------------------------------------------------------
        nativeCGMPCmbndLib = new EncryptNativeC();
    }

    public int modifyScalingFactor(int a)
    {
        this.scalingFactor = a;
        return 0;
    }

    public int getScalingFactor(int a)
    {
        return this.scalingFactor;
    }

    public int scaleUpTermWt(double termWeight)
    {
        return (int)(termWeight*scalingFactor);
    }

    public double squareScaleDownTermWt(int scaledUpTermWeight)
    {
        return (double)(scaledUpTermWeight/scalingFactor/scalingFactor);
    }

    /*queryDocName =
                        null when you want ot build vectors for all documents
                        Query File Name Only not including path for building query vector*/
                        // queryDocName = null to use for all documents
    public DocVectorInfo getDocTFIDFVectors(String indexDir, String queryDocName) throws IOException { // run to get vectors TODO ----------------- After indexer is called
        // write your code here
        //String indexDir = args[0];
        //String filename = args[1];

        String fileName;
        int buildingVectForAllDoc = 0;
        if ( queryDocName == null )
        {
            buildingVectForAllDoc = 1;
        }
        //TODO NOTE: Important to set the correct minimum token length.

        AtomicReader indexAtomicReader = null;
        double freq = 0, termWt = 0, docMagnitude = 0;
        try {
            File pathToIndex = new File(indexDir);
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(pathToIndex));
            List<AtomicReaderContext> atomicReaderContextList = indexReader.leaves();
            if (atomicReaderContextList.isEmpty()) {
                System.err.println("leafReaderContextList is empty!! ERROR!!");
                System.exit(2);
            }
            indexAtomicReader = atomicReaderContextList.iterator().next().reader();
            if (indexAtomicReader == null)
            {
                Exception e = new Exception("indexAtomicReader == null!!");
                e.printStackTrace();
                System.exit(3);
            }


        } catch (IndexNotFoundException e)
        {
            System.out.println("No files found in the index specified in directory = "+indexDir);
            System.exit(1);
        }

        int n =  indexAtomicReader.numDocs();


        //System.out.println("Total number of indexed documents found = "+n);
        //#Get the global terms
        Terms globalTerms = indexAtomicReader.terms(contentsFieldName); // contents is field with file
        long globalTermsSz = globalTerms.size();

        //Fill in all the terms as key into a TreeMap with the corresponding value as a idf

        TermsEnum iGlobalTerm = globalTerms.iterator(null); // null value
        BytesRef bytesRef;
        //Here get all the terms in the collection and store their collection level property of the IDFs
        while ( (bytesRef = iGlobalTerm.next())!=null )
        {
			if ( bytesRef.utf8ToString().length() >= minTokenLength )
            {
            	//TODO:REVIEW: Using logarithm of the IDF
            	double IDF = (double) (log((((double) n / (indexAtomicReader.docFreq(new Term(contentsFieldName, bytesRef)) + 1))))+1);
            	globalTermIDFTreeMap.put(bytesRef.utf8ToString(), IDF);
            	//System.out.println(bytesRef.utf8ToString()+"=="+IDF);
			}
        }
        //IDF of entire collection dictionary now stored as a map in termIDFTreeMap

        System.out.println("Total number of unique terms found in the index = "+globalTermsSz);
        System.out.println("Total number of vector dimensions created = "+globalTermIDFTreeMap.size());
        //System.out.println("Size of global termIDFTreeMap = "+globalTermIDFTreeMap.size());
        //printSize(globalTermIDFTreeMap, "globalTermIDFTreeMap");
        //printTerms(indexAtomicReader.terms(contentsFieldName));  //Printing the total number of terms within the index

        for ( int i = 0; i < n; i++  )
        {
            Document doc = indexAtomicReader.document(i);
            IndexableField indexableField = doc.getField(fileNamesFieldName);
            fileName = indexableField.stringValue();
            int buildingQueryVectOnly = 0;
            if (queryDocName != null)
            {
                if (queryDocName.compareTo(this.getActualFileName(fileName)) == 0)
                {
                    buildingQueryVectOnly = 1;
                    System.out.println("Building Vector for query file!");
                }
            }

            if ( (buildingQueryVectOnly == 1) || (buildingVectForAllDoc == 1) )
            {
                //System.out.println("#"+(i+1)+">"+fileNamesFieldName+": "+fileName);
                Fields fields = indexAtomicReader.getTermVectors(i);
                //Iterator<String> docFieldNameIterator =  fields.iterator();
                Terms locDocTerms = fields.terms(contentsFieldName);

                //System.out.println("\t\tTotal number of unique terms in file:" + doc.get("filename") + " = " + locDocTerms.size());

                //Create a treeMap to hold the document's tf-idf vector
                //First create a vector(TreeMap) equal to the global dictionary size dimensions
                //Creation can be done initially by copying the globalTermIDF as it is and then multiplying it with TF

                //docTFIDFTermVector is the mapping for the term and its weight(i.e. freq*idf). This gets created
                //for every document.
                DocMagnitudeTreeMap docTFIDFTermVector = new DocMagnitudeTreeMap(globalTermIDFTreeMap);
                docMagnitude = 0;
                //System.out.println("Size of docTFIDFTermVector = "+docTFIDFTermVector.size());
                //printSize(docTFIDFTermVector, "docTFIDFTermVector");

                //Looping over all the global space dimensions
                //TODO: remove this line: TermsEnum termsEnum = locDocTerms.iterator(null);
                globalTermsSet = globalTermIDFTreeMap.keySet();
                int count = 1;

                Iterator<String> termStrIt = globalTermsSet.iterator();
                while (termStrIt.hasNext())
                {
                    DocsEnum postingsEnum; // changed Postings enum to docs enum for old lucene
                    String curTerm = termStrIt.next();
                    Term term = new Term(contentsFieldName, curTerm);
                    postingsEnum = indexAtomicReader.termDocsEnum(term); // changed Postings enum to docs enum for old lucene
                    //String termInDocs = "";
                    //String termInDocsPostingEnumEntry = "";

                    int postingEntry = postingsEnum.nextDoc();
                    int postingLstLngth = 0;
                    boolean isTermInDoc = false;
                    freq = 0;
                    while (postingEntry != PostingsEnum.NO_MORE_DOCS)
                    {
                        //termInDocs = termInDocs + postingsEnum.docID() + "; ";
                        //termInDocsPostingEnumEntry  = termInDocsPostingEnumEntry + postingEntry + "; ";
                        if (postingsEnum.docID() == i)
                        {

                            //System.out.println("\n\n"+count+"> term = '"+termBytesRef.utf8ToString()+"' :: freq = "+postingsEnum.freq());
                            freq = postingsEnum.freq();
                            count = count + 1;
                            isTermInDoc = true;

                        }
                        postingEntry = postingsEnum.nextDoc();
                        postingLstLngth++;
                    }
                    termWt = freq * globalTermIDFTreeMap.get(curTerm);
                    //termWt = scaleUpTermWt(termWt);
                    docTFIDFTermVector.put(curTerm, termWt);
                    //Calculating the magnitude for the tfidf vector of the document
                    docMagnitude = docMagnitude + (termWt * termWt);


                    //System.out.println("\n\t\tIndexLeafReader.docFreq = "+indexAtomicReader.docFreq(term)+";");
                    //System.out.println("\t\tLucene Int. Documents idx. containing term = "+ termInDocs+"\t\tPostingsEnumLength = "+postingLstLngth);
                    //TODO Remove this line!! //System.out.println("\tTerm Frequency(termInDocsPostingEnumEntry) = "+ termInDocsPostingEnumEntry);
                }
                //System.out.println(docTFIDFTermVector);


                //Put each document's tfidf vector and magnitude in two different TreeMaps - docTFIDFVectorTreeMap and
                //docMagnitudeTreeMap respectively.
                docVectorInfo.updateDocVector(fileName, docTFIDFTermVector);
                //docTFIDFVectorTreeMap.put(fileName, docTFIDFTermVector);
                docMagnitude = (double) Math.sqrt(docMagnitude);
                docVectorInfo.updateDocVectorMagnitude(fileName, docMagnitude);
                //docMagnitudeTreeMap.put(fileName, Math.sqrt(docMagnitude));
                docVectorInfo.normalizeAndScaleUpVector(fileName, docTFIDFTermVector, globalTermsSet, docMagnitude, scalingFactor);

                //System.out.println("Doc #" + (i + 1) + " Document magnitude = " + docVectorInfo.docMagnitudeTreeMap.get(fileName) + "\n");


                docVectorInfo.printDocTFIDFVectorTreeMapAndDocMagnitudeTreeMap();
            }
             if ( buildingQueryVectOnly == 1 )
             {
                 //We have done building the query vector and that was the only task. Now break.
                 System.out.println("Query Vector Built");
                 break;
             }
        }

        return docVectorInfo;
    }

    private static void printSize(Object a, String msg)
    {
        //System.out.println("DEBUG!!--->> "+"size = "+ObjectSizeFetcher.sizeof(a)+msg);

    }

    public String getActualFileName(String largeFileName)
    {
        if ((largeFileName.lastIndexOf("/") < 0))
        {
            return largeFileName;
        }
        return largeFileName.substring(largeFileName.lastIndexOf("/")+1);
    }

    /*
    * This function would, for a given file terms and their weights, create the corresponding TFIDF and its binary
    * vector representation files.
    * */

    public int createTFIDFAndBinaryVectFile(Set<String> dictionary, String TFIDFVectFileName, String BinVectFileName,
                                            DocMagnitudeTreeMap docWeightInfo)
    {
        int err = 0;
        Iterator<String> termStrIt = dictionary.iterator();
        //File open
        File fp = new File(TFIDFVectFileName);
        File fp_bin = new File(BinVectFileName);
        FileWriter fileWriter = null;
        try
        {
            fileWriter = new FileWriter(fp);

            FileWriter fileWriterBin = new FileWriter(fp_bin);


            int count = 0;

            while(termStrIt.hasNext())
            {
                String term = termStrIt.next();
                String ifNewLine = "";
                int bin;
                double weight = docWeightInfo.get(term);
                if ((dictionary.size()!=1) && (count < dictionary.size() - 1) )
                {
                    ifNewLine = "\n";
                }
                fileWriter.write(((int) weight) + ifNewLine);
                if (weight == 0)
                {bin = 0;}
                else
                {bin = 1;}

                fileWriterBin.write(bin+ifNewLine);
                //System.out.println("Added in " + TFIDFVectFileName + "::" + (count+1) + " " + term + ": " + weight);
                //System.out.println("Added in " + BinVectFileName + "::" + (count + 1) + " " + term + ": " + bin);
                count++;
            }

            //Flush and close the file
            fileWriter.flush();
            fileWriterBin.flush();
            fileWriter.close();
            fileWriterBin.close();

            //TODO: Remove debug statements below
            printDocTermVectors(TFIDFVectFileName);
            //printDocTermVectors(BinVectFileName);
            //System.out.println("\n\n");
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return err;
    }


    /*
    * Takes path as input and writes document vector for all files to directory given by path
    * It should have the encrypted query file name as input. It will calculate
    * the two randomized, encrypted dot products - tfidf and co-ordination factor - and two random numbers for each file
    * and store these following four items
    *
    * random number 1\n
    * randomized, encrypted dot product - tfidf\n
    * random number 2\n
    * randomized, encrypted dot product - binary
    * derandomized factor
    *
    * in a file. Then this function will return a list of filenames containing the four outputs back to the caller
    * for sending these two products for each file to the client as part of multiplication protocol.
    * */
    public LinkedList<String> writeDocVectorsToDirAndComputeSecureDotProducts(int queryNumDim, String path, String absEncrTFIDFQueryFileName, String absEncrBinQueryFileName, String keyFileName) throws IOException {
        LinkedList<String> opListIntermRandAndProdFileNames = new LinkedList<String>();

        int err;

        if(!(new File(path).isDirectory()))
        {
            Exception e = new Exception("Directory Path not valid, path given:"+path);
            e.printStackTrace();
            return null;
        }

        if(docVectorInfo.docTFIDFVectorTreeMap.size()==0)
        {
            Exception e = new Exception("No vectors available for any file");
            e.printStackTrace();
            return null;
        }

        //Obtain the global term list once at the beginning so that output can be generated in the right order for
        //all documents
        Set<String> globalTermsSet = globalTermIDFTreeMap.keySet();
        if ( queryNumDim!= globalTermsSet.size() )
        {
            System.err.println("ERROR! Both the client query dimension size ("+queryNumDim+") and global server " +
                    "collection representation dimension size("+globalTermsSet.size()+") should match");
            return null;
        }
        //Iterate over all the paths
        //System.out.println("No. of dimensions = " +docVectorInfo.docTFIDFVectorTreeMap.size());
        Set<String> docFileNameSet = docVectorInfo.docTFIDFVectorTreeMap.keySet();
        Iterator<String> docFileNameIt = docFileNameSet.iterator();

        while(docFileNameIt.hasNext())
        {

            //Get filename for document indexed from Lucene document structure
            String curDocNameStr = docFileNameIt.next();
            String actualFileName = getActualFileName(curDocNameStr);
            //Create a filename for file having the document's term vector information.
            String newFileNameTFIDF = path + "/dtv_" + actualFileName;
            String newFileNameBin = path + "/dtv_bin_" + actualFileName;

            //Create a file name that will store the intermediate, randomized, encrypted dot products and randomized
            // numbers. These names have to be sent back as part of list.
            String opRandAndProdFile = path + "/interm_rand_encr_result_"+actualFileName;

            //Get the document tfidf term vector contained in the treemap docVectorInfo.docTFIDFVectorTreeMap
            DocMagnitudeTreeMap docTFIDFVectorTreeMap = docVectorInfo.docTFIDFVectorTreeMap.get(curDocNameStr);

            if (this.createTFIDFAndBinaryVectFile(globalTermsSet, newFileNameTFIDF, newFileNameBin, docTFIDFVectorTreeMap)!=0)
            {
                System.err.println("ERROR! In execution of createTFIDFAndBinaryVectFile!");
                new Exception().printStackTrace();
                return null;
            }

            //TODO:DBG line follows
            //System.out.println("Adding the TFIDF vectors for document:"+ curDocNameStr+" in "+ newFileNameTFIDF +" and "+newFileNameBin);
            //Get the iterator to traverse the global term list for this current document.


            if (  (err = nativeCGMPCmbndLib.read_encrypt_vec_from_file_comp_inter_sec_prod(queryNumDim, absEncrTFIDFQueryFileName,
                    absEncrBinQueryFileName, newFileNameTFIDF, newFileNameBin, opRandAndProdFile, keyFileName)) != 0)
            {
                System.out.println("ERROR in calling nativeCGMPCmbndLib's functn. to compute intermediate product values, err:"+err);
                return null;
            }

            opListIntermRandAndProdFileNames.add(opRandAndProdFile);


        }//
        int count = 1;

        //
        return opListIntermRandAndProdFileNames;
    }


	/*
	* Takes path as input and writes document vector for the file  to directory given by path
	* */
	public int writeDocVectorToFile(String relFilename, String outputEncrTFIDFVecFile, String outputEncrBinVecFile, String keyFileName) throws IOException {

        int ret;
		boolean docPresent = false;
        System.out.println("Encrypting Query Vector and writing it to file ...");
        if((new File(relFilename).isDirectory()))
		{
			Exception e = new Exception("ERROR! Required relative filename given directory, name given:"+relFilename);
			e.printStackTrace();
			return 1;
		}

		if(docVectorInfo.docTFIDFVectorTreeMap.size()==0)
		{
			Exception e = new Exception("No vectors available for any file");
			e.printStackTrace();
			return 2;
		}

		//Obtain the global term list once at the beginning so that output can be generated in the right order for
		//all documents
		Set<String> globalTermsSet = globalTermIDFTreeMap.keySet();
		//Iterate over all the paths
		//System.out.println("No. of dimensions = " +docVectorInfo.docTFIDFVectorTreeMap.size());
		Set<String> docFileNameSet = docVectorInfo.docTFIDFVectorTreeMap.keySet();
		Iterator<String> docFileNameIt = docFileNameSet.iterator();

		while(docFileNameIt.hasNext())
		{

			//Get filename for document indexed from Lucene document structure
			String curDocNameStr = docFileNameIt.next();
			//Create a filename for file having the document's term vector information.

			//Write the encrypted vectors to file given by relFileName using the C code
			//System.out.println("Indexed file name: "+getActualFileName(curDocNameStr)+" query document name given: "+relFilename);
			if ( getActualFileName(curDocNameStr).compareTo(relFilename) == 0 )
			{
				docPresent = true;
				//TODO:DBG line follows
				//System.out.println("Adding the TFIDF vectors for document:" + curDocNameStr + " in " + tempUnEncrVectFileName);
                //System.out.println("Adding the Binary vectors for document:" + curDocNameStr + " in " + tempUnEncrBinVectFileName);

                //Get the document tfidf term vector contained in the treemap docVectorInfo.docTFIDFVectorTreeMap
                DocMagnitudeTreeMap docTFIDFVectorTreeMap = docVectorInfo.docTFIDFVectorTreeMap.get(curDocNameStr);

                if (this.createTFIDFAndBinaryVectFile(globalTermsSet, tempUnEncrVectFileName, tempUnEncrBinVectFileName, docTFIDFVectorTreeMap)!=0)
                {
                    System.err.println("ERROR! In execution of createTFIDFAndBinaryVectFile!");
                    new Exception().printStackTrace();
                    return -2;
                }


                //Encrypt the unencrypted TFIDF and write the vector to file using the GMP-C code
                if ((ret = nativeCGMPCmbndLib.encrypt_vec_to_file(globalTermsSet.size(),
                        new File(tempUnEncrVectFileName).getAbsolutePath(), outputEncrTFIDFVecFile, keyFileName))!=0)
                {
                    System.err.println("nativeCGMPCmbndLib.encrypt_vec_to_file returned " + ret);
                }

                //Encrypt the unencrypted Binary TFIDF and write the vector to file using the GMP-C code
                if ((ret = nativeCGMPCmbndLib.encrypt_vec_to_file(globalTermsSet.size(),
                        new File(tempUnEncrBinVectFileName).getAbsolutePath(), outputEncrBinVecFile, keyFileName))!=0)
                {
                    System.err.println("nativeCGMPCmbndLib.encrypt_vec_to_file returned "+ret);
                }


				break;
			}

		}//
        System.out.println("Encryption and writing of Query Vector completed!");
		if (!docPresent)
		{
            System.err.println("ERROR! File not present! "+ relFilename);
            return -1;
		}


		//
		return 0;
	}

    public void printDocTermVectors(String newFileName) throws IOException {

        String value;
        //Obtain the global term list once at the beginning so that output can be generated in the right order for
        //all documents
        Set<String> globalTermsSet = globalTermIDFTreeMap.keySet();

        System.out.println("Printing back all the contents for file:" + newFileName);
        File ifp = new File(newFileName);
        FileReader fileReader = new FileReader(ifp);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuffer stringBuffer = new StringBuffer();

        int count = 1, numCharRead;

        Iterator<String> globalTermIt = globalTermsSet.iterator();
        while((value = bufferedReader.readLine())!=null)
        {
            //System.out.println(newFileName+"::"+count+" "+globalTermIt.next()+": "+value);
            count++;

            /*if((bufferedReader.ready()) && !(globalTermIt.hasNext())||
                    (!(bufferedReader.ready()) && (globalTermIt.hasNext())))
            {
                Exception e = new Exception("Mismatch in number of terms in file:"+newFileName+" & in memory global terms:"+globalTermsSet.size()+"!");
                e.printStackTrace();
                System.exit(1);
            }*/
        }
        fileReader.close();
    }

    public int getNumGlobalTerms()
    {
        return globalTermIDFTreeMap.size();
    }

    /*Since most of the Native API functionality is being used via this class,
    * following function is also being called from this class although not necessary,
    * consider this just as a kind of wrapper.
    * */
    public int computeEncrPhaseOfSecMP(String input_interm_prods_file_name, String output_encrypt_rand_prod_file_name, String key_file_name)
    {
        return nativeCGMPCmbndLib.read_decrypt_mul_encrypt_write_encrypted_rand_prod(input_interm_prods_file_name, output_encrypt_rand_prod_file_name, key_file_name);
    }

    /*Since most of the Native API functionality is being used via this class,
    * following function is also being called from this class although not necessary,
    * consider this just as a kind of wrapper.
    * */
    public int derandomizeEncryptedSimProd(String input_rand_encr_prod_file_name, String input_derand_file_name, String output_encrypted_sim_val_file_name, String key_file_name)
    {
        return nativeCGMPCmbndLib.derandomize_encr_encr_sim_prod( input_rand_encr_prod_file_name, input_derand_file_name, output_encrypted_sim_val_file_name, key_file_name);
    }

    /*Since most of the Native API functionality is being used via this class,
    * following function is also being called from this class although not necessary,
    * consider this just as a kind of wrapper.
    * */

    public double decryptSimScore(String input_encr_prod_file_name, String output_sim_score_file_name, String key_file_name)
    {
        return nativeCGMPCmbndLib.decrypt_sim_score(input_encr_prod_file_name, output_sim_score_file_name, key_file_name);
    }
/*Clustering code is below*/
/**/
    double getSim(String fileName1, String fileName2, DocVectorInfo docVectorInfo)
    {
        double simValue = 0;

        DocMagnitudeTreeMap docMagnitudeTreeMap1 = docVectorInfo.docTFIDFVectorTreeMap.get(fileName1);
        DocMagnitudeTreeMap docMagnitudeTreeMap2 = docVectorInfo.docTFIDFVectorTreeMap.get(fileName2);

        if (docMagnitudeTreeMap1.size() != docMagnitudeTreeMap2.size())
        {
            System.err.println("Invalid dimensions!");
            return -1;
        }

        Set<String> termsIt = docMagnitudeTreeMap1.descendingKeySet();
        Iterator<String> iterator1 = termsIt.iterator();
        while(iterator1.hasNext())
        {
            String term = iterator1.next();
            simValue = simValue + (docMagnitudeTreeMap1.get(term)*docMagnitudeTreeMap2.get(term));
        }

        return simValue;


    }

    TreeSet<DocVectorInfo> setOfClusters;

    int bisectingKMeans(int k, DocVectorInfo entireCollectionD)
    {
        int err = 0, i=0, numClusters=0;

        setOfClusters = new TreeSet<DocVectorInfo>();
        //
        Set<String> fileNameSet= entireCollectionD.docTFIDFVectorTreeMap.keySet();
        Iterator<String> fileNameIt = fileNameSet.iterator();
        DocMagnitudeTreeMap tfidfVector;
        DocVectorInfo cluster = new DocVectorInfo();

        //Copy the original structure/Collection in default single cluster
        while(fileNameIt.hasNext())
        {
            String fileName = fileNameIt.next();
            tfidfVector = entireCollectionD.docTFIDFVectorTreeMap.get(fileName);
            cluster.docTFIDFVectorTreeMap.put(fileName, tfidfVector);
        }
        setOfClusters.add(cluster);
        numClusters = 1;
        //
        while(numClusters<=k)
        {

        }

        return err;
    }
}
