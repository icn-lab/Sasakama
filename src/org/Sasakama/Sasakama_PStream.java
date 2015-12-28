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

public class Sasakama_PStream {
	final static double STEPINIT = 0.1;
	final static double STEPDEC  = 0.5;
	final static double STEPINC  = 1.2;
	final static double W1 = 1.0;
	final static double W2 = 1.0;
	final static int GV_MAX_ITERATION = 5;
	
	int vector_length;
	int length;
	int width;
	double[][] par;
	Sasakama_SMatrices sm;
	Sasakama_Window win;
	
	/*
	int win_size;
	int[] win_l_width;
	int[] win_r_width;
	double[][] win_coefficient;
	*/
	
	Boolean[] msd_flag;
	Sasakama_Gv gv;
	
	/*
	double[] gv_mean;
	double[] gv_vari;
	Boolean[] gv_switch;
	int gv_length;
	*/
	Sasakama_PStream(){	
	}
	
	/* calc_wuw_and_wum: calcurate W'U^{-1}W and W'U^{-1}M */
	public void calc_wuw_and_wum(int m){
		
		for(int t=0;t < length;t++){
			sm.wum[t] = 0.0;
			for(int i=0;i < width;i++)
				sm.wuw[t][i] = 0.0;
			
			/* calc WUW and WUM */
			for(int i=0;i < win.size;i++)
				for(int shift = win.l_width[i];shift <= win.r_width[i];shift++)
					if((t + shift >= 0) && (t + shift < length) && (win.get_coefficient(i, -shift) != 0.0)){
						double wu = win.get_coefficient(i, -shift) * sm.ivar[t + shift][i * vector_length + m];
						sm.wum[t] += wu * sm.mean[t + shift][i * vector_length + m];
						for(int j=0;(j < width) && (t + j < length);j++)
							if((j <= win.r_width[i] + shift) && (win.get_coefficient(i, j-shift) != 0.0))
									sm.wuw[t][j] += wu * win.get_coefficient(i, j-shift);
					}
		}
		/*
		for(int t=0;t < length;t++){
			for(int j=0;j < width;j++){
				System.err.printf("wuw[%d][%d]:%5.2f\n", t, j, sm.wuw[t][j]);
			}
		}
		for(int t=0;t < length;t++)
			System.err.printf("wum[%d]:%5.2f\n", t, sm.wum[t]);
			*/
	}
	
	/* ldl_factorization: Factorize W'*U^{-1}*W to L*D*L' (L: lower triangular, D: diagonal) */
	public void ldl_factorization(){
	//	System.err.printf("sm %dx%d\n", sm.wuw.length, sm.wuw[0].length);
	//	System.err.printf("length:%d width:%d\n", length, width);
		for(int t=0;t < length;t++){
			for(int i=1;(i < width) && (t >= i);i++)
				sm.wuw[t][0] -= sm.wuw[t-i][i] * sm.wuw[t-i][i] * sm.wuw[t-i][0];
			
			for(int i=1;i < width;i++){
				for(int j=1;(i+j < width) && (t>=j);j++)
					sm.wuw[t][i] -= sm.wuw[t-j][j] * sm.wuw[t-j][i+j] * sm.wuw[t-j][0];
				sm.wuw[t][i] /= sm.wuw[t][0];
				//System.err.printf("ldl sm.wuw[%d][%d]:%5.2f\n", t, i, sm.wuw[t][i]);
			}
		}
	}
	
	public void forward_substitution(){	
		for(int t = 0;t < length;t++){
			sm.g[t] = sm.wum[t];
			for(int i=1;(i < width) && (t >= i);i++)
				sm.g[t] -= sm.wuw[t-i][i] * sm.g[t-i];
			//System.err.printf("forward sm.g[%d]:%5.2f\n", t, sm.g[t]);
		}
	}
	
	public void backward_substitution(int m){
		for(int rev=0;rev < length;rev++){
			int t = length - 1 - rev;
			par[t][m] = sm.g[t] / sm.wuw[t][0];
			for(int i=1;(i < width) && (t+i < length);i++)
				par[t][m] -= sm.wuw[t][i] * par[t+i][m];
			//System.err.printf("backward par[%d][%d]:%5.2f\n", t, m, par[t][m]);
		}
	}
	
