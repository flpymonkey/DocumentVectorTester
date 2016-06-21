#include <math.h>
#include<stdio.h>
#include<stdlib.h>
#include<assert.h>
#include<gmp.h>
#include<stdint.h>
#include<inttypes.h>
#include<errno.h>
#include <unistd.h>
#include <time.h>
#include <string.h>

#include <jni.h>
#include "preprocess_EncryptNativeC.h"


#define VSIZE 10
#define KEY_SIZE_BINARY 4096	//n^{2} size
#define KEY_SIZE_BASE10 2*KEY_SIZE_BINARY*log10(2)
#define BIG_NUM_SZ KEY_SIZE_BASE10*sizeof(char)

//global encrypt-decryption variables
mpz_t big_temp;
mpz_t n;
mpz_t n_plus_1;        //n+1, the public key
mpz_t n_square;        //n^2
mpz_t r;
mpz_t r_pow_n;         //r^n mod n^2
mpz_t d;               //d=lcm(p-1, q-1), the private key
mpz_t d_inverse;       //d^{-1} mod n^2
gmp_randstate_t state; //seed for randomization

char g_key_file_name[256];

//function prototypes

//Paillier's public key encrytion and decryption
//set c = (n+1)^m * r^n mod n^2
void encrypt(mpz_t c, int m);

//return m = 
void decrypt(mpz_t c);

//set r to a random value between 1 and n-1
void get_random_r();

//initialize global variables
void init();

//release memory allocated to the global variables
void clear();

void gen_random_input(int v[], int size);

int encrypt_vec_to_file( int vsizelocal, const char * input_file_name, const char * output_file_name, const char * key_file_name);

int get_n_and_d_from_file();
//Size of vector in dimensions
int vsize;

void get_random_r_given_modulo( mpz_t random_no, mpz_t modulo );

//assume state has been initialized
void get_random_r_given_modulo( mpz_t random_no, mpz_t modulo )
{

	do{
		mpz_urandomm(random_no, state, modulo);
	}while(mpz_cmp_ui(random_no, 0) == 0);
}

int encrypt_vec_to_file( int vsizelocal, const char * input_file_name, const char * output_file_name, const char * key_file_name)
{
	int input_size = 0, i, temp;
	mpz_t *vec1;
	char* temp_str = NULL;
	FILE *input_file, *output_file;


	vsize = vsizelocal;
	input_file = fopen(input_file_name, "r");
	output_file = fopen(output_file_name, "w");

	strncpy(g_key_file_name, key_file_name, sizeof(g_key_file_name));

	printf("Number of vector dimensions = %d\n", vsizelocal);
	//printf("p_vec2:%p, ENOMEM:%d\n", p_vec2, (errno == ENOMEM)?1:0);



	//initialize vectors and big number variables
	//Dynamically creating the array
	vec1 = (mpz_t *)malloc(vsizelocal*sizeof(mpz_t));

	//We have to write encrypted values to file
	//to hold each value in string before we need
	//a large string to copy the big number
	//Although the key size is 4096 bits,
	//we will allocate the chars capable of storing
	//8192 bit values. No. of chars required = 
	//log(2^{8192}) base 10 = keysize*log 2 = 8192 * log 2 = 2466.03<2500
	temp_str = (char *)malloc(BIG_NUM_SZ);

	//initialize vectors and big number variables
	for (i = 0; i < vsizelocal; i++)
		mpz_init(*(vec1+i));

	//variables are set to 0

	init();

	for (i = 0; i < vsizelocal; i++)
	{
		mpz_init(*(vec1+i));
	}

	//variables are set to 0

	//init();



	//check if files are opened properly
	if (input_file == NULL) {
		printf("\n%s", "Error: open input_file!");
		return -2;
	}

	if (output_file == NULL) {
		printf("\n%s", "Error: open output_file!");
		return -3;
	}

	//fill in the first vector
	for( fscanf(input_file,"%d", &temp); temp != EOF && input_size < vsizelocal; 
			fscanf(input_file, "%d", &temp) ){

		//temp = (int) temp2;
		//printf("doc1:: temp2: %" PRId64 ", temp:%d\n", temp2, temp);
		printf("doc1::Wt:%d\n", temp);

		encrypt(*(vec1+input_size), temp);
		//gmp_printf("No. of chars:%d, BIGNO:%s\n", gmp_sprintf(temp_str, "%Zd", *(vec1+input_size)), temp_str);
		//TODO: Optimization area use fprintf directly using %Zd instead of temp_str. No memory needed.
		gmp_sprintf(temp_str, "%Zd", *(vec1+input_size));
		//decrypt(vec1[input_size]);
		//gmp_printf("%d: %Zd\n", input_size, *(vec1+input_size));
		fprintf(output_file, "%s", temp_str);
		fflush(output_file);
		sync();
		input_size ++;
		if ( vsizelocal!=1 && input_size < vsizelocal )
		{
			fprintf(output_file, "\n");
		}
	} 




	fclose(input_file);  
	fflush(output_file);
	fclose(output_file);

	//release space used by big number variables
	for (i = 0; i < vsizelocal; i++)
		mpz_clear(*(vec1+i));


	clear();
	free(vec1);
	free(temp_str);

	return 0;

}

