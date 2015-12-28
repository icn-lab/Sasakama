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
import java.util.ArrayList;


public class Sasakama_Window {
	int size;
	int[] l_width;
	int[] r_width;
	ArrayList<double[]> coefficient;
	int max_width;
	
	Sasakama_Window(){
		initialize();
	}
	
	Sasakama_Window(Sasakama_Window win){
		size = win.size;
		max_width = win.max_width;
		l_width = new int[win.l_width.length];
		r_width = new int[win.r_width.length];
		coefficient = new ArrayList<double[]>();
		
		for(int i=0;i < l_width.length;i++)
			l_width[i] = win.l_width[i];
		
		for(int i=0;i < r_width.length;i++)
			r_width[i] = win.r_width[i];
		
		for(int i=0;i < win.coefficient.size();i++){
			double[] src_coe = win.coefficient.get(i);
			double[] dst_coe = new double[src_coe.length];
			for(int j=0;j < dst_coe.length;j++)
				dst_coe[j] = src_coe[j];
			coefficient.add(dst_coe);
		}
	}
	
	public void initialize(){
		size = 0;
		l_width = null;
		r_width = null;
		coefficient = null;
		max_width = 0;
	}
	
	public void clear(){
		if(coefficient != null)
			coefficient = null;
		
		if(l_width != null)
			l_width = null;
		if(r_width != null)
			r_width = null;
		
		initialize();
	}
	
	public Boolean load(Sasakama_File[] hf){
		int size = hf.length;
		
		if(hf == null || size == 0)
			return false;
		
		this.size = size;
		l_width = new int[size];
		r_width = new int[size];
		coefficient = new ArrayList<double[]>();

		int fsize;
		Boolean result = true;
		
		for(int i=0;i < size;i++){
			StringBuffer buff = new StringBuffer();			
			if(hf[i].get_pattern_token(buff) == false){
				result = false;
				fsize = 1;
			}
			else{
				fsize = Integer.parseInt(buff.toString());
				if(fsize == 0){
					result = false;
					fsize  = 1;
				}
			}
			double [] dd = new double[fsize];
			for(int j=0;j < fsize;j++){
				if(hf[i].get_token(buff) == false){
					result = false;
					dd[j] = 0.0;
				}
				else{
					dd[j] = Double.parseDouble(buff.toString());
				}
				//System.err.printf("win.dd[%d][%d]:%5.2f\n", i, j, dd[j]);
			}
			coefficient.add(dd);

			int length = fsize/2;
			l_width[i] = -1 * length;
			r_width[i] = length;
			if(fsize %2 == 0)
				r_width[i]--;
		}
		max_width = 0;
		for(int i=0;i < size;i++){
			if(max_width < Math.abs(l_width[i]))
				max_width = Math.abs(l_width[i]);
			if(max_width < Math.abs(r_width[i]))
				max_width = Math.abs(r_width[i]);
		}
		
		if(result == false){
			clear();
			return false;
		}
		
		return true;
	}
	
	public double get_coefficient(int i, int j){
		double [] dd = coefficient.get(i);
		int length = dd.length/2;
		
		return dd[j+length]; 
	}
	
}
