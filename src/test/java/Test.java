import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;



/**
 *@author Kevin.yang
 *@since 2015年5月17日
 */
public class Test {

	public static void main(String[] args) throws Exception {
		String[] a = "M/d/yy,y".split("\\s?,\\s?");
		BigDecimal c = NumberUtils.createBigDecimal("19,448.00");
		System.out.println(c);
		
		
	}
	

}

class A {
	private List<String> l;

	public List<String> getL() {
		return l;
	}

	public void setL(List<String> l) {
		this.l = l;
	}
	
	
}
