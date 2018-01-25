package stochasticModel;

import ilog.concert.*;
import ilog.cplex.*;
import solution.Project;

import java.util.*;

public class RP {
	public static double solve(){
		double result = 0;
		
		//Sets
		int IMax = 5; // Number of hydro plants
		int JMax = 4; // Number of thermal plants
		int TMax = 24; // Number time periods
		int PMax = 4; // Number of projects
		int OmegaMax = 15; // Number of scenarios
		
		Set<Integer> P = new HashSet<Integer>(); // set of projects
		for(int p=0; p<PMax; p++){
			P.add(p);
		}
		Set<Integer> P_M = new HashSet<Integer>(); // set of mandatory projects
		int[][] P_C = {{},{},{},{}}; // set of projects, which conﬂict with project p
		int[][] P_A = {{},{},{},{}}; // set of projects, which are associated with project p
		Set<Integer> T = new HashSet<Integer>(); // set of stages
		Set<Integer> T1 = new HashSet<Integer>(); // set of stages incl. T+1
		for(int t=0; t<TMax; t++){
			T.add(t);
			T1.add(t+1);
		}
		T1.add(0);
		int[][] T_p = {{4,6,9},{4,8,12},{4,11,19},{4,14,22}}; // set of stages, in which project p can be realized
		Set<Integer> T_E = new HashSet<Integer>(); // set of stages, in which new CO 2 emission allowances become available
		for(int i=0; i< (int) Math.floor(TMax/5); i++){
			T_E.add(1+5*i);
		}
		Set<Integer> I = new HashSet<Integer>(); // set of hydro plants
		for(int i=0; i<IMax; i++){
			I.add(i);
		}
		int[] I_ex = {0,2,4}; // hydro plants, which already exist. Guajoya + Cerron Grande, 15 de Septiembre, Rio Lindo
		int[][] I_p = {{1},{3},{},{}}; // set of hydro plants, which are associated with project p
		int[][] U_i = {{},{0},{1},{},{3}}; // set of hydro plants, which are immediately upstream of plant i
		Set<Integer> J = new HashSet<Integer>(); // set of thermal plants
		for(int j=0; j<JMax; j++){
			J.add(j);
		}
		int[] J_ex = {0,3}; // thermal plants, which already exist.
		int[][] J_p = {{},{},{1},{2}}; // set of thermal plants, which are associated with project p
		Set<Integer> Omega = new HashSet<Integer>(); // set of scenarios
		for(int i=0; i<OmegaMax; i++){
			Omega.add(i);
		}
		
		//Parameters
		double[] U_min = {0, 0, 0, 0, 0}; // minimum release level of hydro generation in plant i
		double[] U_max = {1060.64, 547.2, 1982.02, 60, 70.71}; // maximum release level of hydro generation in plant i
		double[] V_min = {1156.66, 66.05, 219.24, 1270, 0}; // lower limit of hydro reservoir level in plant i
		double[] V_max = {2931.9, 107.09, 306.95, 1785, 0}; // upper limit of hydro reservoir level in plant i
		double[] G_min = {0, 0, 0, 0}; // minimum level of thermal generation in plant j
		double[] G_max = {303.3, 290.27, 46.66, 78.49}; // maximum level of thermal generation in plant j
		double[] rho = {0.24, 0.13, 0.07, 0.36, 0.84}; // hydro generation eﬃciency coeﬃcient of plant i
		double[][] c_inv = new double[PMax][TMax]; // discounted and adjusted investment cost for project p at stage t
		for(int p:P){
			for(int t:T_p[p]){
				if(p==0){
					c_inv[p][t] = 3333.46*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==1){
					c_inv[p][t] = 18594.6*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==2){
					c_inv[p][t] = 7.75*G_max[1]*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==3){
					c_inv[p][t] = 1.26*G_max[2]*Math.pow(10, 4)*Math.pow(1.001, t);
				}
			}
		}
		double[][] c_op = new double[JMax][TMax]; // thermal generation price in plant j at stage t
		for(int j:J){
			for(int t:T){
				if(j==0){
					c_op[j][t] = 10;
				}
				else if(j==1){
					c_op[j][t] = 25;
				}
				else if(j==2){
					c_op[j][t] = 68.7;
				}
				else if(j==3){
					c_op[j][t] = 56.41;
				}
			}
		}
		double[] M_r = new double[TMax]; // penalty for violating electricity demand fulﬁllment at stage t
		for(int t:T){
			M_r[t] = 6000;
		}
		double[] M_o = new double[TMax]; // penalty for emission overspending at stage t
		for(int t:T){
			M_o[t] = 80;
		}
		double[] B = {0, 0, 0.56, 1.45}; // CO2 emissions coeﬃcient of plant j,
		double[] E_allow = new double[TMax]; // emissions allowance, which become available at stage t
		for(int t:T){
			E_allow[t] = 190000*2*Math.pow(0.98, t);
		}
		double[] D_EV = {940.93, 872.4, 983.23, 951.7, 1029.93, 975.13, 1007.77, 995.57, 953.93, 986.27, 929.8, 948.2, 940.93, 872.4, 983.23, 951.7, 1029.93, 975.13, 1007.77, 995.57, 953.93, 986.27, 929.8, 948.2}; // electricity demand at stage t		
		double [][] D = new double[OmegaMax][TMax];
		double demand_deviation = 0.3;
		for(int omega:Omega){
			for(int t:T){
				if(omega < 5){ //Lower demand
					D[omega][t] = (1-demand_deviation) * D_EV[t];
				}
				else if(omega >= 5 && omega < 10){ //Expected demand
					D[omega][t] = D_EV[t];
				}
				else if(omega >= 10 && omega < 15){ //Higher demand
					D[omega][t] = (1+demand_deviation) * D_EV[t];
				}
				else{
					
				}
			}
		}
		double[][][] A = {{{68.3, 56.13, 49.02, 21.26, 93.74, 543.8, 554.7, 1053.15, 934.93, 1038.42, 107.3, 53.3, 47.94, 55.88, 25.98, 33.96, 105, 358.73, 414.35, 1158.14, 1085.01, 663.17, 117.16, 45.8},{60.80, 65.56, 49.28, 25.92, 103.39, 548.99, 570.50, 1045.11, 1065.31, 1037.08, 137.38, 51.69, 55.71, 55.64, 31.07, 46.92, 132.31, 369.10, 438.19, 1228.85, 1068.16, 711.92, 106.53, 33.21},{173.29, 187.97, 90.53, 92.28, 280.96, 956.71, 1037.08, 1720.07, 1909.79, 3675.57, 221.88, 120.80, 101.78, 97.25, 65.35, 96.16, 218.56, 602.90, 964.76, 2881.42, 1843.17, 1261.53, 238.20, 61.60},{19.04, 4.77, 6.85, 4.78, 28.94, 65.77, 54.70, 66.24, 115.86, 91.60, 43.16, 15.47, 11.20, 8.15, 0.36, 0.53, 32.75, 71.59, 62.34, 85.75, 123.06, 83.88, 46.97, 42.92},{24.09, 16.83, 14.46, 7.16, 46.87, 51.29, 23.08, 97.63, 73.63, 81.77, 39.34, 22.09, 26.31, 18.10, 14.89, 13.73, 25.00, 64.97, 59.79, 83.38, 82.82, 96.11, 51.06, 49.42}},{{40.44, 41.37, 51.96, 26.96, 118.12, 568.95, 632.64, 645.49, 1452.82, 841.29, 267.49, 86.24, 61.34, 42.58, 21.43, 14.00, 169.55, 434.42, 424.26, 748.88, 1295.48, 522.29, 89.94, 88.66},{43.39, 39.19, 47.68, 37.32, 147.58, 581.39, 758.26, 975.47, 1782.52, 1163.23, 259.46, 98.30, 70.71, 43.06, 25.98, 17.11, 226.32, 508.03, 456.40, 962.35, 1417.05, 702.54, 82.94, 92.67},{104.73, 94.11, 85.17, 71.28, 340.96, 997.92, 1333.31, 1702.39, 3703.45, 2810.98, 456.97, 233.82, 132.85, 80.32, 102.05, 40.18, 441.13, 877.91, 877.44, 1275.19, 2413.15, 1549.45, 186.88, 175.70},{12.44, 10.64, 11.08, 3.03, 44.75, 76.82, 66.89, 63.98, 81.69, 68.82, 38.32, 26.00, 12.74, 6.56, 10.43, 9.05, 22.20, 70.44, 97.34, 36.08, 80.71, 68.76, 40.43, 33.86},{16.77, 12.70, 10.14, 11.19, 50.17, 89.80, 124.52, 115.74, 128.25, 98.44, 51.01, 28.90, 25.95, 8.23, 11.72, 6.06, 38.85, 87.09, 91.00, 63.75, 83.35, 89.68, 50.77, 34.43}},{{57.32, 52.26, 34.82, 41.21, 111.69, 501.29, 731.74, 634.25, 1674.69, 803.79, 129.08, 85.98, 72.59, 47.90, 37.77, 46.91, 177.58, 537.32, 406.04, 1040.29, 545.36, 576.66, 116.90, 45.27},{61.60, 60.48, 45.53, 54.43, 117.85, 502.85, 736.56, 664.24, 1570.75, 787.45, 137.38, 80.35, 72.32, 53.22, 42.85, 49.25, 182.13, 515.81, 324.09, 1151.71, 565.06, 640.14, 121.82, 50.89}, {156.42, 164.02, 87.58, 153.45, 398.01, 973.56, 1199.39, 1794.53, 2432.07, 1218.14, 227.58, 212.40, 185.08, 140.31, 74.73, 111.97, 290.61, 1145.15, 643.35, 1576.77, 1158.11, 1331.43, 292.64, 110.35}, {11.72, 7.63, 6.10, 6.82, 24.70, 83.14, 100.30, 97.09, 71.37, 82.09, 24.79, 23.55, 7.64, 0.19, 8.07, 8.18, 38.39, 87.17, 145.18, 99.03, 115.01, 76.75, 39.55, 11.27}, {16.19, 8.89, 13.83, 11.43, 46.43, 89.94, 41.11, 96.04, 114.65, 96.46, 37.18, 36.05, 18.31, 13.38, 13.18, 12.78, 24.22, 75.65, 85.28, 79.94, 116.13, 83.57, 57.77, 35.92}}, {{66.69, 30.48, 38.57, 95.64, 144.64, 573.61, 769.24, 1200.46, 2094.59, 1573.83, 253.24, 101.51, 94.82, 77.90, 50.09, 47.69, 408.99, 835.66, 1196.17, 609.87, 1812.33, 683.79, 199.59, 125.89}, {61.60, 29.76, 32.68, 111.46, 216.95, 585.79, 763.34, 1290.99, 2024.35, 1347.24, 228.10, 91.07, 72.32, 62.90, 64.28, 64.80, 487.47, 987.55, 1242.78, 650.85, 1775.52, 691.03, 209.95, 152.67}, {115.17, 70.16, 61.60, 142.56, 353.55, 1168.99, 1178.50, 2324.85, 4256.06, 2488.23, 396.58, 176.77, 131.24, 89.51, 91.07, 98.50, 891.91, 1936.22, 2006.12, 1074.04, 3403.30, 1740.96, 655.78, 216.95}, {17.34, 12.77, 2.52, 17.13, 27.53, 71.48, 44.51, 80.68, 124.45, 113.83, 28.00, 24.63, 19.14, 11.35, 2.22, 6.25, 31.77, 89.10, 135.06, 117.92, 118.47, 76.96, 24.63, 26.22}, {22.21, 14.97, 3.53, 24.66, 22.23, 70.02, 76.49, 118.17, 118.79, 79.47, 60.65, 27.73, 25.98, 14.61, 9.88, 16.90, 40.35, 95.05, 111.34, 95.45, 94.80, 88.32, 44.46, 33.83}}, {{35.36, 22.25, 34.82, 29.81, 56.25, 203.21, 716.47, 1083.95, 2154.21, 624.60, 191.29, 86.52, 61.87, 43.06, 26.52, 113.01, 230.35, 622.86, 1101.09, 1143.41, 1197.51, 719.96, 237.43, 171.68}, {43.93, 31.69, 44.19, 46.66, 74.19, 328.67, 754.24, 1235.01, 2125.18, 651.65, 179.37, 94.01, 60.80, 49.35, 33.21, 102.12, 272.13, 685.58, 1227.78, 1176.62, 981.07, 645.23, 236.65, 181.60}, {93.21, 64.83, 110.89, 154.22, 238.91, 709.69, 2043.89, 2019.78, 3271.62, 1605.97, 317.26, 158.56, 111.42, 100.64, 99.64, 225.24, 592.73, 1472.00, 1736.14, 1740.42, 1867.80, 1231.53, 537.84, 300.25}, {0.67, 7.49, 6.75, 4.65, 30.35, 72.31, 66.14, 94.97, 70.59, 68.17, 42.50, 28.41, 13.87, 8.49, 10.20, 16.38, 25.01, 79.56, 67.37, 24.50, 44.56, 172.50, 41.70, 11.83}, {4.28, 7.14, 9.67, 6.24, 36.52, 98.41, 127.31, 107.21, 122.64, 83.90, 29.11, 22.86, 9.75, 6.08, 13.49, 9.62, 33.31, 52.78, 75.42, 90.99, 64.95, 94.72, 58.07, 36.00}},{{68.3, 56.13, 49.02, 21.26, 93.74, 543.8, 554.7, 1053.15, 934.93, 1038.42, 107.3, 53.3, 47.94, 55.88, 25.98, 33.96, 105, 358.73, 414.35, 1158.14, 1085.01, 663.17, 117.16, 45.8},{60.80, 65.56, 49.28, 25.92, 103.39, 548.99, 570.50, 1045.11, 1065.31, 1037.08, 137.38, 51.69, 55.71, 55.64, 31.07, 46.92, 132.31, 369.10, 438.19, 1228.85, 1068.16, 711.92, 106.53, 33.21},{173.29, 187.97, 90.53, 92.28, 280.96, 956.71, 1037.08, 1720.07, 1909.79, 3675.57, 221.88, 120.80, 101.78, 97.25, 65.35, 96.16, 218.56, 602.90, 964.76, 2881.42, 1843.17, 1261.53, 238.20, 61.60},{19.04, 4.77, 6.85, 4.78, 28.94, 65.77, 54.70, 66.24, 115.86, 91.60, 43.16, 15.47, 11.20, 8.15, 0.36, 0.53, 32.75, 71.59, 62.34, 85.75, 123.06, 83.88, 46.97, 42.92},{24.09, 16.83, 14.46, 7.16, 46.87, 51.29, 23.08, 97.63, 73.63, 81.77, 39.34, 22.09, 26.31, 18.10, 14.89, 13.73, 25.00, 64.97, 59.79, 83.38, 82.82, 96.11, 51.06, 49.42}},{{40.44, 41.37, 51.96, 26.96, 118.12, 568.95, 632.64, 645.49, 1452.82, 841.29, 267.49, 86.24, 61.34, 42.58, 21.43, 14.00, 169.55, 434.42, 424.26, 748.88, 1295.48, 522.29, 89.94, 88.66},{43.39, 39.19, 47.68, 37.32, 147.58, 581.39, 758.26, 975.47, 1782.52, 1163.23, 259.46, 98.30, 70.71, 43.06, 25.98, 17.11, 226.32, 508.03, 456.40, 962.35, 1417.05, 702.54, 82.94, 92.67},{104.73, 94.11, 85.17, 71.28, 340.96, 997.92, 1333.31, 1702.39, 3703.45, 2810.98, 456.97, 233.82, 132.85, 80.32, 102.05, 40.18, 441.13, 877.91, 877.44, 1275.19, 2413.15, 1549.45, 186.88, 175.70},{12.44, 10.64, 11.08, 3.03, 44.75, 76.82, 66.89, 63.98, 81.69, 68.82, 38.32, 26.00, 12.74, 6.56, 10.43, 9.05, 22.20, 70.44, 97.34, 36.08, 80.71, 68.76, 40.43, 33.86},{16.77, 12.70, 10.14, 11.19, 50.17, 89.80, 124.52, 115.74, 128.25, 98.44, 51.01, 28.90, 25.95, 8.23, 11.72, 6.06, 38.85, 87.09, 91.00, 63.75, 83.35, 89.68, 50.77, 34.43}},{{57.32, 52.26, 34.82, 41.21, 111.69, 501.29, 731.74, 634.25, 1674.69, 803.79, 129.08, 85.98, 72.59, 47.90, 37.77, 46.91, 177.58, 537.32, 406.04, 1040.29, 545.36, 576.66, 116.90, 45.27},{61.60, 60.48, 45.53, 54.43, 117.85, 502.85, 736.56, 664.24, 1570.75, 787.45, 137.38, 80.35, 72.32, 53.22, 42.85, 49.25, 182.13, 515.81, 324.09, 1151.71, 565.06, 640.14, 121.82, 50.89}, {156.42, 164.02, 87.58, 153.45, 398.01, 973.56, 1199.39, 1794.53, 2432.07, 1218.14, 227.58, 212.40, 185.08, 140.31, 74.73, 111.97, 290.61, 1145.15, 643.35, 1576.77, 1158.11, 1331.43, 292.64, 110.35}, {11.72, 7.63, 6.10, 6.82, 24.70, 83.14, 100.30, 97.09, 71.37, 82.09, 24.79, 23.55, 7.64, 0.19, 8.07, 8.18, 38.39, 87.17, 145.18, 99.03, 115.01, 76.75, 39.55, 11.27}, {16.19, 8.89, 13.83, 11.43, 46.43, 89.94, 41.11, 96.04, 114.65, 96.46, 37.18, 36.05, 18.31, 13.38, 13.18, 12.78, 24.22, 75.65, 85.28, 79.94, 116.13, 83.57, 57.77, 35.92}}, {{66.69, 30.48, 38.57, 95.64, 144.64, 573.61, 769.24, 1200.46, 2094.59, 1573.83, 253.24, 101.51, 94.82, 77.90, 50.09, 47.69, 408.99, 835.66, 1196.17, 609.87, 1812.33, 683.79, 199.59, 125.89}, {61.60, 29.76, 32.68, 111.46, 216.95, 585.79, 763.34, 1290.99, 2024.35, 1347.24, 228.10, 91.07, 72.32, 62.90, 64.28, 64.80, 487.47, 987.55, 1242.78, 650.85, 1775.52, 691.03, 209.95, 152.67}, {115.17, 70.16, 61.60, 142.56, 353.55, 1168.99, 1178.50, 2324.85, 4256.06, 2488.23, 396.58, 176.77, 131.24, 89.51, 91.07, 98.50, 891.91, 1936.22, 2006.12, 1074.04, 3403.30, 1740.96, 655.78, 216.95}, {17.34, 12.77, 2.52, 17.13, 27.53, 71.48, 44.51, 80.68, 124.45, 113.83, 28.00, 24.63, 19.14, 11.35, 2.22, 6.25, 31.77, 89.10, 135.06, 117.92, 118.47, 76.96, 24.63, 26.22}, {22.21, 14.97, 3.53, 24.66, 22.23, 70.02, 76.49, 118.17, 118.79, 79.47, 60.65, 27.73, 25.98, 14.61, 9.88, 16.90, 40.35, 95.05, 111.34, 95.45, 94.80, 88.32, 44.46, 33.83}}, {{35.36, 22.25, 34.82, 29.81, 56.25, 203.21, 716.47, 1083.95, 2154.21, 624.60, 191.29, 86.52, 61.87, 43.06, 26.52, 113.01, 230.35, 622.86, 1101.09, 1143.41, 1197.51, 719.96, 237.43, 171.68}, {43.93, 31.69, 44.19, 46.66, 74.19, 328.67, 754.24, 1235.01, 2125.18, 651.65, 179.37, 94.01, 60.80, 49.35, 33.21, 102.12, 272.13, 685.58, 1227.78, 1176.62, 981.07, 645.23, 236.65, 181.60}, {93.21, 64.83, 110.89, 154.22, 238.91, 709.69, 2043.89, 2019.78, 3271.62, 1605.97, 317.26, 158.56, 111.42, 100.64, 99.64, 225.24, 592.73, 1472.00, 1736.14, 1740.42, 1867.80, 1231.53, 537.84, 300.25}, {0.67, 7.49, 6.75, 4.65, 30.35, 72.31, 66.14, 94.97, 70.59, 68.17, 42.50, 28.41, 13.87, 8.49, 10.20, 16.38, 25.01, 79.56, 67.37, 24.50, 44.56, 172.50, 41.70, 11.83}, {4.28, 7.14, 9.67, 6.24, 36.52, 98.41, 127.31, 107.21, 122.64, 83.90, 29.11, 22.86, 9.75, 6.08, 13.49, 9.62, 33.31, 52.78, 75.42, 90.99, 64.95, 94.72, 58.07, 36.00}},{{68.3, 56.13, 49.02, 21.26, 93.74, 543.8, 554.7, 1053.15, 934.93, 1038.42, 107.3, 53.3, 47.94, 55.88, 25.98, 33.96, 105, 358.73, 414.35, 1158.14, 1085.01, 663.17, 117.16, 45.8},{60.80, 65.56, 49.28, 25.92, 103.39, 548.99, 570.50, 1045.11, 1065.31, 1037.08, 137.38, 51.69, 55.71, 55.64, 31.07, 46.92, 132.31, 369.10, 438.19, 1228.85, 1068.16, 711.92, 106.53, 33.21},{173.29, 187.97, 90.53, 92.28, 280.96, 956.71, 1037.08, 1720.07, 1909.79, 3675.57, 221.88, 120.80, 101.78, 97.25, 65.35, 96.16, 218.56, 602.90, 964.76, 2881.42, 1843.17, 1261.53, 238.20, 61.60},{19.04, 4.77, 6.85, 4.78, 28.94, 65.77, 54.70, 66.24, 115.86, 91.60, 43.16, 15.47, 11.20, 8.15, 0.36, 0.53, 32.75, 71.59, 62.34, 85.75, 123.06, 83.88, 46.97, 42.92},{24.09, 16.83, 14.46, 7.16, 46.87, 51.29, 23.08, 97.63, 73.63, 81.77, 39.34, 22.09, 26.31, 18.10, 14.89, 13.73, 25.00, 64.97, 59.79, 83.38, 82.82, 96.11, 51.06, 49.42}},{{40.44, 41.37, 51.96, 26.96, 118.12, 568.95, 632.64, 645.49, 1452.82, 841.29, 267.49, 86.24, 61.34, 42.58, 21.43, 14.00, 169.55, 434.42, 424.26, 748.88, 1295.48, 522.29, 89.94, 88.66},{43.39, 39.19, 47.68, 37.32, 147.58, 581.39, 758.26, 975.47, 1782.52, 1163.23, 259.46, 98.30, 70.71, 43.06, 25.98, 17.11, 226.32, 508.03, 456.40, 962.35, 1417.05, 702.54, 82.94, 92.67},{104.73, 94.11, 85.17, 71.28, 340.96, 997.92, 1333.31, 1702.39, 3703.45, 2810.98, 456.97, 233.82, 132.85, 80.32, 102.05, 40.18, 441.13, 877.91, 877.44, 1275.19, 2413.15, 1549.45, 186.88, 175.70},{12.44, 10.64, 11.08, 3.03, 44.75, 76.82, 66.89, 63.98, 81.69, 68.82, 38.32, 26.00, 12.74, 6.56, 10.43, 9.05, 22.20, 70.44, 97.34, 36.08, 80.71, 68.76, 40.43, 33.86},{16.77, 12.70, 10.14, 11.19, 50.17, 89.80, 124.52, 115.74, 128.25, 98.44, 51.01, 28.90, 25.95, 8.23, 11.72, 6.06, 38.85, 87.09, 91.00, 63.75, 83.35, 89.68, 50.77, 34.43}},{{57.32, 52.26, 34.82, 41.21, 111.69, 501.29, 731.74, 634.25, 1674.69, 803.79, 129.08, 85.98, 72.59, 47.90, 37.77, 46.91, 177.58, 537.32, 406.04, 1040.29, 545.36, 576.66, 116.90, 45.27},{61.60, 60.48, 45.53, 54.43, 117.85, 502.85, 736.56, 664.24, 1570.75, 787.45, 137.38, 80.35, 72.32, 53.22, 42.85, 49.25, 182.13, 515.81, 324.09, 1151.71, 565.06, 640.14, 121.82, 50.89}, {156.42, 164.02, 87.58, 153.45, 398.01, 973.56, 1199.39, 1794.53, 2432.07, 1218.14, 227.58, 212.40, 185.08, 140.31, 74.73, 111.97, 290.61, 1145.15, 643.35, 1576.77, 1158.11, 1331.43, 292.64, 110.35}, {11.72, 7.63, 6.10, 6.82, 24.70, 83.14, 100.30, 97.09, 71.37, 82.09, 24.79, 23.55, 7.64, 0.19, 8.07, 8.18, 38.39, 87.17, 145.18, 99.03, 115.01, 76.75, 39.55, 11.27}, {16.19, 8.89, 13.83, 11.43, 46.43, 89.94, 41.11, 96.04, 114.65, 96.46, 37.18, 36.05, 18.31, 13.38, 13.18, 12.78, 24.22, 75.65, 85.28, 79.94, 116.13, 83.57, 57.77, 35.92}}, {{66.69, 30.48, 38.57, 95.64, 144.64, 573.61, 769.24, 1200.46, 2094.59, 1573.83, 253.24, 101.51, 94.82, 77.90, 50.09, 47.69, 408.99, 835.66, 1196.17, 609.87, 1812.33, 683.79, 199.59, 125.89}, {61.60, 29.76, 32.68, 111.46, 216.95, 585.79, 763.34, 1290.99, 2024.35, 1347.24, 228.10, 91.07, 72.32, 62.90, 64.28, 64.80, 487.47, 987.55, 1242.78, 650.85, 1775.52, 691.03, 209.95, 152.67}, {115.17, 70.16, 61.60, 142.56, 353.55, 1168.99, 1178.50, 2324.85, 4256.06, 2488.23, 396.58, 176.77, 131.24, 89.51, 91.07, 98.50, 891.91, 1936.22, 2006.12, 1074.04, 3403.30, 1740.96, 655.78, 216.95}, {17.34, 12.77, 2.52, 17.13, 27.53, 71.48, 44.51, 80.68, 124.45, 113.83, 28.00, 24.63, 19.14, 11.35, 2.22, 6.25, 31.77, 89.10, 135.06, 117.92, 118.47, 76.96, 24.63, 26.22}, {22.21, 14.97, 3.53, 24.66, 22.23, 70.02, 76.49, 118.17, 118.79, 79.47, 60.65, 27.73, 25.98, 14.61, 9.88, 16.90, 40.35, 95.05, 111.34, 95.45, 94.80, 88.32, 44.46, 33.83}}, {{35.36, 22.25, 34.82, 29.81, 56.25, 203.21, 716.47, 1083.95, 2154.21, 624.60, 191.29, 86.52, 61.87, 43.06, 26.52, 113.01, 230.35, 622.86, 1101.09, 1143.41, 1197.51, 719.96, 237.43, 171.68}, {43.93, 31.69, 44.19, 46.66, 74.19, 328.67, 754.24, 1235.01, 2125.18, 651.65, 179.37, 94.01, 60.80, 49.35, 33.21, 102.12, 272.13, 685.58, 1227.78, 1176.62, 981.07, 645.23, 236.65, 181.60}, {93.21, 64.83, 110.89, 154.22, 238.91, 709.69, 2043.89, 2019.78, 3271.62, 1605.97, 317.26, 158.56, 111.42, 100.64, 99.64, 225.24, 592.73, 1472.00, 1736.14, 1740.42, 1867.80, 1231.53, 537.84, 300.25}, {0.67, 7.49, 6.75, 4.65, 30.35, 72.31, 66.14, 94.97, 70.59, 68.17, 42.50, 28.41, 13.87, 8.49, 10.20, 16.38, 25.01, 79.56, 67.37, 24.50, 44.56, 172.50, 41.70, 11.83}, {4.28, 7.14, 9.67, 6.24, 36.52, 98.41, 127.31, 107.21, 122.64, 83.90, 29.11, 22.86, 9.75, 6.08, 13.49, 9.62, 33.31, 52.78, 75.42, 90.99, 64.95, 94.72, 58.07, 36.00}}};
		// A[omega][i][t]
		//Scenarios 0-4 --> Low Demand, 5-9 --> Expected Demand, 10-14 --> High Demand
		//Scenarios 0, 5, 10 --> Low Inflow; 1, 6, 11 --> MidLow Inflow; 2, 7, 12 --> Mid Inflow; 3, 8, 13 --> MidHigh Inflow; 4, 9, 14 --> High Inflow
		double[] zeta = new double[OmegaMax]; // probability of scenario omega
		for(int omega:Omega){
			zeta[omega] = (double) 1/OmegaMax;
		}
 		try{
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
			
		//Decision Variables
		IloIntVar x_inv[][] = new IloIntVar[PMax][]; // investment decision of project p at stage t
		for(int p:P){
			x_inv[p] = cplex.intVarArray(TMax, 0, 1);
		}
		IloNumVar x_r[][] = new IloNumVar[TMax][]; // violation of electricity demand fulﬁllment at stage t in scenario omega
		IloNumVar x_o[][] = new IloNumVar[TMax][]; // emission overspending at stage t in scenario omega
		IloNumVar v[][][] = new IloNumVar[IMax][TMax+1][]; // hydro reservoir level of plant i at the beginning of stage t in scenario omega
		IloNumVar u[][][] = new IloNumVar[IMax][TMax][]; // hydro generation level in plant i at stage t due to generation in scenario omega
		IloNumVar s[][][] = new IloNumVar[IMax][TMax][]; // spillage level in plant i at stage t in scenario omega
		IloNumVar g[][][] = new IloNumVar[JMax][TMax][]; // thermal generation level in plant j at stage t due to generation in scenario omega
		IloNumVar e[][] = new IloNumVar[TMax][]; // emissions allowances left at the end of stage t in scenario omega
		for(int t:T){
			x_r[t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
			x_o[t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
			e[t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
		}
		for(int i:I){
			for(int t:T1){
				v[i][t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
			}
			for(int t:T){
				u[i][t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
				s[i][t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
			}
		}
		for(int j:J){
			for(int t:T){
				g[j][t] = cplex.numVarArray(OmegaMax, 0, Double.MAX_VALUE);
			}
		}
			
		//Constraints
		for(int p:P){
			IloLinearNumExpr c1 = cplex.linearNumExpr(); // A project can be realized at most once.
			for(int t:T_p[p]){
				c1.addTerm(1, x_inv[p][t]);
			}
			cplex.addLe(c1, 1);
		}
			
		for(int p:P_M){
			IloLinearNumExpr c2 = cplex.linearNumExpr(); // Mandatory projects must be realized.
			for(int t:T_p[p]){
				c2.addTerm(1, x_inv[p][t]);
			}
			cplex.addEq(c2, 1);
		}
			
		for(int p:P){
			IloLinearNumExpr c3 = cplex.linearNumExpr(); // Conﬂicting projects cannot be realized together.
			for(int q:P_C[p]){
				for(int t1:T_p[q]){
					c3.addTerm(1, x_inv[q][t1]);
				}
			}
			for(int t:T_p[p]){
				c3.addTerm(1, x_inv[p][t]);
			}
			cplex.addLe(c3, 1);
		}
			
		for(int p:P){
			for(int q:P_A[p]){
				IloLinearNumExpr c4 = cplex.linearNumExpr(); // Interdepentent projects need to be realized together.
				for(int t_p:T_p[p]){
					c4.addTerm(1, x_inv[p][t_p]);
				}
				for(int t_q:T_p[q]){
					c4.addTerm(-1, x_inv[q][t_q]);
				}
				cplex.addEq(c4, 0);
			}
		}
			
		for(int i:I){
			for(int t:T){
				for(int omega:Omega){
					IloLinearNumExpr c17 = cplex.linearNumExpr(); // Water balance in hydro reservoir.
					c17.addTerm(1, v[i][t+1][omega]);
					c17.addTerm(-1, v[i][t][omega]);
					c17.addTerm(1, u[i][t][omega]);
					c17.addTerm(1, s[i][t][omega]);
					for(int h:U_i[i]){
						c17.addTerm(-1, u[h][t][omega]);
						c17.addTerm(-1, s[h][t][omega]);
					}
					cplex.addEq(c17, A[omega][i][t]);
				}
			}
		}
			
		for(int i:I_ex){
			for(int t:T){
				for(int omega:Omega){
					cplex.addLe(u[i][t][omega], U_max[i]); // C18 Hydro generation.
					cplex.addGe(u[i][t][omega], U_min[i]); // C18 Hydro generation.
				}
			}
		}
			
		for(int t:T){
			for(int p:P){
				for(int i:I_p[p]){
					for(int omega:Omega){
						IloLinearNumExpr c19 = cplex.linearNumExpr(); //Hydro generation.
						for(int tau=0; tau<=t; tau++){
							c19.addTerm(1, x_inv[p][tau]);
						}
						cplex.addLe(cplex.prod(U_min[i], c19), u[i][t][omega]);
						cplex.addGe(cplex.prod(U_max[i], c19), u[i][t][omega]);
					}
				}
			}
		}
			
		for(int i:I_ex){
			for(int t:T1){
				for(int omega:Omega){
					cplex.addLe(v[i][t][omega], V_max[i]); // C20 Hydro reservoir levels.
					cplex.addGe(v[i][t][omega], V_min[i]); // C20 Hydro reservoir levels.
				}
			}
		}
			
		for(int t:T1){
			for(int p:P){
				for(int i:I_p[p]){
					for(int omega:Omega){
						IloLinearNumExpr c21 = cplex.linearNumExpr(); //Hydro reservoir levels.
						if(t != TMax){
							for(int tau=0; tau<=t; tau++){
								c21.addTerm(1, x_inv[p][tau]);
							}
						}
						else if(t == TMax){
							for(int tau=0; tau<t; tau++){
								c21.addTerm(1, x_inv[p][tau]);
							}
						}
						cplex.addLe(cplex.prod(V_min[i], c21), v[i][t][omega]);
						cplex.addGe(cplex.prod(V_max[i], c21), v[i][t][omega]);
					}
				}
			}
		}
			
		for(int j:J_ex){
			for(int t:T){
				for(int omega:Omega){
					cplex.addLe(g[j][t][omega], G_max[j]); // C22 Thermal generation.
					cplex.addGe(g[j][t][omega], G_min[j]); // C22 Thermal generation.
					}
			}
		}
			
		for(int t:T){
			for(int p:P){
				for(int j:J_p[p]){
					for(int omega:Omega){
						IloLinearNumExpr c23 = cplex.linearNumExpr(); // Thermal generation.
						for(int tau=0; tau<=t; tau++){
							c23.addTerm(1, x_inv[p][tau]);
						}
						cplex.addLe(cplex.prod(G_min[j], c23), g[j][t][omega]);
						cplex.addGe(cplex.prod(G_max[j], c23), g[j][t][omega]);
					}
				}
			}
		}	
			
		for(int t:T){
			if(T_E.contains(t)){
				for(int omega:Omega){
					IloLinearNumExpr c25 = cplex.linearNumExpr(); // Emission reservoir.
					c25.addTerm(1, e[t+1][omega]);
					for(int j:J){
						c25.addTerm(B[j], g[j][t][omega]);
					}
					c25.addTerm(-1, x_o[t][omega]);
					cplex.addEq(c25, E_allow[t]);
				}
			}
			else if(t != TMax-1){
				for(int omega:Omega){
					IloLinearNumExpr c24 = cplex.linearNumExpr(); // Emission reservoir.
					c24.addTerm(1, e[t+1][omega]);
					c24.addTerm(-1, e[t][omega]);
					for(int j:J){
						c24.addTerm(B[j], g[j][t][omega]);
					}
					c24.addTerm(-1, x_o[t][omega]);
					cplex.addEq(c24, 0);
				}
			}
			else{
				
			}
		}
			
		for(int t:T){
			for(int omega:Omega){
				IloLinearNumExpr c26 = cplex.linearNumExpr(); // Demand violation.
				c26.addTerm(1, x_r[t][omega]);
				for(int j:J){
					c26.addTerm(1, g[j][t][omega]);
				}
				for(int i:I){						
					c26.addTerm(rho[i], u[i][t][omega]);
				}
				cplex.addGe(c26, D[omega][t]);
			}
		}
		
		for(int p:P){
			for(int t:T){
				if(Project.notPossible(T_p[p], t)){
					cplex.addEq(x_inv[p][t], 0); // Investment only is only in some time periods possible
					//System.out.println("Project "+p+" can't be realized in period "+t);
				}
			}
		}
			
		//Decision Expressions
		IloLinearNumExpr z1 = cplex.linearNumExpr(); // Investment cost.
		for(int p:P){
			for(int t:T_p[p]){
				z1.addTerm(c_inv[p][t], x_inv[p][t]);
			}
		}
		IloLinearNumExpr z2 = cplex.linearNumExpr(); // Thermal generation cost.
		for(int j:J){
			for(int t:T){
				for(int omega:Omega){
					z2.addTerm(zeta[omega]*c_op[j][t], g[j][t][omega]);
				}
			}
		}
		IloLinearNumExpr z3 = cplex.linearNumExpr(); // Penalty cost for electricity rationing.
		for(int t:T){
			for(int omega:Omega){
				z3.addTerm(zeta[omega]*M_r[t], x_r[t][omega]);
			}
		}
		IloLinearNumExpr z4 = cplex.linearNumExpr(); // Cost for emission overspending.
		for(int t:T){
			for(int omega:Omega){
				z4.addTerm(zeta[omega]*M_o[t], x_o[t][omega]);
			}
		}
			
		//Objective Function
		IloNumExpr ZF = cplex.sum(z1, z2, z3, z4);
		cplex.addMinimize(ZF);
		if(cplex.solve()){
			result = cplex.getObjValue();
			System.out.println("Optimal objective value of the RP is: "+result+".");
			System.out.println("Z1 (RP) is: "+cplex.getValue(z1));
			System.out.println("Z2 (RP) is: "+cplex.getValue(z2));
			System.out.println("Z3 (RP) is: "+cplex.getValue(z3));
			System.out.println("Z4 (RP) is: "+cplex.getValue(z4));
			for(int p:P){
				for(int t:T_p[p]){
					if(cplex.getValue(x_inv[p][t]) == 1){
						System.out.println("Project "+p+" is realized at stage "+t);
					}	
				}
			}
		}
		else{
			System.out.println("CPLEX could not solve the problem.");
		}
		cplex.end();
	}
	catch (IloException exc) {
		exc.printStackTrace();
	}
	return result;
	}
}
