#!/usr/bin/bash
OUTPUT="secure_vector_computations"
SOURCE=$OUTPUT$".c"
OBJECTFILE=$OUTPUT$".o"
LIBRARYNAME="lib"$OUTPUT$".so"
echo "====START====";
#gcc -lgmp $SOURCE -o $OUTPUT ;
#echo "-------------";
#ls -rtl | grep $OUTPUT ; 
#echo "-------------";
#date; 
#echo "-------------";
#./$OUTPUT $1 $2 $3; 
#echo "";
#echo "OUTPUT-------";
#echo "-------------";
#cat $2
#echo "-------------";
#cat $3
#echo "-------------";
gcc -I/usr/java/jdk1.8.0_45/include  -I/usr/java/jdk1.8.0_45/include/linux -lgmp -c -Wall -Werror -fpic $SOURCE ;
echo "-------------";
gcc -I/usr/java/jdk1.8.0_45/include  -I/usr/java/jdk1.8.0_45/include/linux -lgmp -shared -o $LIBRARYNAME $OBJECTFILE ;
echo "-------------";
gcc -L/home/nuplavikar/IdeaProjects/LuceneSimilarityUsingDP/src/preprocess/secure_compute  -I/usr/java/jdk1.8.0_45/include  -I/usr/java/jdk1.8.0_45/include/linux -lgmp -Wall -o test main.c -l$OUTPUT ;
echo "-------------";
echo $LD_LIBRARY_PATH
echo "-------------";
#./test 7 ../../../../DocSimilaritySecureApp/encr_q.txt encrypted_q.txt
echo "=====END=====";