/*
 * This function reads the encrypted quwey sent by client, computes the intermediate cosine tfidf product and cosine co-ordination factor,
 * randomizes these two values and writes them along with respective randomizing values into the output_file_name.
 * */
int read_encrypt_vec_from_file_comp_inter_sec_prod( int vsizelocal, const char * input_encr_tfidf_file_name, const char * input_encr_bin_file_name, const char * input_tfidf_vec_file_name, const char * input_bin_vec_file_name, const char * output_file_name, const char * key_file_name)
{
	int input_size = 0, i, temp, *p_tfidf_vec, *p_bin_vec;
	mpz_t *vec1;	//holds input encrypted tfidf q values
	mpz_t *vec2;	//holds input encrypted binary q values
	mpz_t cosine_result;
	mpz_t co_ord_factor;
	mpz_t random_no;
	FILE *input_encr_tfidf_file, *input_tfidf_vec_file, *input_bin_vec_file, *output_file, *input_encr_bin_file;


	vsize = vsizelocal;
	input_encr_tfidf_file = fopen(input_encr_tfidf_file_name, "r");
	input_encr_bin_file = fopen(input_encr_bin_file_name, "r");
	input_tfidf_vec_file = fopen(input_tfidf_vec_file_name, "r");
	input_bin_vec_file = fopen(input_bin_vec_file_name, "r");
	output_file = fopen(output_file_name, "w");

	strncpy(g_key_file_name, key_file_name, sizeof(g_key_file_name));

	printf("Number of vector dimensions = %d\n", vsizelocal);
	//printf("p_tfidf_vec:%p, ENOMEM:%d\n", p_tfidf_vec, (errno == ENOMEM)?1:0);



	//initialize vectors and big number variables
	//Dynamically creating the array
	vec1 = (mpz_t *)malloc(vsizelocal*sizeof(mpz_t));
	vec2 = (mpz_t *)malloc(vsizelocal*sizeof(mpz_t));
	p_tfidf_vec = (int*)malloc(vsize*sizeof(int));
	p_bin_vec = (int*)malloc(vsize*sizeof(int));
	printf("p_tfidf_vec:%p, ENOMEM:%d\n", p_tfidf_vec, (errno == ENOMEM)?1:0);

	//initialize vectors and big number variables
	for (i = 0; i < vsizelocal; i++)
		mpz_init(*(vec1+i));
	for (i = 0; i < vsizelocal; i++)
		mpz_init(*(vec2+i));

	//variables are set to 0
	mpz_init(cosine_result);
	mpz_init(co_ord_factor);
	mpz_init(random_no);

	init();

	//variables are set to 0

	//init();



	//check if files are opened properly
	if (input_encr_tfidf_file == NULL) {
		printf("\n%s", "Error: open input_encr_tfidf_file!");
		return -2;
	}

	if (input_encr_bin_file == NULL) {
		printf("\n%s", "Error: open input_encr_bin_file!");
		return -2;
	}

	if (input_tfidf_vec_file == NULL) {
		printf("\n%s", "Error: open input_tfidf_vec_file!");
		return -3;
	}

	if (input_bin_vec_file == NULL) {
		printf("\n%s", "Error: open input_bin_vec_file!");
		return -4;
	}

	if (output_file == NULL) {
		printf("\n%s", "Error: open output_file!");
		exit(1);
	}


	//fill in the first vector
	input_size = 0;
	while ( (input_size < vsizelocal) )
	{
		if ( input_size == vsizelocal - 1 )
		{
			gmp_fscanf(input_encr_tfidf_file,"%Zd", (vec1+input_size));
		}
		else
		{
			gmp_fscanf(input_encr_tfidf_file,"%Zd\n", (vec1+input_size));
		}
		gmp_printf("%d>> READ %Zd\n", input_size+1, *(vec1+input_size));

		input_size++;
	}
	if ( !( input_size == vsizelocal ) )
	{
		fprintf(stderr, "%s:%d::ERROR! TFIDF: Nothing to read OR parsing error! input_size:%d, vsizelocal:%d\n", 
				__func__, __LINE__, input_size, vsizelocal);
		return -4;
	}

	input_size = 0;
	while ( (input_size < vsizelocal) )
	{
		if ( input_size == vsizelocal - 1 )
		{
			gmp_fscanf(input_encr_bin_file,"%Zd", (vec2+input_size));
		}
		else
		{
			gmp_fscanf(input_encr_bin_file,"%Zd\n", (vec2+input_size));
		}
		gmp_printf("%d>> READ %Zd\n", input_size+1, *(vec2+input_size));

		input_size++;
	}
	if ( !( input_size == vsizelocal ) )
	{
		fprintf(stderr, "%s:%d::ERROR! Binary: Nothing to read OR parsing error! input_size:%d, vsizelocal:%d\n", 
				__func__, __LINE__, input_size, vsizelocal);
		return -4;
	}


	printf("\n");
	input_size = 0;

	//fill in the second vector
	for( fscanf(input_tfidf_vec_file,"%d", &temp); temp != EOF && input_size < vsize; 
			fscanf(input_tfidf_vec_file, "%d", &temp) ){

		printf("Non encrypted TFIDF Input::Wt.:%d\n", temp);
		*(p_tfidf_vec + input_size) = temp;
		input_size ++;
	} 

	input_size = 0;
	for( fscanf(input_bin_vec_file,"%d", &temp); temp != EOF && input_size < vsize; 
			fscanf(input_bin_vec_file, "%d", &temp) ){

		printf("Non encrypted Binary Input::Wt.:%d\n", temp);
		*(p_bin_vec + input_size) = temp;
		input_size ++;
	} 

	encrypt(cosine_result, 0);
	//compute encrypted the vec1 * p_tfidf_vec (dot product)
	for (i = 0; i < input_size; i++) {
		//compute m1 * m2
		mpz_powm_ui(big_temp, *(vec1+i), *(p_tfidf_vec+i), n_square);
		//compute m1 + m2
		mpz_mul(cosine_result, cosine_result, big_temp);
		mpz_mod(cosine_result, cosine_result, n_square);
	}

	encrypt(co_ord_factor, 0);
	//compute encrypted the vec2 * co_ord_factor (dot product)
	for (i = 0; i < input_size; i++) {
		//compute m1 * m2
		mpz_powm_ui(big_temp, *(vec2+i), *(p_bin_vec+i), n_square);
		//compute m1 + m2
		mpz_mul(co_ord_factor, co_ord_factor, big_temp);
		mpz_mod(co_ord_factor, co_ord_factor, n_square);
	}


	/*
	 * Donot decrypt here as we would not be having the CORRESPONDING private key
	 * */
	//decrypt the encrypted dot product
	//decrypt(cosine_result);

	//TODO: Remove this debug decryption. - START
	mpz_t dot_prod;
	mpz_init(dot_prod);
	mpz_set(dot_prod, cosine_result);
	decrypt(dot_prod);
	gmp_fprintf(stderr, "\n%s:%d:: Query*%s TFIDF cosine product: %Zd\n", __func__, __LINE__, input_encr_tfidf_file_name, dot_prod);

	mpz_set(dot_prod, co_ord_factor);
	decrypt(dot_prod);
	gmp_fprintf(stderr, "%s:%d:: Query*%s CO-ORD. cosine product: %Zd\n\n", __func__, __LINE__, input_encr_bin_file_name, dot_prod);
	fflush(stderr);

	mpz_clear(dot_prod);
	//TODO: Remove this debug decryption. - END

	//decrypt the encrypted co ordination factor
	//decrypt(co_ord_factor);

	/*
	 * Generate two random numbers of the modulo n_square and the add these two
	 * to the two results - cosine product and co_ord_factor. 
	 * Write these two random values one after the other
	 * and then the randomized values after them in the output file
	 * given for performing the secure multiplication
	 * protocol. All should be seperated by a newline except maybe the last one
	 * written to the file. FORMAT - output file
	 * ===START===
	 * r_1
	 * randomized cosine tfidf product
	 * r_2
	 * randomized cosine co-ord. product
	 *  ===END===
	 * */

	//Generate random number, say r_1
	get_random_r_given_modulo(random_no, n_square);
	//Write r_1 to outputfile
	mpz_out_str(output_file, 10, random_no);
	fprintf(output_file, "\n");
	//Calculate randomized cosine tfidf product, MULTIPLYING to add
	mpz_mul(cosine_result, cosine_result, random_no);
	//Compute modulus
	mpz_mod(cosine_result, cosine_result, n_square);
	//Write randomized cosine tfidf product to output file
	mpz_out_str(output_file, 10, cosine_result);
	fprintf(output_file, "\n");

	//Generate random number, say r_2
	get_random_r_given_modulo(random_no, n_square);
	//Write r_2 to outputfile
	mpz_out_str(output_file, 10, random_no);
	fprintf(output_file, "\n");
	//Calculate randomized cosine tfidf product, MULTIPLYING to add
	mpz_mul(co_ord_factor, co_ord_factor, random_no);
	//Compute modulus
	mpz_mod(co_ord_factor, co_ord_factor, n_square);
	//Write randomized cosine co_ord product to output file
	mpz_out_str(output_file, 10, co_ord_factor);

	gmp_printf("\nThus similarity of %s and %s score = %Zd written in %s\n", input_encr_tfidf_file_name, input_tfidf_vec_file_name, cosine_result, output_file_name);
#if 0
	//print the cosine_result
	if (mpz_out_str(output_file, 10, cosine_result) == 0)
		printf("ERROR: Not able to write the cosine_result!\n");

	fprintf(output_file, "\n");
#endif
	gmp_printf("\nThus co-ord. factor of %s and %s score = %Zd written in %s\n", input_encr_bin_file_name, input_bin_vec_file_name, co_ord_factor, output_file_name);
#if 0
	//print the co_ord_factor
	if (mpz_out_str(output_file, 10, co_ord_factor) == 0)
		printf("ERROR: Not able to write the co_ord_factor!\n");
#endif


	fclose(input_encr_tfidf_file);  
	fclose(input_encr_bin_file);  
	//fflush(input_tfidf_vec_file);
	fclose(input_tfidf_vec_file);
	//fflush(input_bin_vec_file);
	fclose(input_bin_vec_file);
	fflush(output_file);
	fclose(output_file);

	//release space used by big number variables
	for (i = 0; i < vsizelocal; i++)
		mpz_clear(*(vec1+i));
	for (i = 0; i < vsizelocal; i++)
		mpz_clear(*(vec2+i));


	mpz_clear(cosine_result);
	mpz_clear(co_ord_factor);
	mpz_clear(random_no);
	clear();
	free(vec1);
	free(vec2);
	free(p_tfidf_vec);
	free(p_bin_vec);

	return 0;
}



