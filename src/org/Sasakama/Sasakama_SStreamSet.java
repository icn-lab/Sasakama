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

public class Sasakama_SStreamSet {
	Sasakama_SStream[] sstream;
	int nstream;
	int nstate;
	int[] duration;
	int total_state;
	int total_frame;
	
	
	Sasakama_SStreamSet(){
		initialize();
	}
	
	public void initialize(){
		nstream = 0;
		nstate  = 0;
		sstream  = null;
		duration = null;
		total_state = 0;
		total_frame = 0;
	}
	
	public Boolean create(Sasakama_ModelSet ms, Sasakama_Label label, Sasakama_Condition condition){
		Boolean phoneme_alignment_flag = condition.phoneme_alignment_flag;
		double speed                   = condition.speed;
		double [] duration_iw          = condition.duration_iw;
		double[][] parameter_iw        = condition.parameter_iw;
		double[][] gv_iw               = condition.gv_iw;
		
		if(label.get_size() == 0)
			return false;
		
		double temp = 0.0;
		for(int i=0;i < ms.get_nvoices();i++)
			temp += duration_iw[i];

		if(temp == 0.0){
			return false;
		}
		else if(temp != 1.0){
			for(int i=0;i < ms.get_nvoices();i++)
				if(duration_iw[i] != 0.0)
					duration_iw[i] /= temp;
		}
		
		for(int i=0;i < ms.get_nstream();i++){
			temp = 0.0;
			for(int j=0;j < ms.get_nvoices();j++)
				temp += parameter_iw[j][i];
			
			if(temp == 0.0){
				return false;
			}
			else if(temp != 1.0){
				for(int j=0;j < ms.get_nvoices();j++)
					if(parameter_iw[j][i] != 0.0)
						parameter_iw[j][i] /= temp;
			}
			if(ms.use_gv(i)){
				temp = 0.0;
				for(int j=0;j < ms.get_nvoices();j++)
					temp += gv_iw[j][i];
				if(temp == 0.0)
					return false;
				else if(temp != 1.0)
					for(int j=0;j < ms.get_nvoices();j++)
						if(gv_iw[j][i] != 0.0)
							gv_iw[j][i] /= temp;
			}
		}
		
		/* initialize state sequence */
		nstate  = ms.get_nstate();
		nstream = ms.get_nstream();
		
		total_frame = 0;
		total_state = label.get_size() * nstate;
		
		duration = new int[total_state];
		sstream  = new Sasakama_SStream[nstream];
		
		//System.err.printf("nstate:%d nstream:%d\n", nstate, nstream);
		for(int i=0;i < nstream;i++){
			sstream[i] = new Sasakama_SStream();
			Sasakama_SStream sst = sstream[i];
			sst.vector_length = ms.get_vector_length(i);
			sst.mean = new double[total_state][sst.vector_length * ms.get_window_size(i)];
			sst.vari = new double[total_state][sst.vector_length * ms.get_window_size(i)];
			
			if(ms.is_msd(i))
				sst.msd = new double[total_state];
			else
				sst.msd = null;
			
			if(ms.use_gv(i)){
				sst.gv = new Sasakama_Gv();
				sst.gv.gv_switch = new Boolean[total_state];
				for(int j=0;j < total_state;j++)
					sst.gv.gv_switch[j] = true;
			}
			else{
				sst.gv = null;
			}
		}
		
		double[] duration_mean = new double[total_state];
		double[] duration_vari = new double[total_state];
		
		for(int i=0;i < label.get_size();i++){
	//		System.err.printf("label[%d]:%s\n", i, label.get_string(i));
			ms.get_duration(label.get_string(i), duration_iw, duration_mean, duration_vari, i*nstate);
		}
		if(phoneme_alignment_flag == true){
				/* use duration set by user */
				int next_time = 0;
				int next_state = 0;
				int state = 0;
				for(int i=0;i < label.get_size();i++){
					temp = label.get_end_frame(i);
					if(temp >= 0.0){
						next_time += set_specified_duration(duration, duration_mean, duration_vari, 
								state + nstate-next_state, temp-next_time, next_state);
						next_state = state + nstate;
					}
					else if(i+1 == label.get_size()){
						Sasakama_Misc.error("Sasakama_SStreamSet.create: The time of final label is not specified.");
						set_default_duration(duration, duration_mean, duration_vari, 
								state + nstate - next_state, next_state);
					}
					state += nstate;
				}
		}
		else{
			/* determine frame length */
			if(speed != 1.0){
				temp = 0.0;
				for(int i = 0;i < total_state;i++){
					temp += duration_mean[i];
				}
				double frame_length = temp / speed;
				set_specified_duration(duration, duration_mean, duration_vari, total_state, frame_length, 0);
			}
			else{
				set_default_duration(duration, duration_mean, duration_vari, total_state, 0);
			}
		}
		
		/* get parameter */
		int state = 0;
		for(int i=0;i < label.get_size();i++){
			for(int j=2;j <= nstate+1;j++){
				//System.err.printf("duration[%d]:%d\n", state, duration[state]);
				total_frame += duration[state];
				for(int k=0;k < nstream;k++){
					Sasakama_SStream sst = sstream[k];
					if(sst.msd != null){
						double[] temp_msd = new double[1];
						temp_msd[0] = sst.msd[state];
						ms.get_parameter(k, j, label.get_string(i), parameter_iw, sst.mean[state], sst.vari[state], temp_msd);
						sst.msd[state] = temp_msd[0];
					}
					else
						ms.get_parameter(k, j, label.get_string(i), parameter_iw, sst.mean[state], sst.vari[state], null);
				}
				state++;
			}
		}
		
		/* copy dynamic window */
		for(int i=0;i < nstream;i++){
			sstream[i].win = new Sasakama_Window(ms.window[i]);
		}
		
		/* determine GV */
		for(int i=0;i < nstream;i++){
			Sasakama_SStream sst = sstream[i];
			if(ms.use_gv(i)){
				sst.gv.mean = new double[sst.vector_length];
				sst.gv.vari = new double[sst.vector_length];
				ms.get_gv(i,  label.get_string(0), gv_iw, sst.gv.mean, sst.gv.vari);
			}
		}
		
		for(int i=0;i < label.get_size();i++){
			//System.err.printf("label.get_string: %s ", label.get_string(i));
			if(ms.get_gv_flag(label.get_string(i)) == false){
				//System.err.printf(" false\n");
				for(int j=0;j < nstream;j++)
					if(ms.use_gv(j) == true)
						for(int k=0;k < nstate;k++)
							sstream[j].gv.gv_switch[i * nstate + k] = false;
			}
			/*
			else
				System.err.printf(" true\n");
				*/
		}
		return true;
	}
	
