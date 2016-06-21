#include<stdio.h>
#include<stdlib.h>
#include<assert.h>
#include<gmp.h>
#include<stdint.h>
#include<inttypes.h>
#include<errno.h>
#include "key_gen_modified.h"

#define VSIZE 10

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


//Size of vector in dimensions
int vsize;

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


int main(int argc, char* argv[]){

  FILE *input_file1, *input_file2, *output_file;
  int input_size = 0, i, temp, *p_vec2;
  mpz_t *vec1;
  mpz_t result;
  uint64_t temp2;
  char* temp_str = NULL;


  if (argc != 5){
    printf("\n%s", "Usage: a.out num_vec_dim input_file1 input_file2 output_file");
    exit(1);
  }

  //Dynamically creating the array
  vsize = atoi(argv[1]);
  printf("Number of vector dimensions = %d\n", vsize);
  p_vec2 = (int*)malloc(vsize*sizeof(int));
  printf("p_vec2:%p, ENOMEM:%d\n", p_vec2, (errno == ENOMEM)?1:0);


  input_file1 = fopen(argv[2], "r");
  input_file2 = fopen(argv[3], "r");
  output_file = fopen(argv[4], "w");
  
  //initialize vectors and big number variables
  //Dynamically creating the array
  vec1 = (mpz_t *)malloc(vsize*sizeof(mpz_t));
  //initialize vectors and big number variables
  for (i = 0; i < vsize; i++)
    mpz_init(*(vec1+i));

  //variables are set to 0
  mpz_init(result);

  init();

  for (i = 0; i < vsize; i++)
  {
    mpz_init(*(vec1+i));
  }

  //variables are set to 0
  //mpz_init(result);

  //init();


  
  //check if files are opened properly
  if (input_file1 == NULL) {
    printf("\n%s", "Error: open input_file1!");
    exit(1);
  }

  if (input_file2 == NULL) {
    printf("\n%s", "Error: open input_file2!");
    exit(1);
  }

  if (output_file == NULL) {
    printf("\n%s", "Error: open output_file!");
    exit(1);
  }
  
  //fill in the first vector
  for( fscanf(input_file1,"%d", &temp); temp != EOF && input_size < vsize; 
       fscanf(input_file1, "%d", &temp) ){

       //temp = (int) temp2;
       //printf("doc1:: temp2: %" PRId64 ", temp:%d\n", temp2, temp);
       printf("doc1::Wt:%d\n", temp);

    encrypt(*(vec1+input_size), temp);
    //decrypt(vec1[input_size]);
    input_size ++;
  } 

  printf("\n");
  input_size = 0;

  //fill in the second vector
  for( fscanf(input_file2,"%d", &temp); temp != EOF && input_size < vsize; 
       fscanf(input_file2, "%d", &temp) ){

       printf("doc2::Wt.:%d\n", temp);
    *(p_vec2 + input_size) = temp;
    input_size ++;
  } 

  encrypt(result, 0);
  //compute encrypted the vec1 * p_vec2 (dot product)
  for (i = 0; i < input_size; i++) {
    //compute m1 * m2
    mpz_powm_ui(big_temp, *(vec1+i), *(p_vec2+i), n_square);
    //compute m1 + m2
    mpz_mul(result, result, big_temp);
    mpz_mod(result, result, n_square);
  }

  //decrypt the encrypted dot product
  decrypt(result);

  //print the result
  if (mpz_out_str(output_file, 10, result) == 0)
    printf("ERROR: Not able to write the result!\n");
  
  fclose(input_file1);  
  fclose(input_file2);
  fclose(output_file);

  //release space used by big number variables
  for (i = 0; i < vsize; i++)
    mpz_clear(*(vec1+i));

  mpz_clear(result);

  clear();

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

	if ( (key_fp = fopen(KEY_FILE_NAME, "r"))==NULL )
	{
		fprintf(stderr, "File:%s for key not present", KEY_FILE_NAME);
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
