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

public class Sasakama_Vocoder {
	static final double ZERO  = 1e-10;
	
	static final double PI    = Math.PI;
	static final double PI2   = PI*2.0;
	
	static final int RANDMAX  = 32767;
	
	static final long SEED    = 1;
	
	static final int B0       = 0x00000001;
	static final int B28      = 0x10000000;
	static final int B31      = 0x80000000;
	static final int B31_     = 0x7fffffff;
	static final int Z        = 0x00000000;
	// ifdef Sasakama_EMBEDED
	/* static final int PADEORDER = 4;
	static final int IRLENG    = 384; */
	// else
	static final Boolean GAUSS = true;
	static final int PADEORDER = 5;
	static final int IRLENG    = 576;
	
	static final double CHECK_LSP_STABILITY_MIN = 0.25;
	static final int CHECK_LSP_STABILITY_NUM    = 4;
	
	static final Boolean NORMFLG1 = true;
	static final Boolean NORMFLG2 = false;
	static final Boolean MULGFLG1 = true;
	static final Boolean MULGFLG2 = false;
	static final Boolean NGAIN    = false;
	
	public Boolean is_first;
	int stage;
	double gamma;
	Boolean use_linear;
	Boolean use_log_gain;
	int fprd;
	long[] next;
	Boolean gauss;
	double rate;
	double pitch_of_curr_point;
	double pitch_counter;
	double pitch_inc_per_point;
	double [] excite_ring_buff;
	int excite_buff_size;
	int excite_buff_index;
	char sw;
	int x;
	double[] freqt_buff;
	int freqt_size;
	double [] spectrum2en_buff;
	int spectrum2en_size;
	double r1, r2, s;
	double[] postfilter_buff;
	int postfilter_size;
	double[] c;
	int cc, cinc, d1;
	double[] lsp2lpc_buff;
	int lsp2lpc_size;
	double[] gc2gc_buff;
	int gc2gc_size;
	
	Sasakama_Vocoder(){
	}
	
	public void initialize_excitation(double pitch, int nlpf){
		pitch_of_curr_point = pitch;
		pitch_counter       = pitch;
		pitch_inc_per_point = 0.0;

		if(nlpf > 0){
			excite_buff_size = nlpf;
			excite_ring_buff = new double[excite_buff_size];
			/*
			for(int i=0;i < excite_buff_size;i++)
				excite_ring_buff[i] = 0.0;
				*/
			excite_buff_index = 0;
		}
		else{
			excite_buff_size  = 0;
			excite_ring_buff  = null;
			excite_buff_index = 0;
		}
	}
	
	public void start_excitation(double pitch){
		if(pitch_of_curr_point != 0.0 && pitch != 0.0){
			pitch_inc_per_point = (pitch - pitch_of_curr_point) / fprd;
		}
		else{
			pitch_inc_per_point = 0.0;
			pitch_of_curr_point = pitch;
			pitch_counter       = pitch;
		}
	}
	
	public void excite_unvoiced_frame(double noise){
		int center = (excite_buff_size - 1) / 2;
		excite_ring_buff[(excite_buff_index + center) % excite_buff_size] += noise;
	}
	
	public void excite_voiced_frame(double noise, double pulse, final double[] lpf){
		int center = (excite_buff_size - 1) / 2;
		
		if(noise != 0.0){
			for(int i=0;i < excite_buff_size;i++){
				if(i == center)
					excite_ring_buff[(excite_buff_index + i) % excite_buff_size] += noise * (1.0 - lpf[i]);
				else
					excite_ring_buff[(excite_buff_index + i) % excite_buff_size] += noise * (0.0 - lpf[i]);
			}
		}
		
		if(pulse != 0.0)
			for(int i=0;i < excite_buff_size;i++)
				excite_ring_buff[(excite_buff_index + i) % excite_buff_size] += pulse * lpf[i];
	}
	