	public int get_nstream(){
		return nstream;
	}
	
	public int get_vector_length(int stream_index){
		return sstream[stream_index].vector_length;
	}
	
	public Boolean is_msd(int stream_index){
		return sstream[stream_index].msd != null ? true: false;
	}
	
	public int get_total_state(){
		return total_state;
	}
	
	public int get_total_frame(){
		return total_frame;
	}
	
	public double get_msd(int stream_index, int state_index){
		return sstream[stream_index].msd[state_index];
	}
	
	public int get_window_size(int stream_index){
		return sstream[stream_index].win.size;
	}
	
	public int get_window_left_width(int stream_index, int window_index){
		return sstream[stream_index].win.l_width[window_index];
	}
	
	public int get_window_right_width(int stream_index, int window_index){
		return sstream[stream_index].win.r_width[window_index];
	}
	
	public double get_window_coefficient(int stream_index, int window_index, int coefficient_index){
		return sstream[stream_index].win.get_coefficient(window_index, coefficient_index);
	}
	
	public int get_window_max_width(int stream_index){
		return sstream[stream_index].win.max_width;
	}
	
	public Boolean use_gv(int stream_index){
		return sstream[stream_index].gv != null ? true : false;
	}
	
	public int get_duration(int state_index){
		return duration[state_index];
	}
	