//using the formula: y = (int){ (double)rand() / [ ( (double)RAND_MAX + (double)1 ) / M] }
//For y, if M is an integer then the result is between 0 and M-1 inclusive
//http://members.cox.net/srice1/random/crandom.html 
void gen_random_input(int v[], int size){

	int i, m = 2;

	srand(time(NULL));

	for(i = 0; i < size; i++) {
		v[i] = (int) ( (double)rand() / ( ( (double)RAND_MAX + (double)1 ) / m) );

		//printf("%d\n", v[i]);
	}
}

//assume c has been initialized
void encrypt(mpz_t c, int m){ 

	get_random_r();

	//set r^n mod n^2
	mpz_powm(r_pow_n, r, n, n_square);

	//set big_temp = (n+1)^m mod n^2
	mpz_powm_ui(big_temp, n_plus_1, m, n_square);

	//set c = (n+1)^m*r^n mod n^2
	mpz_mul(c, big_temp, r_pow_n);
	mpz_mod(c, c, n_square);
}

void decrypt(mpz_t c){

	//set big_temp = c^d mod n^2
	mpz_powm(big_temp, c, d, n_square);

	//set big_temp = big_temp -1
	mpz_sub_ui(big_temp, big_temp, 1);

	//divide big_temp by n
	mpz_divexact(big_temp, big_temp, n);

	//d^-1 * big_temp
	mpz_mul(big_temp, d_inverse, big_temp);

	mpz_mod(c, big_temp, n);
}

