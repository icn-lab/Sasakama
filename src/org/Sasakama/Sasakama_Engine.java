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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class Sasakama_Engine {
	Sasakama_Condition condition;
	Sasakama_Audio audio;
	Sasakama_ModelSet ms;
	Sasakama_Label label;
	Sasakama_SStreamSet sss;
	Sasakama_PStreamSet pss;
	Sasakama_GStreamSet gss;

	public Sasakama_Engine(){
		initialize();
	}
	
	public void initialize(){
		condition = new Sasakama_Condition();
		
		/* global */
		condition.sampling_frequency = 0;
		condition.fperiod            = 0;
		condition.audio_buff_size    = 0;
		condition.stop               = false;
		condition.volume             = 1.0;
		
		/* duration */
		condition.speed                  = 1.0;
		condition.phoneme_alignment_flag = false;
		
		/* spectrum */
		condition.stage         = 0;
		condition.use_log_gain  = false;
		condition.alpha         = 0.0;
		condition.beta          = 0.0;
		
		/* log F0 */
		condition.additional_half_tone = 0.0;
		
		/* initialize audio */
		audio = new Sasakama_Audio();
		ms    = new Sasakama_ModelSet();
		label = new Sasakama_Label();
		sss   = new Sasakama_SStreamSet();
		pss   = new Sasakama_PStreamSet();
		gss   = new Sasakama_GStreamSet();
	}
	
	public Boolean load(String[] voices){
		
		/* reset engine */
		clear();
		
		if(ms.load(voices) != true){
			clear();
			return false;
		}
		
		int nstream = ms.get_nstream();
		double average_weight = 1.0 / voices.length;
		
		/* global */
		condition.sampling_frequency = ms.get_sampling_frequency();
		condition.fperiod            = ms.get_fperiod();
		condition.msd_threshold  = new double[nstream];
		condition.gv_weight      = new double[nstream];
		
		for(int i=0;i < nstream;i++){
			condition.msd_threshold[i] = 0.5;
			condition.gv_weight[i]     = 1.0;
		}
		
		/* spectrum */
		String option = ms.get_option(0);
		
		String regex  = "GAMMA=(-?\\d+\\.?\\d+)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(option);
		if(m.find())
			condition.stage = Integer.parseInt(m.group(1));

		regex = "LN_GAIN=(-?\\d+\\.?\\d+)";
		p = Pattern.compile(regex);
		m = p.matcher(option);
		if(m.find()){
			int b = Integer.parseInt(m.group(1));
			if(b == 1){
				condition.use_log_gain = true;
			}
			else{
				condition.use_log_gain = false;
			}
		}
		
		regex = "ALPHA=(-?\\d+\\.?\\d+)";
		p = Pattern.compile(regex);
		m = p.matcher(option);
		if(m.find())
			condition.alpha = Double.parseDouble(m.group(1));
		
		/* interpolation weights */
		condition.duration_iw = new double[voices.length];
		for(int i=0;i < voices.length;i++)
			condition.duration_iw[i] = average_weight;
		
		condition.parameter_iw = new double[voices.length][nstream];
		condition.gv_iw = new double[voices.length][nstream];
		
		for(int i=0;i < nstream;i++)
			for(int j=0;j < voices.length;j++){
				condition.parameter_iw[j][i] = average_weight;
				condition.gv_iw[j][i] = average_weight;
			}
		
		return true;
	}
	
	public void set_sampling_frequency(int i){
		if(i < 1)
			i = 1;
		
		condition.sampling_frequency = i;
		audio.set_parameter(condition.sampling_frequency, condition.audio_buff_size);
	}
	
	public int get_sampling_frequency(){
		return condition.sampling_frequency;
	}
	
	public void set_fperiod(int i){
		if(i < 1)
			i = 1;
		condition.fperiod = i;
	}
	
	public int get_fperiod(){
		return condition.fperiod;
	}
	
	public void close_audio(){
		audio.stopThread();
	}
	
	public void set_audio_buff_size(int i){
		condition.audio_buff_size = i;
		//System.err.printf("set_audio_buff_size:%d\n", condition.audio_buff_size);
		audio.set_parameter(condition.sampling_frequency, condition.audio_buff_size);
	}
	
	public void set_stop_flag(Boolean b){
		condition.stop = b;
	}
	
	public Boolean get_stop_flag(){
		return condition.stop;
	}
	
	public void set_volume(double f){
		condition.volume = Math.exp(f* Sasakama_Constant.DB);
	}
	
	public double get_volume(){
		return Math.log(condition.volume) / Sasakama_Constant.DB;
	}
	
	public void set_msd_threshold(int stream_index, double f){
		if(f < 0.0)
			f = 0.0;
		if(f > 1.0)
			f = 1.0;
		
		condition.msd_threshold[stream_index] = f;
	}
	
	public double get_msd_threshold(int stream_index){
		return condition.msd_threshold[stream_index];
	}
	
	public void set_gv_weight(int stream_index, double f){
		if(f < 0.0)
			f = 0.0;
		condition.gv_weight[stream_index] = f;
	}
	
	public double get_gv_weight(int stream_index){
		return condition.gv_weight[stream_index];
	}
	
	public void set_speed(double f){
		if(f < 1.0E-06)
			f = 1.0E-06;
		condition.speed = f;
	}
	
	public void set_phoneme_alignment_flag(Boolean b){
		condition.phoneme_alignment_flag = b;
	}
	
	public void set_alpha(double f){
		if(f < 0.0)
			f = 0.0;
		if(f > 1.0)
			f = 1.0;
		condition.alpha = f;	
	}
	
	public double get_alpha(){
		return condition.alpha;
	}

	public void set_beta(double f){
		if(f < 0.0)
			f = 0.0;
		if(f > 1.0)
			f = 1.0;
		condition.beta = f;	
	}
	
	public double get_beta(){
		return condition.beta;
	}

	public void add_half_tone(double f){
		condition.additional_half_tone = f;
	}
	
	public void set_duration_interpolation_weight(int voice_index, double f){
		condition.duration_iw[voice_index] = f;
	}
	
	public double get_duration_interpolation_weight(int voice_index){
		return condition.duration_iw[voice_index];
	}
	
	public void set_parameter_interpolation_weight(int voice_index, int stream_index, double f){
		condition.parameter_iw[stream_index][voice_index] = f;
	}
	
	public double get_paramter_interpolation_weight(int voice_index, int stream_index){
		return condition.parameter_iw[stream_index][voice_index];
	}
	
	public void set_gv_interpolation_weight(int voice_index, int stream_index, double f){
		condition.gv_iw[stream_index][voice_index] = f;
	}
	
	public double get_gv_interpolation_weight(int voice_index, int stream_index){
		return condition.gv_iw[stream_index][voice_index];
	}
	
	public int get_total_state(){
		return sss.get_total_state();
	}
	
	public void set_state_mean(int stream_index, int state_index, int vector_index, double f){
		sss.set_mean(stream_index, state_index, vector_index, f);
	}
	
	public double get_state_mean(int stream_index, int state_index, int vector_index){
		return sss.get_mean(stream_index, state_index, vector_index);
	}
	
	public int get_state_duration(int state_index){
		return sss.get_duration(state_index);
	}
	
	public int get_nvoices(){
		return ms.get_nvoices();
	}
	
	public int get_nstream(){
		return ms.get_nstate();
	}
	
	public int get_nstate(){
		return ms.get_nstate();
	}
	
	public String get_full_context_label_format(){
		return ms.get_fullcontext_label_format();
	}
	
	public String get_fullcontext_label_version(){
		return ms.get_fullcontext_label_version();
	}
	
	public int get_total_frame(){
		return gss.get_total_frame();
	}
	
	public int get_nsamples(){
		return gss.get_total_nsamples();
	}
	
	public double get_generated_parameter(int stream_index, int frame_index, int vector_index){
		return gss.get_parameter(stream_index, frame_index, vector_index);
	}
	
	public double get_generated_speech(int index){
		return gss.get_speech(index);
	}
	
	public Boolean generate_state_sequence(){
		if(sss.create(ms, label, condition) != true){
			refresh();
			return false;
		}

		if(condition.additional_half_tone != 0.0){
			int state_index = 0;

			double f;
			for(int i=0;i < get_total_state();i++){
				f = get_state_mean(1, i, 0);
				f += condition.additional_half_tone * Sasakama_Constant.HALF_TONE;
				if(f < Sasakama_Constant.MIN_LF0)
					f = Sasakama_Constant.MIN_LF0;
				else if (f > Sasakama_Constant.MAX_LF0)
					f = Sasakama_Constant.MAX_LF0;
				set_state_mean(1, i, 0, f);
				state_index++;
				if(state_index >= get_nstate()){
					state_index = 0;
				}
			}
		}
		return true;
	}
	
	public Boolean generate_state_sequence_from_fn(String fn){
		refresh();
		label.load_from_fn(condition.sampling_frequency, condition.fperiod, fn);
		return generate_state_sequence();
	}
	
	public Boolean generate_state_sequence_from_strings(String[] lines){
		refresh();
		label.load_from_strings(condition.sampling_frequency,condition.fperiod, lines);
		return generate_state_sequence();
	}
	
	public Boolean generate_parameter_sequence(){
		return pss.create(sss, condition.msd_threshold, condition.gv_weight);
	}
	
	public Boolean generate_sample_sequence(){
		return gss.create(pss, condition, (condition.audio_buff_size > 0)? audio:null);
	}
	
	public Boolean synthesize(){
		if(generate_state_sequence() != true){
			refresh();
			return false;
		}
		if(generate_parameter_sequence() != true){
			refresh();
			return false;
		}
		if(generate_sample_sequence() != true){
			refresh();
			return false;
		}
		return true;
	}
	
	public Boolean synthesize_from_fn(String fn){
		label.load_from_fn(condition.sampling_frequency, condition.fperiod, fn);
		return synthesize();
		
	}
	
	public Boolean synthesize_from_strings(String[] lines){
		refresh();
		label.load_from_strings(condition.sampling_frequency, condition.fperiod, lines);
		return synthesize();
	}
	
	public void save_information(FileOutputStream fos){
		PrintStream ps = new PrintStream(fos, true);
		
		/* global parameter */
		ps.printf("[Global parameter]\n");
		ps.printf("Sampring frequency                     -> %8d(Hz)\n",    condition.sampling_frequency);
		ps.printf("Frame period                           -> %8d(point)\n", condition.fperiod);
		ps.printf("                                          %8.5f(msec)\n",1e+3 * condition.fperiod / condition.sampling_frequency);
		ps.printf("All-pass constant                      -> %8.5f\n",      condition.alpha);
		ps.printf("Gamma                                  -> %8.5f\n",      condition.stage == 0 ? 0.0 : -1.0 / condition.stage);
		
		if(condition.stage != 0){
			if(condition.use_log_gain == true)
				ps.printf("Log gain flag                          ->     TRUE\n");
			else
				ps.printf("Log gain flag                          ->    FALSE\n");
		}
		
		 ps.printf("Postfiltering coefficient              -> %8.5f\n",        condition.beta);
		 ps.printf("Audio buffer size                      -> %8d(sample)\n", condition.audio_buff_size);
		 ps.printf("\n");

		/* duration parameter */
		 ps.printf("[Duration parameter]\n");
		 ps.printf("Number of states                       -> %8d\n", ms.get_nstate());
		 ps.printf("         Interpolation size            -> %8d\n", ms.get_nvoices());
		   
		/* check interpolation */
		double temp = 0.0;
		for(int i=0;i < ms.get_nvoices();i++)
			temp += condition.duration_iw[i];
		for(int i=0;i < ms.get_nvoices();i++)
			if(condition.duration_iw[i] != 0.0)
				condition.duration_iw[i] /= temp;
		for(int i=0;i < ms.get_nvoices();i++)
			ps.printf("         Interpolation weight[%2d]      -> %8.0f(%%)\n", i, (float)(100 * condition.duration_iw[i]));
		ps.printf("\n");
		
		ps.printf("[Stream parameter]\n");
		for(int i=0;i < ms.get_nstream();i++){
			/* stream parameter */
			ps.printf("Stream[%2d] vector length               -> %8d\n", i, ms.get_vector_length(i));
		    ps.printf("           Dynamic window size         -> %8d\n", ms.get_window_size(i));
		    /* interpolation */
		    ps.printf("           Interpolation size          -> %8d\n", ms.get_nvoices());
		     
		    temp = 0.0;
			for(int j=0;j < ms.get_nvoices();j++)
				temp += condition.parameter_iw[j][i];
			for(int j=0;j < ms.get_nvoices();j++)
				if(condition.parameter_iw[j][i] != 0.0)
					condition.parameter_iw[j][i] /= temp;
			for(int j=0;j < ms.get_nvoices();j++)
				ps.printf("           Interpolation weight[%2d]    -> %8.0f(%%)\n", j, 100.0*condition.parameter_iw[i][j]);
			/* MSD */
			if(ms.is_msd(i)){
				ps.printf("           MSD flag                    ->     TRUE\n");
				ps.printf("           MSD threshold               -> %8.5f\n", condition.msd_threshold[i]);
			}
			else{
				ps.printf("           MSD flag                    ->    FALSE\n");
			}
			
			/* GV */
			if(ms.use_gv(i)){
				ps.printf("           GV flag                     ->     TRUE\n");
				ps.printf("           GV weight                   -> %8.0f(%%)\n", 100.0 * condition.gv_weight[i]);
				ps.printf("           GV interpolation size       -> %8d\n", ms.get_nvoices());
				
				/* interpolation */
				temp = 0.0;
				for(int j=0;j < ms.get_nvoices();j++)
					temp += condition.gv_iw[j][i];
				for(int j=0;j < ms.get_nvoices();j++)
					if(condition.gv_iw[j][i] != 0.0)
						condition.gv_iw[j][i] /= temp;
				for(int j=0;j < ms.get_nvoices();j++)
					ps.printf("           GV interpolation weight[%2d] -> %8.0f(%%)\n", j, 100.0 * condition.gv_iw[j][i]);
			}
			else{
				ps.printf("           GV flag                     ->    FALSE\n");
			}
		}
		ps.printf("\n");
		
		/* generated sequence */
		ps.printf("[Generated sequence]\n");
		ps.printf("Number of HMMs                         -> %8d\n", label.get_size());
		ps.printf("Number of stats                        -> %8d\n", label.get_size() * ms.get_nstate());
		ps.printf("Length of this speech                  -> %8.3f(sec)\n", (double)pss.get_total_frame() * condition.fperiod / condition.sampling_frequency);
		ps.printf("                                       -> %8d(frames)\n", pss.get_total_frame() * condition.fperiod);
		
		for(int i=0;i < label.get_size();i++){
			ps.printf("HMM[%2d]\n", i);
			ps.printf("  Name                                 -> %s\n", label.get_string(i));
			ps.printf("  Duration\n");
			for(int j=0;j < ms.get_nvoices();j++){
				ps.printf("    Interpolation[%2d]\n", j);
				int[] k = new int[1];
				int[] l = new int[1];
				ms.get_duration_index(j, label.get_string(i), k, l);
				ps.printf("      Tree index                       -> %8d\n", k[0]);
				ps.printf("      PDF index                        -> %8d\n", l[0]);
			}
			for(int j=0;j < ms.get_nstate(); j++){
				ps.printf("  State[%2d]\n", j+2);
				ps.printf("    Length                             -> %8d(frames)\n", sss.get_duration(i*ms.get_nstate()+j));
				
				for(int k=0;k < ms.get_nstream();k++){
					ps.printf("    Stream[%2d]\n", k);
					if(ms.is_msd(k)){
						if(sss.get_msd(k, i*ms.get_nstate()+j) > condition.msd_threshold[k])
							ps.printf("      MSD flag                         ->     TRUE\n");
						else
							ps.printf("      MSD flag                         ->    FALSE\n");
					}
					for(int l=0;l < ms.get_nvoices();l++){
						ps.printf("      Interpolation[%2d]\n", l);
						int[] m = new int[1];
						int[] n = new int[1];
						
					    ms.get_parameter_index(l, k, j+2, label.get_string(i), m, n); 
						ps.printf("        Tree index                     -> %8d\n", m[0]);
						ps.printf("        PDF index                      -> %8d\n", n[0]);
					}
				}
			}
		}
	}
	
	public String[] get_label(){
		ArrayList<String> array = new ArrayList<String>();
		
		int nstate = ms.get_nstate();
		double rate = condition.fperiod * Sasakama_Constant.TIME_CONSTANT / condition.sampling_frequency;
		
		for(int i=0, state = 0, frame = 0; i < label.get_size();i++){
			int duration = 0;
			for(int j=0;j < nstate;j++)
				duration += sss.get_duration(state++);
			String str = String.format("%d %d %s", (int)(frame*rate), (int)((frame+duration)*rate), label.get_string(i));
			array.add(str);
			frame += duration;
		}
		
		String[] retStr = new String[array.size()];
		for(int i=0;i < retStr.length;i++)
			retStr[i] = array.get(i);
		
		//String[] retStr = (String[])array.toArray();
		return retStr;
	}
	
	public void save_label(FileOutputStream fos){
		PrintStream ps = new PrintStream(fos, true);
		/*
		int nstate = ms.get_nstate();
		double rate = condition.fperiod * Sasakama_Constant.TIME_CONSTANT / condition.sampling_frequency;
		
		for(int i=0, state = 0, frame = 0; i < label.get_size();i++){
			int duration = 0;
			for(int j=0;j < nstate;j++)
				duration += sss.get_duration(state++);
			ps.printf("%d %d %s\n", (int)(frame*rate), (int)((frame+duration)*rate), label.get_string(i));
			frame += duration;
		}
		*/
		String[] label = get_label();
		for(int i=0;i <label.length;i++)
			ps.printf("%s\n", label[i]);

		ps.close();
	}
	
	public void save_generated_parameter(int stream_index, FileOutputStream fos){
		int elemSize  = Float.SIZE / Byte.SIZE;
		ByteBuffer bf = ByteBuffer.allocate(elemSize * gss.get_total_frame() * gss.get_vector_length(stream_index));
		bf.order(ByteOrder.LITTLE_ENDIAN);

		for(int i=0;i < gss.get_total_frame();i++)
			for(int j=0;j < gss.get_vector_length(stream_index);j++){
				float temp = (float)gss.get_parameter(stream_index, i, j);
				bf.putFloat(temp);
			}
		
		byte[] byteArray = bf.array();
		try {
				fos.write(byteArray);
		} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		}
	}
	
	public void save_generated_speech(FileOutputStream fos){
		int elemSize = Short.SIZE / Byte.SIZE;
		ByteBuffer bf = ByteBuffer.allocate(elemSize * gss.get_total_nsamples());
		bf.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i=0;i < gss.get_total_nsamples();i++){
			short temp;
			double x = gss.get_speech(i);
			if(x > 32767.0)
				temp = 32767;
			else if(x < -32768.0)
				temp = -32768;
			else
				temp = (short)x;

			bf.putShort(temp);
		}
		
		byte[] byteArray = bf.array();
		try {
				fos.write(byteArray);
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
	}
	
	public void save_riff(FileOutputStream fos){
		byte[] wavData = new byte[gss.get_total_nsamples()*Short.SIZE/Byte.SIZE];
		ByteBuffer bf  = ByteBuffer.wrap(wavData);
		/*
		System.err.printf("sample:%d\n", gss.get_total_nsamples());
		System.err.printf("sampling:%d", condition.sampling_frequency);
		*/
		for(int i=0;i < gss.get_total_nsamples();i++){
			double x = gss.get_speech(i);
			short temp;
			if(x > 32767.0)
				temp = 32767;
			else if(x < -32768.0)
				temp = -32768;
			else
				temp = (short)x;
			//System.err.printf("data:%d\n", temp);
			bf.putShort(temp);
		}
		
		AudioFormat fmt = new AudioFormat((float)condition.sampling_frequency, 16, 1, true, true);
		ByteArrayInputStream bis = new ByteArrayInputStream(bf.array());
		
		AudioInputStream ais = new AudioInputStream(
                	bis,
                	fmt,	
                	gss.get_total_nsamples());
		try {
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void refresh(){
		gss.clear();
		pss.clear();
		sss.clear();
		label.clear();
		condition.stop = false;
	}
	
	public void clear(){
		initialize();
	}
}