	public double get_excitation(final double[] lpf){
		double x;
		
		if(excite_buff_size > 0){
			double noise = white_noise();
			//System.err.printf("noise:%5.2f\n", noise);
			double pulse = 0.0;
			
			if(pitch_of_curr_point == 0.0){
				excite_unvoiced_frame(noise);
				//System.err.printf("get_excitation: unvoiced\n");
			}
			else{
				pitch_counter += 1.0;
				if(pitch_counter >= pitch_of_curr_point){
					pulse = Math.sqrt(pitch_of_curr_point);
					pitch_counter -= pitch_of_curr_point;
				}
				excite_voiced_frame(noise, pulse, lpf);
				pitch_of_curr_point += pitch_inc_per_point;
				//System.err.printf("get_excitation: voiced\n");
			}
			x = excite_ring_buff[excite_buff_index];
			excite_ring_buff[excite_buff_index] = 0.0;
			excite_buff_index = (excite_buff_index + 1) % excite_buff_size;
/*			if(excite_buff_index >= excite_buff_size)
				excite_buff_index = 0;
				*/
		}
		else{
			if(pitch_of_curr_point == 0.0)
				x = white_noise();
			else{
				pitch_counter += 1.0;
				if(pitch_counter >= pitch_of_curr_point){
					x = Math.sqrt(pitch_of_curr_point);
					pitch_counter -= pitch_of_curr_point;
				}
				else{
					x = 0.0;
				}
				pitch_of_curr_point += pitch_inc_per_point;
			}
		}
		
		return x;
	}
	
	public void end_excitation(double pitch){
		pitch_of_curr_point = pitch;
	}
	
	public void postfilter_mcp(double[] mcp, int mpos, final int m, double alpha, double beta){
		
		if(beta > 0.0 && m > 1){
			if(postfilter_size < m){
				postfilter_buff = new double[m+1];
				postfilter_size = m;
			}
			mc2b(mcp, mpos, postfilter_buff, 0, m, alpha);
			double e1 = b2en(postfilter_buff, 0, m, alpha);
			
			postfilter_buff[1] -= beta * alpha * postfilter_buff[2];
			for(int k=2;k <= m;k++)
				postfilter_buff[k] *= (1.0 + beta);
			
			double e2 = b2en(postfilter_buff, 0, m, alpha);
			postfilter_buff[0] += Math.log(e1/e2)/2.0;
			b2mc(postfilter_buff, 0, mcp, mpos, m, alpha);
		}
	}
	
	public void postfilter_lsp(double[] lsp, int lpos, int m, double alpha, double beta){
		
		if(beta > 0.0 && m > 1){
			if(postfilter_size < m){
				postfilter_buff = new double[m+1];
				postfilter_size = m;
			}
		}
		
		double e1 = lsp2en(lsp, lpos, m, alpha);
		
		for(int i=0;i <= m;i++){
			if(i > 1 && i < m){
				double d1 = beta * (lsp[i + 1] - lsp[i]);
				double d2 = beta * (lsp[i] - lsp[i-1]);
				postfilter_buff[i] = lsp[i-1] + d2 + (d2*d2 * ((lsp[i+1]-lsp[i-1]) - (d1 + d2))) / ((d2 * d2) + d1 * d1);
			}
			else
				postfilter_buff[i] = lsp[i];
		}
		movem(postfilter_buff, 0, lsp, 0, m+1);
		
		double e2 = lsp2en(lsp, lpos, m, alpha);
		
		if(e1 != e2){
			if(use_log_gain)
				lsp[lpos+0] += 0.5 * Math.log(e1 / e2);
			else
				lsp[lpos+0] *= Math.sqrt(e1 / e2);
		}
	}
	