	public double get_mean(int stream_index, int state_index, int vector_index){
		return sstream[stream_index].mean[state_index][vector_index];
	}
	
	public void set_mean(int stream_index, int state_index, int vector_index, double f){
		sstream[stream_index].mean[state_index][vector_index] = f;
	}
	
	public double get_vari(int stream_index, int state_index, int vector_index){
		return sstream[stream_index].vari[state_index][vector_index];
	}
	
	public void set_vari(int stream_index, int state_index, int vector_index, double f){
		sstream[stream_index].vari[state_index][vector_index] = f;
	}
	
	public double get_gv_mean(int stream_index, int vector_index){
		return sstream[stream_index].gv.mean[vector_index];
	}
	
	public double get_gv_vari(int stream_index, int vector_index){
		return sstream[stream_index].gv.vari[vector_index];
	}
	
	public void set_gv_switch(int stream_index, int state_index, Boolean flag){
		sstream[stream_index].gv.gv_switch[state_index] = flag;
	}
	
	public Boolean get_gv_switch(int stream_index, int state_index){
		return sstream[stream_index].gv.gv_switch[state_index];
	}
	
	public void clear(){
		initialize();
	}
	
	private double set_default_duration(int[] duration, double[] mean, double[] vari, int size, int base){
		int sum = 0;
		
		for(int i=0;i < size;i++){
			double temp = mean[base+i] + 0.5;
			if(temp < 1.0)
				duration[base+i] = 1;
			else
				duration[base+i] = (int)temp;
			sum += duration[base+i];
		}
		
		return sum;
	}
	
	private double set_specified_duration(int[] duration, double[] mean, double[] vari, int size, double frame_length, int base){
		int target_length = 0;
		
		if(frame_length + 0.5 < 1.0)
			target_length = 1;
		else
			target_length = (int)(frame_length + 0.5);
		
		if(target_length <= size){
			if(target_length < size)
				Sasakama_Misc.error("Sasakama_StreamSet. set_specified_duration: Specified frame length is too short.");
			for(int i=0;i < size;i++)
				duration[base+i] = 1;
			
			return (double)size;
		}
		
		/* RHO calculation */
		double temp1 = 0.0;
		double temp2 = 0.0;
		for(int i=0;i < size;i++){
			temp1 += mean[base+i];
			temp2 += vari[base+i];
		}
		double rho = (target_length-temp1)/temp2;
		
		int sum = 0;
		/* first estimation */
		for(int i=0;i < size;i++){
			temp1 = mean[base+i] + rho * vari[base+i] + 0.5;
			if(temp1 < 1.0)
				duration[base+i] = 1;
			else
				duration[base+i] = (int)temp1;
			sum += duration[base+i];
		}
		
		int j = 0;
		/* loop estimation */
		while (target_length != sum){
			if(target_length > sum){
				j = -1;
				for(int i=0;i < size;i++){
					temp2 = Math.abs(rho-(duration[base+i]+1-mean[base+i]) / vari[base+i]);
					if(j < 0 || temp1 > temp2){
						j = i;
						temp1 = temp2;
					}
				}
				sum++;
				duration[j]++;
			}
			else{
				j = -1;
				for(int i=0;i < size;i++){
					if(duration[base+i] > 1){
						temp2 = Math.abs(rho-(duration[base+i]-1-mean[base+i]) / vari[base+i]);
						if(j < 0 || temp1 > temp2){
							j = i;
							temp1 = temp2;
						}
					}
				}
				sum--;
				duration[base+j]--;
			}
		}
		
		return (double)target_length;
	}
}
