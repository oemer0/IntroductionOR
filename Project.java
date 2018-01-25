package solution;

public class Project {
	public static boolean notPossible(int[] a_array, int b){
		boolean result = true;
		for(int a:a_array){
			if(b==a){
				result = false;
			}
		}
		return result;
	}
}