	public void initialize(int m, Sasakama_Condition condition){
		is_first = true;
		stage    = condition.stage;
		
		if(stage != 0)
			gamma = -1.0 / stage;
		else
			gamma = 0.0;
		
		use_log_gain = condition.use_log_gain;
		fprd    = condition.fperiod;
		//	System.err.printf("fperiod:%d\n", fprd);
		
		use_linear = false;
		if(use_linear){
			next    = new long[1];
			next[0] = SEED;
		}
		else{
			next = new long[4];
			next[0] = 123456789L;
			next[1] = 362436069L;
			next[2] = 521288629L;
			next[3] = 88675123L;
		}
		gauss   = GAUSS;
		this.rate    = (double)condition.sampling_frequency;
		// System.err.printf("rate:%5.2f\n", this.rate);
		pitch_of_curr_point = 0.0;
		pitch_counter       = 0.0;
		pitch_inc_per_point = 0.0;

		excite_ring_buff  = null;
		excite_buff_size  = 0;
		excite_buff_index = 0;
		sw = 0;
		x = 0x55555555;
		
		/* init buffer */
		freqt_buff = null;
		freqt_size = 0;
		gc2gc_buff = null;
		gc2gc_size = 0;
		lsp2lpc_buff = null;
		lsp2lpc_size = 0;
		postfilter_buff = null;
		postfilter_size = 0;
		spectrum2en_buff = null;
		spectrum2en_size = 0;
		
		if(stage == 0){
			c = new double[m*(3 + PADEORDER) + 5 * PADEORDER + 6];
			cc = m+1;
			cinc = cc + m + 1;
			d1 = cinc + m + 1;
		}
		else{
			c = new double[(m+1) * (stage + 3)];
			cc = m + 1;
			cinc = cc + m + 1;
			d1 = cinc + m + 1;
		}
	}
	
	public void synthesize(int m, double lf0, double[] spectrum, int spos, int nlpf, double[] lpf, Sasakama_Condition condition, double[] rawdata, int rpos, Sasakama_Audio audio){
		double p;

		double alpha  = condition.alpha;
		double beta   = condition.beta;
		double volume = condition.volume;
		
		if(lf0 == Sasakama_Constant.LZERO)
			p = 0.0;
		else if (lf0 <= Sasakama_Constant.MIN_LF0)
			p = rate / Sasakama_Constant.MIN_F0;
		else if (lf0 >= Sasakama_Constant.MAX_LF0)
			p = rate / Sasakama_Constant.MAX_F0;
		else
			p = rate / Math.exp(lf0);
		
		//System.err.printf("p:%5.2f\n", p);
		
		if(is_first){
			initialize_excitation(p, nlpf);
			if(stage == 0){
				mc2b(spectrum, spos, c, 0, m, alpha);
			}
			else{
				movem(spectrum, spos, c, 0, m+1);
				lsp2mgc(c, 0, c, 0, m, alpha);
				mc2b(c, 0, c, 0, m, alpha);
				gnorm(c, 0, c, 0, m, gamma);
				for(int i=1;i <= m;i++)
					c[i] *= gamma;
			}
			is_first = false;
		}
		
		start_excitation(p);
		if(stage == 0){
			postfilter_mcp(spectrum, spos, m, alpha, beta);
			mc2b(spectrum, spos, c, cc, m, alpha);
			for(int i=0;i <= m;i++)
				c[cinc+i] = (c[cc+i] - c[i]) / fprd;
		}
		else{
			postfilter_lsp(spectrum, spos, m, alpha, beta);
			check_lsp_stability(spectrum, spos, m);
			lsp2mgc(spectrum, spos, c, cc, m, alpha);
			mc2b(c, cc, c, cc, m, alpha);
			gnorm(c, cc, c, cc, m, gamma);
			for(int i=1;i <= m;i++)
				c[cc+i] *= gamma;
			for(int i=0;i <= m;i++)
				c[cinc+i] = (c[cc+i] - c[i]) / fprd;
		}
		
		int rawidx = 0;
		for(int j=0;j < fprd;j++){
			double x = get_excitation(lpf);
			//System.err.printf("excite:%5.2f\n", x);
			if(stage == 0){
				if(x != 0.0)
					x *= Math.exp(c[0]);
				x = mlsadf(x, c, 0, m, alpha, PADEORDER, c, d1);
			}
			else{
				if(!NGAIN)
					x *= c[0];
				x = mglsadf(x, c, 0, m, alpha, stage, c, d1);
			}
			x *= volume;
			//System.err.printf("data:%5.2f\n", x);
			
			if(rawdata != null){
				rawdata[rpos+rawidx] = x;
				rawidx++;
			}
						
			if(audio != null){
				short xs;
				if(x > 32767.0)
					xs = 32767;
				else if (x < -32768.0)
		            xs = -32768;
		         else
		            xs = (short) x;
				audio.write(xs);
				//System.err.printf("audio:%d", xs);
			}
			
			for(int i=0;i <=m;i++)
				c[i] += c[cinc+i];
		}
	
		end_excitation(p);
		movem(c, cc, c, 0, m+1);
	}
	
