package org.sag.common.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.hash.HashCode;

public class MakeEmptyJar {
	
	private static final String jarHexString = 
			"504b03041400000808000000213800000000020000000000000009000400" + 
			"4d4554412d494e462ffeca00000300504b03040a000000000001ae264bb2" + 
			"7f02ee1900000019000000140000004d4554412d494e462f4d414e494645" + 
			"53542e4d464d616e69666573742d56657273696f6e3a20312e300d0a0d0a" + 
			"504b01021400140000080800000021380000000002000000000000000900" + 
			"040000000000000000000000000000004d4554412d494e462ffeca000050" + 
			"4b01023f000a000000000001ae264bb27f02ee1900000019000000140024" + 
			"00000000000000200000002d0000004d4554412d494e462f4d414e494645" + 
			"53542e4d460a0020000000000001001800463e17577b27d30195abf7747a" + 
			"27d30195abf7747a27d301504b05060000000002000200a1000000780000" + 
			"000000";
	
	private static byte[] jarHex = HashCode.fromString(jarHexString).asBytes();
	
	public static void writeEmptyJar(Path path) throws IOException {
		try(DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
			out.write(jarHex);
		}
	}

}