//assume state has been initialized
void get_random_r(){

	do{
		mpz_urandomm(r, state, n);
	}while(mpz_cmp_ui(r, 0) == 0);
}


/* File should have keys in order of:
 * #1. p
 * #2. q
 * #3. n - We require n.
 * #4. n+1 (public key)
 * #5. d (private key) - We require d
 * These are seperated by a newline
 * */

int get_n_and_d_from_file()
{
	FILE *key_fp = NULL;
	int count=1, err=-1;
	mpz_t temp;

	mpz_init(temp);

	if ( (key_fp = fopen(g_key_file_name, "r"))==NULL )
	{
		fprintf(stderr, "File:%s for key not present", g_key_file_name);
		err = errno;
	}


	while ( mpz_inp_str(temp, key_fp, 10) != 0  )
	{
		//ignore p, q and n+1
		if ( count==3 )
		{
			// read n
			mpz_set(n, temp);
		}
		else if ( count==5 )
		{
			//read d
			mpz_set(d, temp);
		}
		count++;
	}

	mpz_clear(temp);
	if ( key_fp )
	{
		fclose(key_fp);
	}
	err = 0;
	return err;
}

void init(){

	mpz_init(big_temp);
	mpz_init(n);
	mpz_init(n_plus_1);
	mpz_init(n_square);      
	mpz_init(r);
	mpz_init(r_pow_n);       
	mpz_init(d);             
	mpz_init(d_inverse);
	gmp_randinit_default(state);
	gmp_randseed_ui(state, time(NULL));     

#if 0

	//if (mpz_set_str(n, "179681631915977638526315179067310074434153390395025087607016290555239821629901731559598243352941859391381209211619271844002852733873844383750232911574662592776713675341534697696513241904324622555691981004726000585832862539270063589746625628692671893634789450932536008307903467370375372903436564465076676639793", 10) == -1) {
	if (mpz_set_str(n, "32317006071311007300714876688666765257611171752763855809160912665177570453236751584543954797165007338356871062761077928870875400023524429983317970103631801129940018920824479704435252236861111449159643484346382578371909996991024782612350354546687409434034812409194215016861565205286780300229046771688880430612167916071628041661162278649907703501859979953765149466990620201855101883306321620552981581118638956530490592258880907404676403950212619825592177687668726740525457667131084835164607889060249698382116887240122647424904709577375839097384133219410477128893432015573232511359202702215050502392962065602621373646633", 10) == -1) {
		printf("\n n = %s is invalid \n", n);
		exit(0);
	}
#endif

	// Get the values of n and d from already generated file
	get_n_and_d_from_file();
	gmp_printf("n read = %Zd\n", n);
	gmp_printf("d read = %Zd\n", d);

	mpz_add_ui(n_plus_1, n, 1);
	mpz_pow_ui(n_square, n, 2);


	//d=lcm(p-1, q-1)
#if 0

	//if (mpz_set_str(d, "11230101994748602407894698691706879652134586899689067975438518159702488851868858222474890209558866211961325575726204490250178295867115273984389556973416410372977387708241492548657648934473794873887265114170151559636690542947614482279486573108720489183236783924737117351777821606184702946528449106783160617728", 10) == -1) {
	if (mpz_set_str(d, "16158503035655503650357438344333382628805585876381927904580456332588785226618375792271977398582503669178435531380538964435437700011762214991658985051815900564970009460412239852217626118430555724579821742173191289185954998495512391306175177273343704717017406204597107508430782602643390150114523385844440215305904188722327789239808208805874958140879652760769485822638556957710893419557319777578361302813366793271881989825323106333296234499565420424474500540721018071974424406358805685133447529623729447991492181271325655526833481571159446598626059774013428343748622306000136795074246791265885436988193283384961017822594", 10) == -1) {
		printf("\n d = %s is invalid \n", d);
		exit(0);
	}
#endif

	if (mpz_invert (d_inverse, d, n_square) == 0) {

		printf("\n%s\n", "d^-1 does not exist!");
		exit(0);
	}  


}

