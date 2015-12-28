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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sasakama_ModelSet {
	String hts_voice_version;
	int sampling_frequency;
	int frame_period;
	int num_voices;
	int num_states;
	int num_streams;
	String stream_type;
	String fullcontext_format;
	String fullcontext_version;
	Sasakama_Question gv_off_context;
	String[] option;
	Sasakama_Model[] duration;
	Sasakama_Window[] window;
	Sasakama_Model[][] stream;
	Sasakama_Model[][] gv;
	
	/* for internal use */
	private String gv_off_context_string;
	private int[] vector_length;
	private Boolean[] is_msd;
	private int[] num_windows;
	private Boolean[] use_gv;
	private String duration_pdf;
	private String duration_tree;
	private ArrayList<String[]> stream_win;
	private String[] stream_pdf;
	private String[] stream_tree;
	private String[] gv_pdf;
	private String[] gv_tree;
	
	Sasakama_ModelSet(){
		initialize();
	}
	
	public void initialize(){
		hts_voice_version = null;
		sampling_frequency = 0;
		frame_period = 0;
		num_voices = 0;
		num_states = 0;
		num_streams = 0;
		stream_type = null;
		fullcontext_format = null;
		fullcontext_version = null;
		gv_off_context = null;
		option = null;
		
		duration = null;
		window = null;
		stream = null;
		gv = null;
	}
	
	public void clear(){
		hts_voice_version = null;
		stream_type = null;
		fullcontext_format = null;
		fullcontext_version = null;
		gv_off_context = null;
		option = null;
		duration = null;
		stream = null;
		gv = null;
		initialize();
	}
	
	private Boolean load_global_info(Sasakama_File hf, int i){
		/*
		[GLOBAL]
		HTS_VOICE_VERSION:1.0
		SAMPLING_FREQUENCY:16000
		FRAME_PERIOD:80
		NUM_STATES:5
		NUM_STREAMS:3
		STREAM_TYPE:MCP,LF0,LPF
		FULLCONTEXT_FORMAT:HTS_TTS_JPN
		FULLCONTEXT_VERSION:1.0
		GV_OFF_CONTEXT:"*-sil+*","*-pau+*"
		COMMENT:
		 */
		String separator = ":";
		String buffer = null;
		Boolean error = false;

		String temp_hts_voice_version = null;
		int temp_sampling_frequency = 0;
		int temp_frame_period = 0;
		int temp_num_states = 0;
		int temp_num_streams = 0;
		String temp_stream_type = null;
		String temp_fullcontext_format = null;
		String temp_fullcontext_version = null;
		String temp_gv_off_context_string = null;
		
		buffer = hf.readLine();
		//System.err.printf("line:[%s]\n", buffer);
		if(Sasakama_Misc.strequal(buffer, "[GLOBAL]") != true){
			error = true;
		}
		while(error == false){
			int fpos = hf.ftell();
			buffer   = hf.readLine();
			//System.err.printf("line:[%s]\n", buffer);
			if(Sasakama_Misc.strequal(buffer, "[STREAM]") == true){
				hf.fseek(fpos, Sasakama_File.SEEK_SET);
				break;
			}
			
			int colon_pos = buffer.indexOf(separator);
			String key    = buffer.substring(0, colon_pos);
			String value  = buffer.substring(colon_pos+1);
			
			switch(key){
			case "HTS_VOICE_VERSION":
				temp_hts_voice_version = value;
				break;
			case "SAMPLING_FREQUENCY":
				temp_sampling_frequency = Integer.parseInt(value);
				break;
			case "FRAME_PERIOD":
				temp_frame_period = Integer.parseInt(value);
				break;
			case "NUM_STATES":
				temp_num_states = Integer.parseInt(value);
				break;
			case "NUM_STREAMS":
				temp_num_streams = Integer.parseInt(value);
				break;
			case "STREAM_TYPE":
				temp_stream_type = value;
				break;
			case "FULLCONTEXT_FORMAT":
				temp_fullcontext_format = value;
				break;
			case "FULLCONTEXT_VERSION":
				temp_fullcontext_version = value;
				break;
			case "GV_OFF_CONTEXT":
				temp_gv_off_context_string = value;
				break;
			case "COMMENT":
				break;
			default:
				Sasakama_Misc.error("Sasakama_ModelSet.load_global: unknown option "+key);
				error = true;
			}
		}
		if(error == false)
			if(i==0){
				hts_voice_version     = temp_hts_voice_version;
				sampling_frequency    = temp_sampling_frequency;
				frame_period          = temp_frame_period;
				num_states            = temp_num_states;
				num_streams           = temp_num_streams;
				stream_type           = temp_stream_type;
				fullcontext_format    = temp_fullcontext_format;
				fullcontext_version   = temp_fullcontext_version;
				gv_off_context_string = temp_gv_off_context_string;
				if(num_streams != stream_type.split(",").length)
					error = true;
			}
			else{
				if(Sasakama_Misc.strequal(hts_voice_version, temp_hts_voice_version) != true)
					error = true;
				if(sampling_frequency != temp_sampling_frequency)
					error = true;
				if(frame_period != temp_frame_period)
					error = true;
				if(num_states != temp_num_states)
					error = true;
				if(num_streams != temp_num_streams)
					error = true;
				if(num_streams != temp_stream_type.split(",").length)
					error = true;
				if(Sasakama_Misc.strequal(stream_type, temp_stream_type) != true)
					error = true;
				if(Sasakama_Misc.strequal(fullcontext_format, temp_fullcontext_format) != true)
					error = true;
				if(Sasakama_Misc.strequal(fullcontext_version, temp_fullcontext_version) != true)
					error = true;
				if(Sasakama_Misc.strequal(gv_off_context_string, temp_gv_off_context_string) != true)
					error = true;						
			}
		
		return !error;
	}
	
	private Boolean load_stream_info(Sasakama_File hf, int i){
		/*
		[STREAM]
		VECTOR_LENGTH[MCP]:25
		VECTOR_LENGTH[LF0]:1
		VECTOR_LENGTH[LPF]:31
		IS_MSD[MCP]:0
		IS_MSD[LF0]:1
		IS_MSD[LPF]:0
		NUM_WINDOWS[MCP]:3
		NUM_WINDOWS[LF0]:3
		NUM_WINDOWS[LPF]:1
		USE_GV[MCP]:0
		USE_GV[LF0]:0
		USE_GV[LPF]:0
		OPTION[MCP]:ALPHA=0.42
		OPTION[LF0]:
		OPTION[LPF]:
		 */
		String buffer = null;
		Boolean error = false;

		buffer = hf.readLine();
		//System.err.println(buffer);

		if(Sasakama_Misc.strequal(buffer, "[STREAM]") != true){
			error = true;
		}
		
		int[] temp_vector_length = new int[num_streams];
		Boolean[] temp_is_msd    = new Boolean[num_streams];
		int[] temp_num_windows   = new int[num_streams];
		Boolean[] temp_use_gv    = new Boolean[num_streams];
		String[] temp_option     = new String[num_streams];
		Pattern p = Pattern.compile("^(.+)\\[([^\\]]+)\\]:(.*)$");
		
		String[] stream_type_list = stream_type.split(",");
		
		while(error == false){
			int fpos = hf.ftell();
			buffer = hf.readLine();
			if(buffer.equals("[POSITION]") == true){
				hf.fseek(fpos, Sasakama_File.SEEK_SET);
				break;
			}
			
			Matcher m = p.matcher(buffer);
			if(m.find()){
				String str1 = m.group(1);
				String str2 = m.group(2);
				String str3 = m.group(3);
					
				switch(str1){
				case "VECTOR_LENGTH":
					for(int j=0;j < num_streams;j++)
						if(stream_type_list[j].equals(str2)){
							temp_vector_length[j] = Integer.parseInt(str3);
							break;
						}
					break;
				case "IS_MSD":
					for(int j=0;j < num_streams;j++)
						if(stream_type_list[j].equals(str2)){
							temp_is_msd[j] = (str3.equals("1")) ? true : false;
							break;
						}
					break;
				case "NUM_WINDOWS":
					for(int j=0;j < num_streams;j++)
						if(stream_type_list[j].equals(str2)){
							temp_num_windows[j] = Integer.parseInt(str3);
							break;
						}
					break;
				case "USE_GV":
					for(int j=0;j < num_streams;j++)
						if(stream_type_list[j].equals(str2)){
							temp_use_gv[j] = (str3.equals("1")) ? true : false;
							//System.err.printf("USE_GV[%d]:%s\n", j, temp_use_gv[j]?"true":"false");
							break;
						}
					break;
				case "OPTION":
					for(int j=0;j < num_streams;j++)
						if(stream_type_list[j].equals(str2)){
							temp_option[j] = str3;
							break;
						}
					break;
				default:
					Sasakama_Misc.error("Sasakama_ModelSet.load_stream: Unknown option "+str1);
					error = true;
				}
			}
			else{
				error = true;
				break;
			}
		}
		if(error == false){
			if(i == 0){
				vector_length = temp_vector_length;
				is_msd        = temp_is_msd;
				num_windows   = temp_num_windows;
				use_gv        = temp_use_gv;
				option        = temp_option;
			}
			else{
				for(int j=0;j < num_streams;j++){						
					if(vector_length[j] != temp_vector_length[j]){
						error = true;
						break;
					}
					if(is_msd[j] != temp_is_msd[j]){
						error = true;
						break;
					}
					if(num_windows[j] != temp_num_windows[j]){
						error = true;
						break;
					}
					if(use_gv[j] != temp_use_gv[j]){
						error = true;
						break;
					}
					if(!option[j].equals(temp_option[j])){
						error = true;
						break;
					}
				}				
			}
		}

		return(!error);		
	}
	
	private Boolean load_position_info(Sasakama_File hf){
		/*
		[POSITION]
		DURATION_PDF:0-1163
		DURATION_TREE:1164-4493
		STREAM_WIN[MCP]:4494-4499,4500-4514,4515-4529
		STREAM_WIN[LF0]:4530-4535,4536-4550,4551-4565
		STREAM_WIN[LPF]:4566-4571
		STREAM_PDF[MCP]:4572-315991
		STREAM_PDF[LF0]:315992-350955
		STREAM_PDF[LPF]:350956-352215
		STREAM_TREE[MCP]:352216-402230
		STREAM_TREE[LF0]:402231-513885
		STREAM_TREE[LPF]:513886-513990
		 */
		String buffer = null;
		Boolean error = false;
		
		String temp_duration_pdf  = null;
		String temp_duration_tree = null;
		ArrayList<String[]> temp_stream_win = new ArrayList<String[]>();
		for(int j=0;j < num_streams;j++){
			String[] dd = new String[num_windows[j]];
			temp_stream_win.add(dd);	
		}
		
		String[] temp_stream_pdf  = new String[num_streams];
		String[] temp_stream_tree = new String[num_streams];
		String[] temp_gv_pdf      = new String[num_streams];
		String[] temp_gv_tree     = new String[num_streams];
	
		String[] stream_type_list = stream_type.split(",");
		Pattern p1 = Pattern.compile("^([^\\[]+):([^\\]]+)$");
		Pattern p2 = Pattern.compile("^([^\\[]+)\\[(.+)\\]:(.+)$");
		
		buffer = hf.readLine();
		if(Sasakama_Misc.strequal(buffer, "[POSITION]") != true){
			error = true;
		}
		
		while(error == false){
			int fpos = hf.ftell();
			buffer = hf.readLine();
		
			if(buffer.equals("[DATA]") == true){
				hf.fseek(fpos, Sasakama_File.SEEK_SET);
				break;
			}
			
			String str1 = null, str2 = null, str3 = null;
			Matcher m1 = p1.matcher(buffer);
			Matcher m2 = p2.matcher(buffer);
			if(m1.find()){
				str1 = m1.group(1);
				str2 = m1.group(2);
			}
			else if(m2.find()){
				str1 = m2.group(1);
				str2 = m2.group(2);
				str3 = m2.group(3);
			}
			else{
				error = true;
				break;
			}
		//	System.err.printf("str1:%s, str2:%s, str3:%s\n", str1, str2, str3);
			switch(str1){
			case "DURATION_PDF":
				temp_duration_pdf = str2;
				break;
			case "DURATION_TREE":
				temp_duration_tree = str2;
				break;
			case "STREAM_WIN":
				for(int j=0;j < num_streams;j++){
					if(stream_type_list[j].equals(str2)){
						String[] temp_list = str3.split(",");
						if(num_windows[j] != temp_list.length){
							error = true;
							break;
						}
						for(int k=0;k < num_windows[j];k++)
							temp_stream_win.get(j)[k] = temp_list[k];
						break;
					}
				}
				break;
			case "STREAM_PDF":
				for(int j=0;j < num_streams;j++){
					if(stream_type_list[j].equals(str2)){
						temp_stream_pdf[j] = str3;
						break;
					}
				}
				break;
			case "STREAM_TREE":
				for(int j=0;j < num_streams;j++){
					if(stream_type_list[j].equals(str2)){
						temp_stream_tree[j] = str3;
						break;
					}
				}
				break;
			case "GV_PDF":
				for(int j=0;j < num_streams;j++){
					if(stream_type_list[j].equals(str2)){
						temp_gv_pdf[j] = str3;
						break;
					}
				}
				break;
			case "GV_TREE":
				for(int j=0;j< num_streams;j++){
					if(stream_type_list[j].equals(str2)){
						temp_gv_tree[j] = str3;
						break;
					}
				}
				break;
			default:
				Sasakama_Misc.error("Sasakama_ModelSet.load_position: Unknown option "+str1);
				break;
			}
		}

		if(temp_duration_pdf == null)
			error = true;
		for(int j=0;j < num_streams;j++){
			for(int k=0;k < num_windows[j];k++)
				if(temp_stream_win.get(j)[k] == null){
					error = true;
					break;
				}

			if(temp_stream_pdf[j] == null){
				error = true;
				break;
			}
		}
		
		if(error == false){
			duration_pdf = temp_duration_pdf;
			duration_tree= temp_duration_tree;
			stream_win   = temp_stream_win;
			stream_pdf   = temp_stream_pdf;
			stream_tree  = temp_stream_tree;
			gv_pdf       = temp_gv_pdf;
			/*
			for(int kk=0;kk < gv_pdf.length;kk++)
				System.err.printf("gv_pdf[%d]:%s\n", kk, gv_pdf[kk]);
				*/
			gv_tree      = temp_gv_tree;
		}
		
		return (!error);
	}
	
	private Boolean load_data_info(Sasakama_File hf, int i){
		String buffer = null;
		//System.err.printf("i:%d\n", i);
		
		buffer = hf.readLine();
		if(Sasakama_Misc.strequal(buffer, "[DATA]") != true){
			return false;
		}
		
		if(load_duration(hf, i) != true){
			Sasakama_Misc.error("error: load_duration");
			return false;
		}
		if(load_windows(hf) != true){
			Sasakama_Misc.error("error: load_window");
			return false;
		}
		if(load_streams(hf, i) != true){
			Sasakama_Misc.error("error: load_streams");
			return false;
		}
		if(load_gv(hf, i) != true){
			Sasakama_Misc.error("error: load_gv");
			return false;
		}
		return true;
	}
	
	private Boolean load_duration(Sasakama_File hf, int i){
		Boolean error = false;
		int start_of_data = hf.ftell();
		
		/* pdf */
		String[] pdf_list = duration_pdf.split("-");
		int s = Integer.parseInt(pdf_list[0]);
		int e = Integer.parseInt(pdf_list[1]);
		hf.fseek(s, Sasakama_File.SEEK_CUR);
		
		//System.err.printf("s:%d e:%d\n", s, e);
		
		Sasakama_File hf_pdf = new Sasakama_File();
		hf_pdf.read(hf, e-s+1);
		hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
		//System.err.printf("hf_pdf.size:%d\n", hf_pdf.buffer.length);
		/* tree */
		String[] tree_list = duration_tree.split("-");
		//System.err.println("tree:"+duration_tree);
		s = Integer.parseInt(tree_list[0]);
		e = Integer.parseInt(tree_list[1]); 
		hf.fseek(s,  Sasakama_File.SEEK_CUR);
		//System.err.printf("s:%d e:%d\n", s, e);
		
		Sasakama_File hf_tree = new Sasakama_File();
		hf_tree.read(hf, e-s+1);
		hf.fseek(start_of_data,  Sasakama_File.SEEK_SET);
		
		if(duration[i].load(hf_pdf, hf_tree, num_states, 1, false) != true)
			error = true;
		
		hf_tree.close();
		hf_pdf.close();
		
		return(!error);
	}
	
	private Boolean load_windows(Sasakama_File hf){
		Boolean error = false;
		int start_of_data = hf.ftell();
		
		for(int j=0;j < num_streams;j++){
			Sasakama_File[] win_fp = new Sasakama_File[num_windows[j]];
			
			for(int k=0;k < num_windows[j];k++){
				String[] temp_list = stream_win.get(j)[k].split("-");
				int s = Integer.parseInt(temp_list[0]);
				int e = Integer.parseInt(temp_list[1]);  
			
				hf.fseek(s,  Sasakama_File.SEEK_CUR);
				win_fp[k] = new Sasakama_File();
				win_fp[k].read(hf, e-s+1);
				hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
			}
				
			if(window[j].load(win_fp) != true){
				error = true;
				break;
			}
			for(int k=0;k < num_windows[j];k++)
				win_fp[k].close();
		}
		return(!error);
	}
	
	private Boolean load_streams(Sasakama_File hf, int i){
		Boolean error = false;
		int start_of_data = hf.ftell();
		
		for(int j=0;j < num_streams;j++){
			String[] temp_list_pdf = stream_pdf[j].split("-");
			int s = Integer.parseInt(temp_list_pdf[0]);
			int e = Integer.parseInt(temp_list_pdf[1]);   
			hf.fseek(s, Sasakama_File.SEEK_CUR);
			Sasakama_File pdf_hf = new Sasakama_File();
			pdf_hf.read(hf, e-s+1);
			hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
			
			String[] temp_list_tree = stream_tree[j].split("-");
			s = Integer.parseInt(temp_list_tree[0]);
			e = Integer.parseInt(temp_list_tree[1]);
			hf.fseek(s, Sasakama_File.SEEK_CUR);
			Sasakama_File tree_hf = new Sasakama_File();
			tree_hf.read(hf, e-s+1);
			hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
			
			if(stream[i][j].load(pdf_hf, tree_hf, vector_length[j], num_windows[j], is_msd[j]) != true){
				error = true;
				break;
			}
			tree_hf.close();
			pdf_hf.close();
		}
		return(!error);
	}
	
	private Boolean load_gv(Sasakama_File hf, int i){
		Boolean error = false;
		int start_of_data = hf.ftell();
		
		for(int j=0;j < num_streams;j++){
			if(use_gv[j] == true){
			//	System.err.printf("gv_pdf:%s\n", gv_pdf[j]);
				String[] temp_list_pdf = gv_pdf[j].split("-");
				int s = Integer.parseInt(temp_list_pdf[0]);
				int e = Integer.parseInt(temp_list_pdf[1]);   
				hf.fseek(s, Sasakama_File.SEEK_CUR);
				Sasakama_File pdf_hf = new Sasakama_File();
				pdf_hf.read(hf, e-s+1);
				hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
			
				String[] temp_list_tree = gv_tree[j].split("-");
				s = Integer.parseInt(temp_list_tree[0]);
				e = Integer.parseInt(temp_list_tree[1]);
				hf.fseek(s, Sasakama_File.SEEK_CUR);
				Sasakama_File tree_hf = new Sasakama_File();
				tree_hf.read(hf, e-s+1);
				hf.fseek(start_of_data, Sasakama_File.SEEK_SET);
			
				if(gv[i][j].load(pdf_hf, tree_hf, vector_length[j], 1, false) != true){
					error = true;
					break;
				}
				tree_hf.close();
				pdf_hf.close();
			}
		}
		return(!error);
	}
	
	public Boolean load(String [] voices){
		clear();
		
		if(voices == null || voices.length == 0)
			return false;
		
		num_voices = voices.length;
	
		duration = new Sasakama_Model[num_voices];
		for(int i=0;i < duration.length;i++)
			duration[i] = new Sasakama_Model();
				
		for(int i=0;i < num_voices;i++){
			Sasakama_File hf = new Sasakama_File();
			if(hf.open(voices[i], "r") != true){
				Sasakama_Misc.error("fail1");
				return false;
			}
			if(load_global_info(hf, i) != true){
				Sasakama_Misc.error("fail2");
				return false;
			}
			else{
				window   = new Sasakama_Window[num_streams];
				for(int j=0;j < window.length;j++)
					window[j] = new Sasakama_Window();
				
				stream   = new Sasakama_Model[num_voices][num_streams];
				gv       = new Sasakama_Model[num_voices][num_streams];

				for(int j=0;j < num_voices;j++){
					for(int k=0;k < num_streams;k++){
						stream[j][k] = new Sasakama_Model();
						gv[j][k] = new Sasakama_Model();
					}
				}
			}
			if(load_stream_info(hf, i) != true){
				Sasakama_Misc.error("fail3");
				return false;
			}
			if(load_position_info(hf) != true){
				Sasakama_Misc.error("fail4");
				return false;
			}
			if(load_data_info(hf, i) != true){
				Sasakama_Misc.error("fail5");
				return false;
			}
			hf.close();
		}
		
		String temp_string = "GV-Off { " + gv_off_context_string + " }";
		Sasakama_File temphf = new Sasakama_File();
		temphf.open(temp_string);
		gv_off_context = new Sasakama_Question();
		gv_off_context.initialize();
		gv_off_context.load(temphf);
		temphf.close();
				
		return true;
	}
	
	public int get_sampling_frequency(){
		return sampling_frequency;
	}
	
	public int get_fperiod(){
		return frame_period;
	}
	
	public String get_option(int stream_index){
		return option[stream_index];
	}
	
	public Boolean get_gv_flag(final String string){
		if(gv_off_context == null)
			return true;
		else if(gv_off_context.match(string) == true)
			return false;
		else
			return true;
	}
	
	public int get_nstate(){
		return num_states;
	}
	
	public String get_fullcontext_label_format(){
		return fullcontext_format;
	}
	
	public String get_fullcontext_label_version(){
		return fullcontext_version;
	}
	
	
	public int get_nstream(){
		return num_streams;
	}
	
	public int get_nvoices(){
		return num_voices;
	}
	
	public int get_vector_length(int stream_index){
		return stream[0][stream_index].vector_length;
	}
	
	public Boolean is_msd(int stream_index){
		return stream[0][stream_index].is_msd;
	}
	
	public int get_window_size(int stream_index){
		return window[stream_index].size;
	}
	
	public int get_window_left_width(int stream_index, int window_index){
		return window[stream_index].l_width[window_index];
	}
	
	public int get_window_right_width(int stream_index, int window_index){
		return window[stream_index].r_width[window_index];
	}
	
	public double get_window_coefficient(int stream_index, int window_index, int coefficient_index){
		return window[stream_index].get_coefficient(window_index, coefficient_index);
	}
	
	public int get_window_max_width(int stream_index){
		return window[stream_index].max_width;
	}
	
	public Boolean use_gv(int stream_index){
		if(gv[0][stream_index].vector_length != 0)
			return true;
		else
			return false;
	}

	public void get_duration_index(int voice_index, String string, int[] tree_index, int[] pdf_index){
		duration[voice_index].get_index(2, string, tree_index, pdf_index);
		
	}
	
	public void get_duration(String string, final double[] iw, double[] mean, double[] vari, int base){		
		for(int i=0;i < num_states;i++){
			mean[base+i] = 0.0;
			vari[base+i] = 0.0;
		}
		for(int i=0;i < num_voices;i++)
			if(iw[i] != 0.0)
				duration[i].add_parameter(2, string, mean, vari, null, iw[i], base);
		/*
		for(int i=0;i < num_states;i++){
			System.err.printf("get_duration. mean[%d]:%5.2f vari[%d]:%5.2f\n", i, mean[base+i], i, vari[base+i]);
			
		}
		*/
	}
	
	
	public void get_parameter_index(int voice_index, int stream_index, int state_index, String string, int[] tree_index, int[] pdf_index){
		stream[voice_index][stream_index].get_index(state_index, string, tree_index, pdf_index);
	}
	
	public void get_parameter(int stream_index, int state_index, String string, final double[][] iw, double[] mean, double[] vari, double[] msd){
		int len = stream[0][stream_index].vector_length * stream[0][stream_index].num_windows;
		
		for(int i=0;i < len;i++){
			mean[i] = 0.0;
			vari[i] = 0.0;
		}
		if(msd != null)
			msd[0] = 0.0;
		
		for(int i=0;i < num_voices;i++){
			if(iw[i][stream_index] != 0.0)
				stream[i][stream_index].add_parameter(state_index, string, mean, vari, msd, iw[i][stream_index], 0);
		}
	}
	
	public void get_gv_index(int voice_index, int stream_index, final String string ,int[] tree_index, int[] pdf_index){
		gv[voice_index][stream_index].get_index(2, string, tree_index, pdf_index);
	}
	
	public void get_gv(int stream_index, final String string, final double[][] iw, double[] mean, double[] vari){
		int len = stream[0][stream_index].vector_length;
		
		for(int i=0;i < len;i++){
			mean[i] = 0.0;
			vari[i] = 0.0;
		}
		for(int i=0;i < num_voices;i++)
			if(iw[i][stream_index] != 0.0)
				gv[i][stream_index].add_parameter(2, string, mean, vari, null, iw[i][stream_index], 0);
		}
	}