	public void clear(){
	}
	
	private static final double pade[] = {
		   1.00000000000,
		   1.00000000000,
		   0.00000000000,
		   1.00000000000,
		   0.00000000000,
		   0.00000000000,
		   1.00000000000,
		   0.00000000000,
		   0.00000000000,
		   0.00000000000,
		   1.00000000000,
		   0.49992730000,
		   0.10670050000,
		   0.01170221000,
		   0.00056562790,
		   1.00000000000,
		   0.49993910000,
		   0.11070980000,
		   0.01369984000,
		   0.00095648530,
		   0.00003041721
		};

	private void movem(double[] a, int apos, double[] b, int bpos, final int nitem){
		if(apos > bpos){
			int cnt = 0;
			while(cnt < nitem){
				b[bpos+cnt] = a[apos+cnt];
				cnt++;
			}
		}
		else{
			int cnt = nitem-1;
			while(cnt >= 0){
				b[bpos+cnt] = a[apos+cnt];
				cnt--;
			}
		}
	}
	
	private double mlsafir(final double x, final double[] b, final int bpos, final int m, final double a, final double aa, double d[], final int dpos){
		double y = 0.0;
		
		d[dpos+0] = x;
		d[dpos+1] = aa * d[dpos+0] + a * d[dpos+1];
		
		for(int i=2;i <= m;i++)
			d[dpos+i] += a * (d[dpos+i+1] - d[dpos+i-1]);
		
		for(int i=2;i <= m;i++)
			y += d[dpos+i] * b[bpos+i];
		
		for(int i=m+1;i > 1;i--)
			d[dpos+i] = d[dpos+i-1];
		
		return y;
	}
	
	private double mlsadf1(double x, final double[] b, final int bpos, final int m, final double a, final double aa, final int pd, final double[] d, final int dpos, final double[] ppade, final int padepos){
		double v, out = 0.0;
		int pt = dpos+pd + 1;
		
		for(int i=pd;i >= 1;i--){
			int di = dpos+i;
			d[di] = aa * d[pt+i-1] + a * d[di];
			d[pt+i] = d[di] * b[bpos+1];
			v = d[pt+i] * ppade[padepos+i];
			x += (1 & i) == 1 ? v : -v;
			out += v;
		}
		
		d[pt+0] = x;
		out += x;
	
		return out;
	}
	
	private double mlsadf2(double x, final double[] b, final int bpos, final int m, final double a, final double aa, final int pd, final double[] d, int dpos, final double[] ppade, final int padepos){
		double out = 0.0;
		
		int pt = dpos + pd*(m+2);
		
		for(int i=pd;i >= 1;i--){
			d[pt+i] = mlsafir(d[pt+i-1], b, bpos, m, a, aa, d, dpos+(i-1)*(m+2));
			//System.err.printf("mlsafir:%5.2f\n", d[pt+i]);
			double v = d[pt+i] * ppade[padepos+i];
			x += (1 & i) == 1 ? v: -v;
			out += v;
		}
		
		d[pt+0] = x;
		out += x;
		
		return out;
	}
	
	private double mlsadf(double x, final double[] b, int bpos, final int m, final double a, final int pd, double[] d, int dpos){
		final double aa = 1-a*a;
		int padepos = pd * (pd +1) /2;
		
		x = mlsadf1(x, b, bpos, m, a, aa, pd, d, dpos, pade, padepos);
		//System.err.printf("mlsadf1:%5.2f\n", x);
		x = mlsadf2(x, b, bpos, m, a, aa, pd, d, dpos+2*(pd+1), pade, padepos);
		//System.err.printf("mlsadf2:%5.2f\n", x);
		return x;
	}
	
