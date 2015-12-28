// This software is a language translation version of "hts_engine API" developed by HTS Working Group.
// 
// Copyright (c) 2015 Intelligent Communication Network (Ito-Nose) Laboratory
// Tohoku University
// Copyright (c) 2001-2015 Nagoya Institute of Technology
// Department of Computer Science
// 2001-2008 Tokyo Institute of Technology
// Interdisciplinary Graduate School of
// Science and Engineering
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright notice, 
// this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice, 
// this list of conditions and the following disclaimer in the documentation 
// and/or other materials provided with the distribution.
// * Neither the name of the "Intelligent Communication Network Laboratory, Tohoku University" nor the names of its contributors 
// may be used to endorse or promote products derived from this software 
// without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.Sasakama;

public class Sasakama_SMatrices {
	double[][] mean;
	double[][] ivar;
	double[] g;
	double[][] wuw;
	double[] wum;


	Sasakama_SMatrices(){
	}
	
	public void create(int length, int vector_length, int win_size, int width){
		mean = new double[length][vector_length * win_size];
		ivar = new double[length][vector_length * win_size];
		wum  = new double[length];
		wuw  = new double[length][width];
		g    = new double[length];
	}
	
	public void print(){
		System.err.printf("*** SMatrices begin ***\n");
		print_mean();
		print_ivar();
		print_g();
		print_wuw();
		print_wum();
		System.err.printf("*** SMatrices end ***\n");
	}
	
	public void print_mean(){
		for(int i=0;i < mean.length;i++){
			for(int j=0;j < mean[i].length;j++){
				System.err.printf("mean[%d][%d]:%5.2f\n", i, j, mean[i][j]);
			}
		}
	}
	
	public void print_ivar(){
		for(int i=0;i < ivar.length;i++){
			for(int j=0;j < ivar[i].length;j++){
				System.err.printf("ivar[%d][%d]:%5.2f\n", i, j, ivar[i][j]);
			}
		}
	}
	
	public void  print_g(){
		for(int i=0;i < g.length;i++){
			System.err.printf("g[%d]:%5.2f\n", i, g[i]);
		}
	}
	
	public void print_wuw(){
		for(int i=0;i < wuw.length;i++){
			for(int j=0;j < wuw[i].length;j++){
				System.err.printf("wuw[%d][%d]:%5.2f\n", i, j, wuw[i][j]);
			}
		}
	}
	
	public void  print_wum(){
		for(int i=0;i < wum.length;i++){
			System.err.printf("wum[%d]:%5.2f\n", i, wum[i]);
		}
	}
	
	
	
}