void clear(){

	gmp_randclear(state);
	mpz_clear(big_temp);
	mpz_clear(n);
	mpz_clear(n_plus_1);
	mpz_clear(n_square);
	mpz_clear(r);
	mpz_clear(r_pow_n);
	mpz_clear(d);
	mpz_clear(d_inverse);

}

	JNIEXPORT jint JNICALL Java_preprocess_EncryptNativeC_encrypt_1vec_1to_1file
(JNIEnv *env, jobject obj, jint vsize, jstring input_file_name, jstring output_encr_file_name, jstring name_key_file)
{
	int err = -1;
	const char *ip_file_name = (*env)->GetStringUTFChars(env, input_file_name, 0);
	const char *op_file_name = (*env)->GetStringUTFChars(env, output_encr_file_name, 0);
	const char *key_file_name = (*env)->GetStringUTFChars(env, name_key_file, 0);


	printf("Number of dimensions:%d\n", vsize);
	printf("Query's Un-Encrypted vectors stored at: %s\n", ip_file_name);
	printf("Query's Encrypted vectors stored at: %s\n", op_file_name);
	printf("Key file used from : %s\n", key_file_name);


	err = encrypt_vec_to_file(vsize, ip_file_name, op_file_name, key_file_name);

	(*env)->ReleaseStringUTFChars(env, input_file_name, ip_file_name);
	(*env)->ReleaseStringUTFChars(env, output_encr_file_name, op_file_name);
	(*env)->ReleaseStringUTFChars(env, name_key_file, key_file_name);

	return err;
}


	JNIEXPORT jint JNICALL Java_preprocess_EncryptNativeC_read_1encrypt_1vec_1from_1file_1comp_1inter_1sec_1prod