	private double rnd_linear(long[] next){
		next[0] = next[0] * 1103515245L + 12345;

		double r = (next[0] / 65536L) % 32768L;
		//System.err.printf("next:%d ret:%5.2f\n", next[0], r/(double)RANDMAX);
		return (r / RANDMAX);
	}
	
	private double rnd_xorshift(long[] next){
		long t = (next[0]^(next[0] << 11));
		next[0] = next[1];
		next[1] = next[2];
		next[2] = next[3];
		next[3] = (next[3]^(next[3] >>19))^(t^(t>>8));
		
		double r = (next[3] / 65536L) % 32768L;
		return r / RANDMAX;
	}
	
	private double rnd(long[] next){
		if(use_linear)
			return rnd_linear(next);
		else
			return rnd_xorshift(next);
	}
	
	private double nrandom(){
		if(sw == 0){
			sw = 1;
			do{
				r1 = 2 * rnd(next) - 1;
				r2 = 2 * rnd(next) - 1;
				s = r1 * r1 + r2 * r2;
			} while (s > 1 || s == 0);
			
			s = Math.sqrt(-2 * Math.log(s) / s);
			return (r1*s);
		}
		else{
			sw = 0;
			return (r2 * s);
		}
	}
	
	private int mseq(){
		int x0, x28;
		
		x >>= 1;
		if((x & B0 ) > 0)
			x0 = 1;
		else
			x0 = -1;
		if((x & B28) > 0)
			x28 = 1;
		else
			x28 = -1;
		if(x0 + x28 > 0)
			x &= B31_;
		else
			x |= B31;
		
		return(x0);
	}
	
	private void mc2b(double[] mc, int mcpos, double[] b, int bpos, int m, final double a){
		/*
		if(! (mcpos == bpos && mc.equals(b)) ){
			if(a != 0.0){
				b[bpos+m] = mc[mcpos+m];
				for(m--;m >= 0;m--)
					b[bpos+m] = mc[mcpos+m] - a * b[bpos+m + 1];
			}
			else
				movem(mc, mcpos, b, bpos, m+1);
		}
		else if(a != 0.0)
			for(m--;m >=0;m--)
				b[bpos+m] -= a * b[bpos+m + 1];
		*/
		b[bpos+m] = mc[mcpos+m];
		for(m--;m >= 0;m--)
			b[bpos+m] = mc[mcpos+m] - a * b[bpos+m + 1];
	}
	
	private void b2mc(final double[] b, int bpos, double[] mc, int mcpos, int m, final double a){
		double d = mc[mcpos+m] = b[bpos+m];
		for(m--;m >=0;m--){
			double o = b[bpos+m] + a * d;
			d = b[bpos+m];
			mc[mcpos+m] = o;
		}
	}
	
	private void freqt(final double[] c1, int c1pos, final int m1, final double[] c2, int c2pos, final int m2, final double a){
		final double b = 1 - a * a;
		
		if(m2 > freqt_size){
			freqt_buff = new double[m2+m2+2];
			freqt_size = m2;
		}
		
		int g = freqt_size + 1;
		/*
		for(int i=0;i < m2+1;i++)
			freqt_buff[g+i] = 0.0;
			*/
		for(int i=-m1;i <= 0;i++){
			if(0 <= m2)
				freqt_buff[g+0] = c1[c1pos-i] + a * (freqt_buff[0] = freqt_buff[g+0]);
			if(1 <= m2)
				freqt_buff[g+1] = b * freqt_buff[0] + a * (freqt_buff[1] = freqt_buff[g+1]);
			for(int j=2;j <= m2;j++)
				freqt_buff[g+j] = freqt_buff[j-1] + a * ((freqt_buff[j] = freqt_buff[g+j])-freqt_buff[g+j-1]);
			
		}
		
		movem(freqt_buff, g, c2, c2pos, m2+1);
	}
	
