package fr.xephi.authme.security.crypts;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 implements EncryptionMethod {

	@Override
	public String getHash(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        sha1.update(password.getBytes());
        byte[] digest = sha1.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1,digest));
	}

	@Override
	public boolean comparePassword(String hash, String password, String playerName)
			throws NoSuchAlgorithmException {
		return hash.equals(getHash(password, ""));
	}

}
