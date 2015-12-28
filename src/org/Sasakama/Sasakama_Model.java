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

public class Sasakama_Model {
	int vector_length;
	int num_windows;
	Boolean is_msd;
	int ntree;
	int[] npdf;
	ArrayList<ArrayList<float[]>> pdf;
	Sasakama_Tree tree;
	Sasakama_Question question;
	
	Sasakama_Model(){
		initialize();
	}
	
	public void initialize(){
		vector_length = 0;
		num_windows = 0;
		is_msd = false;
		ntree = 0;
		npdf = null;
		pdf  = null;
		tree = null;
		question = null;
	}
	
	public void clear(){	
		question = null;
		tree     = null;
		pdf      = null;
		npdf 	 = null;
		initialize();
	}
	
	public Boolean load_tree(Sasakama_File hf){
		if(hf == null){
			ntree = 1;
			return true;
		}
		
		ntree = 0;
		Sasakama_Question last_question = null;
		Sasakama_Tree last_tree = null;
		while(hf.feof() == false){
			StringBuffer buff = new StringBuffer();
			hf.get_pattern_token(buff);
			//System.err.printf("buff:%s\n", buff.toString());
			if(buff.toString().startsWith("QS")){
				Sasakama_Question q = new Sasakama_Question();
				if(q.load(hf) == false){
					q.clear();
					clear();
					return false;
				}
				if(question != null)
					last_question.next = q;
				else
					question = q;

				q.next = null;
				last_question = q;
			}
			
			int state = Sasakama_Misc.get_state_num(buff.toString());
			//System.err.printf("str:%s state:%d\n", buff.toString(), state);
			if(state != 0){
				Sasakama_Tree tr = new Sasakama_Tree();
				tr.state = state;
				tr.parse_pattern(buff.toString());
				if(tr.load(hf, question)== false){
					tr.clear();
					clear();
					return false;
				}
				if(tree != null)
					last_tree.next = tr;
				else
					tree = tr;
				
				tr.next = null;
				last_tree = tr;
				ntree++;
			}
		}
		if(tree == null)
			ntree = 1;
		
		return true;
	}
	
	public Boolean load_pdf(Sasakama_File hf, int vector_length, int num_windows, Boolean is_msd){
		Boolean result = true;
		
		if(hf == null || ntree <= 0){
			Sasakama_Misc.error("HTS_Model.load_pdf: File for pdfs is not specified.");
			return false;
		}
		/*
		System.err.println("vector_length:"+Integer.toString(vector_length));
		System.err.println("num_windows:"+Integer.toString(num_windows));
		System.err.println("ntree:"+Integer.toString(ntree));
		*/
		
		this.vector_length = vector_length;
		this.num_windows = num_windows;
		this.is_msd = is_msd;
		npdf = new int[ntree+2];
		npdf[0] = npdf[1] = -1;
		
		for(int j=2;j <= ntree+1;j++){
			int[] buf = new int[1];

			if(hf.fread(buf) != 1){
				result = false;
				break;
			}

			npdf[j] = buf[0];
			//System.err.println("npdf["+Integer.toString(j)+"]:"+Integer.toString(npdf[j]));
		}
		
		for(int j=2;j <= ntree+1;j++){
			if(npdf[j] <= 0){
				Sasakama_Misc.error("HTS_Model.load_pdf: # of pdfs at "+String.valueOf(j)+"-th state should be positive.");
				result = false;
				break;
			}
			
		}
		if(result == false){
			npdf = null;
			initialize();
			return false;
		}
		pdf = new ArrayList<ArrayList<float[]>>();
		int len = 0;
		if(is_msd)
			len = vector_length * num_windows * 2 + 1;
		else
			len = vector_length * num_windows * 2;
		
		for(int j=0;j < 2;j++){
			pdf.add(null);
		}
		for(int j=2;j <=ntree+1;j++){
			ArrayList<float[]> tmplist = new ArrayList<float[]>();
			tmplist.add(null);
		//	System.err.println("npdf:"+Integer.toString(npdf[j]));
			for(int k=1;k <= npdf[j];k++){
				float[] dd = new float[len];
				int cnt = hf.fread(dd);
				if(cnt != len){
					//System.err.printf("cnt:%d, len:%d\n", cnt, len);
					result = false;
				}
				tmplist.add(dd);
			}
			pdf.add(tmplist);
		}
		if(result == false){
			clear();
			return false;
		}
		return true;
	}
	
	public Boolean load(Sasakama_File pdf, Sasakama_File tree, int vector_length, int num_windows, Boolean is_msd){
		if(pdf == null || vector_length == 0 || num_windows == 0)
			return false;
		
		clear();
		
		if(load_tree(tree) != true){
			clear();
			return false;
		}
		
		if(load_pdf(pdf, vector_length, num_windows, is_msd) != true){
			clear();
			return false;
		}
		
		return true;
	}
	
	public void get_index(int state_index, final String string, int[] tree_index, int[] pdf_index){
		Sasakama_Tree tr = null;
		
		tree_index[0] = 2;
		pdf_index[0]  = 1;
		
		if(tree == null)
			return;
		
		//System.err.printf("get_index: %s\n", string);
		Boolean find = false;
		for(tr = tree;tr != null;tr = tr.next){
			if(tr.state == state_index){
				Sasakama_Pattern pattern = tr.head;
				if(pattern == null)
					find = true;
				for(;pattern != null;pattern = pattern.next)
					if(Sasakama_Misc.pattern_match(string, pattern.string)){
						//System.err.printf("get_index: find!!, string:%s pattern:%s\n", string, pattern.string);
						find = true;
						break;
					}
				if(find == true)
					break;
			}
			tree_index[0]++;
		}
		
		if(tr != null)
			pdf_index[0] = tr.search_node(string);
		else
			pdf_index[0] = tree.search_node(string);
		
		//System.err.printf("tree_index:%d, pdf_index:%d\n", tree_index[0], pdf_index[0]);
	}
	
	public float get_pdf(int i, int j, int k){
		return pdf.get(i).get(j)[k];
	}
	
	public void add_parameter(int state_index, String string, double[] mean, double[] vari, double[] msd, double weight, int base){
		int[] tree_index = new int[1];
		int[] pdf_index  = new int[1];
		
		int len = vector_length * num_windows;
		
		get_index(state_index, string, tree_index, pdf_index);
		for(int i=0;i < len;i++){
			//System.err.printf("i:%d j:%d k:%d\n", tree_index[0], pdf_index[0], i);
			mean[base+i] += weight * get_pdf(tree_index[0], pdf_index[0], i);
			vari[base+i] += weight * get_pdf(tree_index[0], pdf_index[0], i+len);
			/* System.err.printf("mean:%5.2f vari:%5.2f\n", 
						get_pdf(tree_index[0], pdf_index[0], i),
						get_pdf(tree_index[0], pdf_index[0], i+len) 
						);*/
		}
		if(msd != null && is_msd == true)
			msd[0] += weight * get_pdf(tree_index[0], pdf_index[0], len+len);
	}
}