	private void c2ir(final double[] c, int cpos, final int nc, double[] h, int hpos, final int leng){
		h[hpos+0] = Math.exp(c[cpos+0]);
		
		for(int n=1;n < leng;n++){
			double d = 0.0;
			double upl = (n >= nc) ? nc-1:n;
			for(int k=1;k <= upl;k++)
				d += k * c[cpos+k] * h[hpos+n-k];
			h[hpos+n] = d / n;
		}
	}
	
	private double b2en(final double[] b, int bpos, final int m, final double a){
		double en = 0.0;
		
		if(spectrum2en_size < m){
			spectrum2en_buff = new double[(m+1)+2*IRLENG];
			spectrum2en_size = m;
		}
		
		int cep = m + 1;
		int ir = cep + IRLENG;
		
		b2mc(b, bpos, spectrum2en_buff, 0, m, a);
		freqt(spectrum2en_buff, 0, m, spectrum2en_buff, cep, IRLENG-1, -a);
		c2ir(spectrum2en_buff, cep, IRLENG, spectrum2en_buff, ir, IRLENG);
		
		for(int i=0;i < IRLENG;i++)
			en += spectrum2en_buff[ir+i] * spectrum2en_buff[ir+i];
		
		return en;
	}
	
	private void ignorm(double[] c1, int c1pos, double[] c2, int c2pos, int m, final double g){
		if(g != 0.0){
			double k = Math.pow(c1[c1pos+0], g);
			for(;m >=1;m--)
				c2[c2pos+m] = k * c1[c1pos+m];
			c2[c2pos+0] = (k - 1.0) / g;
		}
		else{
			movem(c1, c1pos+1, c2, c2pos+1, m);
			c2[c2pos+0] = Math.log(c1[c1pos+0]);
		}
	}
	
	private void gnorm(double[] c1, int c1pos, double[] c2, int c2pos, int m, final double g){
		
		if(g != 0.0){
			double k = 1.0 + g * c1[c1pos+0];
			for(; m >= 1;m--)
				c2[c2pos+m] = c1[c1pos+m] / k;
			c2[c2pos+0] = Math.pow(k, 1.0/g);
		}
		else{
			movem(c1, c1pos+1, c2, c2pos+1, m);
			c2[c2pos+0] = Math.exp(c1[c1pos+0]);
		}
	}
	
