package bf.cloud.vr;


public class Points {

	public static float pp[] = null;
	public static float tt[] = null;
	public static float ps[] = null;
	public static short index[] = null;
	public Points(){
	}
	
	public static float[] getXYZ(){
		if(pp == null){
			fill();
		}
		return pp;
	}
	
	public static float[] getUV(){
		if(tt == null){
			fill();
		}
		return tt;
	}
	
	private static void fill(){
		pp = new float[ps.length / 5 * 3];
		tt = new float[ps.length / 5 * 2];
		for(int i = 0, ii = ps.length; i < ii; i += 5){
			pp[i / 5 * 3] = ps[i];
			pp[i / 5 * 3 + 1] = ps[i + 1];
			pp[i / 5 * 3 + 2] = ps[i + 2];
			tt[i / 5 * 2] = ps[i + 3];
			tt[i / 5 * 2 + 1] = ps[i + 4];
		}
	}
}
