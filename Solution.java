package solution;

public class Solution {
	
	public int[][] opening = new int[5][24];
		public Solution(int[][] a){
			for(int i=0; i<a.length; i++){
				for(int j=0; j<a[i].length; j++){
				this.opening[i][j]=a[i][j];
				}
			}
		}
		public Solution(){
			for(int i=0; i<5; i++){
				for(int j=0; j<24; j++){
				this.opening[i][j]=0;
				}
			}
		}
}