	private void lsp2lpc(double[] lsp, int lpos, double[] a, int apos, final int m){
		Boolean flag_odd = false;
		int mh1, mh2;
		
		if(m % 2 == 0)
			mh1 = mh2 = m / 2;
		else{
			mh1 = (m+1)/2;
			mh2 = (m-1)/2;
			flag_odd = true;
		}
		
		if(m > lsp2lpc_size){
			lsp2lpc_buff = new double[5*m+6];
			lsp2lpc_size = m;
		}
		
		int p = m;
		int q = p + mh2;
		
		int a0 = q + mh2;
		int a1 = a0 + (mh1+1);
		int a2 = a1 + (mh1+1);
		int b0 = a2 + (mh1+1);
		int b1 = b0 + (mh2+1);
		int b2 = b1 + (mh2+1);
		
		movem(lsp, lpos, lsp2lpc_buff, 0, m);
		
		for(int i=0;i < mh1 + 1;i++)
			lsp2lpc_buff[a0+i] = 0.0;

		for(int i=0;i < mh1 + 1;i++)
			lsp2lpc_buff[a1+i] = 0.0;
		
		for(int i=0;i < mh1 + 1;i++)
			lsp2lpc_buff[a2+i] = 0.0;
		
		for(int i=0;i < mh2 + 1;i++)
			lsp2lpc_buff[b0+i] = 0.0;
		
		for(int i=0;i < mh2 + 1;i++)
			lsp2lpc_buff[b1+i] = 0.0;
		
		for(int i=0;i < mh2 + 1;i++)
			lsp2lpc_buff[b2+i] = 0.0;
		
		for(int i=0, k=0;i < mh1;i++, k+=2)
			lsp2lpc_buff[p+i] = -2.0 * Math.cos(lsp2lpc_buff[k]);
		
		for(int i=0, k=0;i < mh2;i++, k+=2)
			lsp2lpc_buff[q+i] = -2.0 * Math.cos(lsp2lpc_buff[k+1]);
		
		double xx = 1.0;
		double xf = 0.0, xff = 0.0;
		
		for(int k=0;k <= m;k++){
			if(flag_odd){
				lsp2lpc_buff[a0+0] = xx;
				lsp2lpc_buff[b0+0] = xx - xff;
				xff = xf;
				xf = xx;
			}
			else{
				lsp2lpc_buff[a0+0] = xx + xf;
				lsp2lpc_buff[b0+0] = xx - xf;
				xf = xx;
			}
			
			for(int i=0;i < mh1;i++){
				lsp2lpc_buff[a0+i+1] = lsp2lpc_buff[a0+i] + 
						lsp2lpc_buff[p+i] * lsp2lpc_buff[a1+i] + lsp2lpc_buff[a2+i];
				lsp2lpc_buff[a2+i] = lsp2lpc_buff[a1+i];
				lsp2lpc_buff[a1+i] = lsp2lpc_buff[a0+i];
			}
			
			for(int i=0;i < mh2;i++){
				lsp2lpc_buff[b0+i+1] = lsp2lpc_buff[b0+i] + 
						lsp2lpc_buff[q+i] * lsp2lpc_buff[b1+i] + lsp2lpc_buff[b2+i];
				lsp2lpc_buff[b2+i] = lsp2lpc_buff[b1+i];
				lsp2lpc_buff[b1+i] = lsp2lpc_buff[b0+i];
			}
			
			if(k != 0)
				a[apos+k-1] = -0.5 * (lsp2lpc_buff[a0+mh1] + lsp2lpc_buff[b0+mh2]);
			xx = 0.0;
		}
		
		for(int i=m-1;i >= 0;i--)
			a[apos+ i + 1] = -a[apos+ i];

		a[apos + 0] = 1.0;
	}
	
	private void gc2gc(double[] c1, int c1pos, final int m1, final double g1, double[] c2, int c2pos, final int m2, double g2){

		if(m1 > gc2gc_size){
			gc2gc_buff = new double[m1+1];
			gc2gc_size = m1;
		}
		
		movem(c1, c1pos, gc2gc_buff, 0, m1 + 1);
		
		c2[c2pos+0] = gc2gc_buff[0];
		for(int i=1;i <= m2;i++){
			double ss1 = 0.0, ss2 = 0.0;
			int min = m1 < i ? m1 : i-1;

			for(int k=1;k <= min;k++){
				int mk = i - k;
				double cc = gc2gc_buff[k] * c2[c2pos+mk];
				ss2 += k * cc;
				ss1 += mk + cc;	
			}
			
			if(i <= m1)
				c2[c2pos+i] = gc2gc_buff[i] + (g2 * ss2 - g1 * ss1) / i;
			else
				c2[c2pos+i] = (g2 * ss2 - g1 * ss1) / i;
		}
	}
	
	private void mgc2mgc(double[] c1, int c1pos, final int m1, final double a1, final double g1, double[] c2, int c2pos, final int m2, final double a2, final double g2){
		double a;
		
		if(a1 == a2){
			gnorm(c1, c1pos, c2, c2pos, m1, g1);
			gc2gc(c1, c1pos, m1, g1, c2, c2pos, m2, g2);
			ignorm(c2, c2pos, c2, c2pos, m2, g2);
		}
		else{
			a = (a2 - a1) / (1-a1*a2);
			freqt(c1, c1pos, m1, c2, c2pos, m2, a);
			gnorm(c2, c2pos, c2, c2pos, m2, g1);
			gc2gc(c2, c2pos, m2, g1, c2, c2pos, m2, g2);
			ignorm(c2, c2pos, c2, c2pos, m2, g2);
		}
	}
	
