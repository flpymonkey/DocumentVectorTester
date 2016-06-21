#include <gmp.h>

//Currently this is been taken from the application
#define KEY_FILE_NAME "keys.txt"
//function prototypes

//Paillier's public key encrytion and decryption
//set c = (n+1)^m * r^n mod n^2
extern void encrypt(mpz_t c, int m);

//return m = 
extern void decrypt(mpz_t c);

//set r to a random value between 1 and n-1
extern void get_random_r();

//initialize global variables
extern void init();

//release memory allocated to the global variables
extern void clear();

extern void gen_random_input(int v[], int size);

extern int encrypt_vec_to_file( int vsizelocal, const char * input_file_name, const char * output_file_name, const char * key_file_name);

extern int get_n_and_d_from_file();