	public void calc_gv(int m, double[] mean, double[] vari){
		
		mean[0] = 0.0;
		for(int t=0;t < length;t++)
			if(gv.gv_switch[t]){
				mean[0] += par[t][m];
				//System.err.printf("calc_gv_if: [%d] true\n", t);
			}
		/*
			else
				System.err.printf("calc_gv_if: [%d] false\n", t);
*/
		mean[0] /= gv.length;
		
		vari[0] = 0.0;
		for(int t=0;t < length;t++)
			if(gv.gv_switch[t])
				vari[0] += (par[t][m] - mean[0]) * (par[t][m] - mean[0]);
		vari[0] /= gv.length;
		//System.err.printf("calc_gv: mean:%5.2f vari:%5.2f\n", mean[0], vari[0]);
	}
	
	public void conv_gv(int m){
		double[] mean = new double[1];
		double[] vari = new double[1];
	
		calc_gv(m, mean, vari);
		double ratio = Math.sqrt(gv.mean[m] / vari[0]);
		
		for(int t=0;t < length;t++)
			if(gv.gv_switch[t]){
				par[t][m] = ratio * (par[t][m] - mean[0]) + mean[0];
			//	System.err.printf("conv_gv: par[%d][%d]:%5.2f\n", t, m, par[t][m]);
			}
	}

	public double calc_derivative(int m){
		double[] mean = new double[1];
		double[] vari = new double[1];
		double w = 1.0 / (win.size * length);
				
		calc_gv(m, mean, vari);
		double gvobj = -0.5 * W2 * vari[0] * gv.vari[m] * (vari[0] - 2.0 * gv.mean[m]);
		double dv    = -2.0 * gv.vari[m] * (vari[0] - gv.mean[m])/length;
		
		for(int t=0;t < length;t++){
			sm.g[t] = sm.wuw[t][0] * par[t][m];
			for(int i=1;i < width;i++){
				if(t+i < length)
					sm.g[t] += sm.wuw[t][i] * par[t+i][m];
				if(t+1 > i)
					sm.g[t] += sm.wuw[t-i][i] * par[t-i][m];
			}
		}
		
		double hmmobj = 0.0;
		for(int t=0;t < length;t++){
			hmmobj += W1 * w * par[t][m] * (sm.wum[t] - 0.5 * sm.g[t]);
			double h = -W1 * w * sm.wuw[t][0] - W2 * 2.0 / (length * length) *
					((length-1) * gv.vari[m] * (vari[0] - gv.mean[m]) + 
							2.0 * gv.vari[m] * (par[t][m] - mean[0]) * (par[t][m]-mean[0]));
			
			if(gv.gv_switch[t])
				sm.g[t] = 1.0 / h * (W1 * w * (-sm.g[t] + sm.wum[t]) + W2 * dv * (par[t][m] - mean[0]));
			else
				sm.g[t] = 1.0 / h * (W1 * w * (-sm.g[t] + sm.wum[t]));
			
		//	System.err.printf("derivative sm.g[%d]:%5.2f\n", t, sm.g[t]);
		}
		
		return (-(hmmobj + gvobj));
	}
	
	public void gv_parmgen(int m){
		double step = STEPINIT;
		double prev = 0.0;
		
		if(length == 0)
			return;
		
		conv_gv(m);
		if(GV_MAX_ITERATION > 0){
			calc_wuw_and_wum(m);
			for(int i=1;i <= GV_MAX_ITERATION;i++){
				double obj = calc_derivative(m);
				if(i > 1){
					if(obj > prev)
						step *= STEPDEC;
					if(obj < prev)
						step *= STEPINC;
				}
				for(int t=0;t < length;t++)
					par[t][m] += step * sm.g[t];
				
				prev = obj;
			}
		}
	}
	
	public void mlpg(){
		if(length == 0)
			return;
		
		for(int m=0;m < vector_length;m++){
			calc_wuw_and_wum(m);
			ldl_factorization();
			forward_substitution();
			backward_substitution(m);
			if(gv.length > 0)
				gv_parmgen(m);
		}
	}
	
	public void print_par(){
		for(int i=0;i < par.length;i++){
			for(int j=0;j < par[i].length;j++){
				System.err.printf("par[%d][%d]:%5.2f\n", i, j, par[i][j]);
			}
		}
	}
	
}