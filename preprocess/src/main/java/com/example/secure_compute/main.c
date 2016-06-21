#include <stdio.h>
#include <stdlib.h>
#include "encryptvector.h"

/* Usage: ./executable_name num_vec_dim input_file output_file
 *
 * */
int main(int argc, char* argv[]){



	printf("No. of parameters: %d\n", argc);
	if (argc != 4){
		printf("Usage: %s num_vec_dim input_file output_file", argv[0]);
		exit(1);
	}

	//Dynamically creating the array

	return encrypt_vec_to_file(atoi(argv[1]), argv[2], argv[3], KEY_FILE_NAME);
}