(JNIEnv *env, jobject obj, jint vsize, jstring ip_encr_tfidf_f_name, jstring ip_encr_bin_f_name, jstring ip_unencr_tfidf_f_name, jstring ip_unencr_bin_f_name, jstring op_encr_rand_inter_prod_f_name, jstring ip_key_f_name)
{
	int err = -1;
	const char *ip_encr_tfidf_q_file 			= (*env)->GetStringUTFChars(env, ip_encr_tfidf_f_name, 0);
	const char *ip_encr_bin_q_file 			= (*env)->GetStringUTFChars(env, ip_encr_bin_f_name, 0);
	const char *ip_unencr_tfidf_file 		= (*env)->GetStringUTFChars(env, ip_unencr_tfidf_f_name, 0);
	const char *ip_unencr_bin_file 			= (*env)->GetStringUTFChars(env, ip_unencr_bin_f_name, 0);
	const char *op_encr_rand_inter_prod_file 	= (*env)->GetStringUTFChars(env, op_encr_rand_inter_prod_f_name, 0);
	const char *key_file_name 			= (*env)->GetStringUTFChars(env, ip_key_f_name, 0);


	printf("Number of dimensions:%d\n", vsize);
	printf("Query's Encrypted TFIDF vector obtained from client stored at: %s\n", ip_encr_tfidf_q_file);
	printf("Query's Encrypted binary vector obtained from client stored at: %s\n", ip_encr_bin_q_file);
	printf("Collection document's unencrypted, scaled tfidf vector stored at: %s\n", ip_unencr_tfidf_file);
	printf("Collection document's unencrypted, scaled binary vector stored at: %s\n", ip_unencr_bin_file);
	printf("Output: random nos., encrypted intermediate dot products stored at: %s\n", op_encr_rand_inter_prod_file);
	printf("Key file used from : %s\n", key_file_name);


	//Call the function here
	err = read_encrypt_vec_from_file_comp_inter_sec_prod(vsize, ip_encr_tfidf_q_file, ip_encr_bin_q_file, ip_unencr_tfidf_file, ip_unencr_bin_file, op_encr_rand_inter_prod_file, key_file_name );


	(*env)->ReleaseStringUTFChars(env, ip_encr_tfidf_f_name, ip_encr_tfidf_q_file);
	(*env)->ReleaseStringUTFChars(env, ip_encr_bin_f_name, ip_encr_bin_q_file);
	(*env)->ReleaseStringUTFChars(env, ip_unencr_tfidf_f_name, ip_unencr_tfidf_file);
	(*env)->ReleaseStringUTFChars(env, ip_unencr_bin_f_name, ip_unencr_bin_file);
	(*env)->ReleaseStringUTFChars(env, op_encr_rand_inter_prod_f_name, op_encr_rand_inter_prod_file);
	(*env)->ReleaseStringUTFChars(env, ip_key_f_name, key_file_name);


	return err;
}
