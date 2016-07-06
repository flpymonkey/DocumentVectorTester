package com.example.android.documentvectortester;

/**
 * Created by nuplavikar on 2/29/16.
 */
public class EncryptNativeC
{

	static
	{
		System.loadLibrary("libsecure_vector_computations");
	}
	//write java portion and translate into C code. Use function signitures that are auto generated using Java Edge command
	//Code to integrate C code using Native Java Interface
	public native int encrypt_vec_to_file(int vsizelocal, String input_file_name, String output_file_name, String keyFileName);
	public native int read_encrypt_vec_from_file_comp_inter_sec_prod(int vsizelocal, String input_encr_tfidf_file_name, String input_encr_bin_file_name, String input_unencr_tfidf_file_name, String input_unencr_bin_file_name, String output_file_name, String keyFileName);
	public native int read_decrypt_mul_encrypt_write_encrypted_rand_prod( String input_interm_prods_file_name, String output_encrypt_rand_prod_file_name, String key_file_name);
	public native int derandomize_encr_encr_sim_prod( String input_rand_encr_prod_file_name, String input_derand_file_name, String output_encrypted_sim_val_file_name, String key_file_name);
	public native double decrypt_sim_score( String input_encr_prod_file_name, String output_sim_score_file_name, String key_file_name);

}

