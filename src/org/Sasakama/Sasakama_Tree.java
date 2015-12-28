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

public class Sasakama_Tree {
	Sasakama_Pattern head;
	Sasakama_Tree next;
	Sasakama_Node root;
	int state;
	
	public Sasakama_Tree(){
		initialize();
	}
	
	public void initialize(){
		head = null;
		next = null;
		root = null;
		state = 0;
	}
	
	public void clear(){
		head = null;
		next = null;

		if(root != null){
			root.clear();
			root = null;
		}
		initialize();
	}
	
	public void parse_pattern(String string){
		int left;
		head = null;
		
		//System.err.printf("in parse_pattern: %s\n", string);
		
		if((left = string.indexOf('{')) != -1){
			StringBuilder sb = new StringBuilder(string);
			sb.delete(0, left+1);

			if(string.charAt(0) == '(')
				sb.deleteCharAt(0);
			
			int right = sb.lastIndexOf("}");
			if(0 < right && sb.charAt(right-1) == ')')
				--right;
			sb.setCharAt(right, ',');
			
			Sasakama_Pattern last_pattern = null;
			while((left = sb.indexOf(",")) != -1){
				Sasakama_Pattern pattern = new Sasakama_Pattern();

				if(head != null)
					last_pattern.next = pattern;
				else
					head = pattern;

				pattern.string = sb.substring(0, left);
				//System.err.printf("parse_pattern:%s\n", pattern.string);
				sb.delete(0, left+1);
				pattern.next = null;
				last_pattern = pattern;
			}
		}	
	}
	
	public Boolean load(Sasakama_File hf, Sasakama_Question question){
		if(hf == null)
			return false;
		
		StringBuffer sb = new StringBuffer();
		if(hf.get_pattern_token(sb) == false){
			clear();
			return false;
		}
		//System.err.printf("tree load pat:%s\n", sb.toString());
		Sasakama_Node node, last_node;
		node = new Sasakama_Node();
		root = last_node = node;
		
		if(sb.charAt(0) == '{'){
			sb = new StringBuffer();
			while(hf.get_pattern_token(sb) == true && sb.charAt(0) != '}'){
			//	System.err.printf("tree_load buff1:%s\n", sb.toString());
				node = last_node.find(Integer.parseInt(sb.toString()));
				if(node == null){
					Sasakama_Misc.error("Sasakama_Tree.load: Cannot find node "+sb.toString());
					clear();
					return false;
				}
				sb = new StringBuffer();
				if(hf.get_pattern_token(sb) == false){
					clear();
					return false;
				}
				//System.err.printf("tree_load buff2:%s\n", sb.toString());
				node.quest = question.find(sb.toString());
				if(node.quest == null){
					Sasakama_Misc.error("Sasakama_Tree.load: Cannot find question "+sb.toString());
					clear();
					return false;
				}
				node.yes = new Sasakama_Node();
				node.no  = new Sasakama_Node();

				sb = new StringBuffer();
				if(hf.get_pattern_token(sb) == false){
					node.quest = null;
					node.yes = null;
					node.no  = null;
					clear();
					return false;
				}
				//System.err.printf("tree_load buff3:%s\n", sb.toString());
				if(Sasakama_Misc.is_num(sb.toString()))
					node.no.index = Integer.parseInt(sb.toString());
				else{
					node.no.pdf = Sasakama_Misc.name2num(sb.toString());
					//System.err.printf("name2num: no, buff:%s num:%d\n", sb.toString(), node.no.pdf);
				}
				node.no.next = last_node;
				last_node = node.no;
				
				sb = new StringBuffer();				
				if(hf.get_pattern_token(sb) == false){
					node.quest = null;
					node.yes = null;
					node.no  = null;
					clear();
					return false;
				}
				//System.err.printf("tree_load buff4:%s\n", sb.toString());
				/*
				if(Sasakama_Misc.is_num(sb.toString()))
					System.err.printf("tree_load true\n");
				*/
				if(Sasakama_Misc.is_num(sb.toString()))
					node.yes.index = Integer.parseInt(sb.toString());
				else{
					node.yes.pdf = Sasakama_Misc.name2num(sb.toString());
					//System.err.printf("name2num: yes, buff:%s num:%d\n", sb.toString(), node.yes.pdf);
				}
				node.yes.next = last_node;
				last_node = node.yes;
			}
		}
		else{
			node.pdf = Sasakama_Misc.name2num(sb.toString());
		}
			
		return true;
	}
	
	public int search_node(final String string){
		Sasakama_Node node = root;
		
		while(node != null){
			if(node.quest == null)
				return node.pdf;
			if(node.quest.match(string)){
				if(node.yes.pdf > 0)
					return node.yes.pdf;
				node = node.yes;
			}
			else{
				if(node.no.pdf > 0)
					return node.no.pdf;
				node = node.no;
			}
		}
		
		Sasakama_Misc.error("Sasakama_Tree.search_node: Cannot find node.");
		return 1;
	}
}

