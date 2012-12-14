package ch.admin.vbs.cube.tmp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.crypto.Base64;

public class KeyASCIIOutput {
	private static final int LINEBREAK = 64;
	private static final Logger LOG = LoggerFactory.getLogger(KeyASCIIOutput.class);
	private static int CRC24_INIT = 0xb704ce;
	private static int CRC24_POLY = 0x1864cfb;

	public void write(OutputStream os, PublicKey pubkey) {
		try {
			byte[] pkeyb64 = Base64.encodeBytes(pubkey.getEncoded()).getBytes("ASCII");
			//
			os.write("-----BEGIN PGP PUBLIC KEY BLOCK-----\nVersion: Cube v2.0\n\n".getBytes("ASCII"));
			//
			int x = 0;
			while (x < pkeyb64.length) {
				os.write(pkeyb64, x, Math.min(LINEBREAK, pkeyb64.length - x));
				// next line
				x += LINEBREAK;
				// CRC
				if (x >= pkeyb64.length) {
					os.write('=');
					os.write(Base64.encodeBytes(crc_octets(pubkey.getEncoded())).getBytes("ASCII"));
				}
				os.write('\n');
			}
			//
			os.write("-----END PGP PUBLIC KEY BLOCK-----\n".getBytes("ASCII"));
		} catch (UnsupportedEncodingException e) {
			LOG.error("Failed write key", e);
			System.exit(1);
		} catch (IOException e) {
			throw new RuntimeException("Failed write key", e);
		}
	}

	public byte[] crc_octets(byte[] data) {
		int crc = CRC24_INIT;
		for (int j = 0; j < data.length; j++) {
			crc ^= (data[j]) << 16;
			for (int i = 0; i < 8; i++) {
				crc <<= 1;
				if ((crc & 0x1000000) != 0)
					crc ^= CRC24_POLY;
			}
		}
		System.out.printf("%h\n",crc);
		return new byte[] { (byte) ((crc & 0xff0000) >> 16), (byte) ((crc & 0x00ff00) >> 8), (byte) (crc & 0x0000ff) };
	}
}