	private void lsp2mgc(double[] lsp, int lpos, double[] mgc, int mpos, final int m, final double alpha){	
	
		lsp2lpc(lsp, 1, mgc, mpos, m);
		if(use_log_gain)
			mgc[mpos+0] = Math.exp(lsp[lpos+0]);
		else
			mgc[mpos+0] = lsp[lpos+0];
		
		if(NORMFLG1)
			ignorm(mgc, mpos, mgc, mpos, m, gamma);
		else if (MULGFLG1)
			mgc[mpos+0] = (1.0 - mgc[mpos+0]) * (double)stage;
		
		if(MULGFLG1)
			for(int i=m;i >=1;i--)
				mgc[mpos+i] *= -(double)stage;
		mgc2mgc(mgc, mpos, m, alpha, gamma, mgc, mpos, m, alpha, gamma);
		if(NORMFLG2)
			for(int i=m;i >= 1;i--)
				mgc[mpos+i] *= gamma;
	}
	
	private double mglsadff(double x, final double[] b, int bpos, final int m, final double a, double[] d, int dpos){

		double y = d[dpos+0] * b[bpos+1];
		for(int i=1;i < m;i++){
			d[dpos+i] += a * (d[dpos+i+1] - d[dpos+i-1]);
			y += d[dpos+i] * b[bpos+i+1];
		}
		x -= y;
		
		for(int i=m;i > 0;i--)
			d[dpos+i] = d[dpos+i-1];
		d[dpos+0] = a * d[dpos+0] + (1 - a * a) * x;
		
		return x;
	}

	private double mglsadf(double x, double[] b, int bpos, final int m, final double a, final int n, double[] d, int dpos){
		for(int i=0;i < n;i++)
			x = mglsadff(x, b, bpos, m, a, d, dpos+i*(m+1));
		
		return x;
	}
	
	private void check_lsp_stability(double[] lsp, int lpos, int m){
		double min = (CHECK_LSP_STABILITY_MIN * PI) / (m + 1);
		Boolean find;
		
		for(int i=0;i < CHECK_LSP_STABILITY_NUM;i++){
			find = false;
			
			for(int j=1;j < m;j++){
				int lj = lpos + j;
				double tmp = lsp[lj+1] - lsp[lj];
				if(tmp < min){
					lsp[lj] -= 0.5 * (min - tmp);
					lsp[lj+1] += 0.5 * (min - tmp);
					find = true;
				}
			}
			
			if(lsp[lpos+1] < min){
				lsp[lpos+1] = min;
				find = true;
			}
			if(lsp[lpos+m] > PI - min){
				lsp[lpos+m] = PI-min;
				find = true;
			}
			
			if(find == false)
				break;
		}
	}
	
	private double lsp2en(double[] lsp, int lpos, int m, double alpha){
		double en = 0.0;
		if(spectrum2en_size < m){
			spectrum2en_buff = new double[m+1+IRLENG];
			spectrum2en_size = m;
		}
		int buff = m + 1;
		
		lsp2lpc(lsp, lpos+1, spectrum2en_buff, 0, m);
		if(use_log_gain)
			spectrum2en_buff[0] = Math.exp(lsp[lpos+0]);
		else
			spectrum2en_buff[0] = lsp[lpos+0];
		
		if(NORMFLG1)
			ignorm(spectrum2en_buff, 0, spectrum2en_buff, 0, m, gamma);
		else if(MULGFLG1)
			spectrum2en_buff[0] = (1.0 - spectrum2en_buff[0])*(double)stage;
		
		if(MULGFLG1)
			for(int i=0;i <= m;i++)
				spectrum2en_buff[i] *= -(double)stage;
		
		mgc2mgc(spectrum2en_buff, 0, m, alpha, gamma, spectrum2en_buff, buff, IRLENG-1, 0.0, 1);
		
		for(int i=0;i < IRLENG;i++)
			en += spectrum2en_buff[buff+i] * spectrum2en_buff[buff+i];

		return en;
	}
	
	private double white_noise(){
		if(gauss)
			return nrandom();
		else
			return mseq();
	}
}
