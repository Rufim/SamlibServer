package ru.samlib.server.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;


public class CryptUtil {

    private static final String TAG = CryptUtil.class.getSimpleName();

    public static final Integer DECRYPT_PORTION = 1600;
    public static final Integer ENCRYPT_PORTION = 1616;
    public static final Integer TAIL_LIMIT = 1599;          //DECRYPT_PORTION-1
    public static final Integer GENERAL_PORTION = 28800;
    public static final Integer ENCRYPTED_BLOCK_PORTION = ENCRYPT_PORTION + GENERAL_PORTION;
    public static final Integer DECRYPTED_BLOCK_PORTION = DECRYPT_PORTION + GENERAL_PORTION;
    public static final Boolean MODE_ENCRYPT = true;
    public static final Boolean MODE_DECRYPT = false;
    public static final Integer MAX_INT_VALUE_LIMIT = 256;

    // Config
    public static Integer RAW_KEY_SIZE = 16;
    public static String defaultKey = "97b0b5dcd405c82d128d22ae3e9fba77";
    public static byte[] cachedKey = new byte[RAW_KEY_SIZE];
    public static boolean isKeysEncrypted = false;

    /**
     * Made rawKey from seed. Be careful - this method is platform depended
     *
     * @param seed String seed
     * @return the byte array
     * @throws Exception
     */
    public static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

    /**
     * Generate rawKey as byte array of RAW_KEY_SIZE size
     *
     * @return rawKey
     */
    public static String generateKey() throws Exception {
        List<Integer> seed = new ArrayList<Integer>(MAX_INT_VALUE_LIMIT);
        byte[] key = new byte[RAW_KEY_SIZE];
        Random random = new Random();

        // make list of non repeated elements which can be converted to byte
        for (Integer i = 0; i < MAX_INT_VALUE_LIMIT; i++) {
            seed.add(i);
        }

        // randomly non-repeated choose element from non-repeated list;
        // RAW_KEY_SIZE cannot be bigger than MAX_INT_VALUE_LIMIT
        Integer seedSize = seed.size();
        for (int i = 0; i < RAW_KEY_SIZE; i++) {
            int index = random.nextInt(seedSize - i);
            key[i] = ((Integer) seed.toArray()[index]).byteValue();
            seed.remove(index);
        }
        if (isKeysEncrypted) {
            return encodeKey(getHexStringKey(key));
        }
        return getHexStringKey(key);
    }

    /**
     * Method generates special HexString from key or use own mechanism to make key
     *
     * @param key rawKey that encode to Hex String, if not - method generate with own key
     * @return Special encoded Hex String
     */
    public static String getHexStringKey(byte[] key) {
        String result = "";
        for (int i = 0; i < RAW_KEY_SIZE; i++) {
            String hexInt = Integer.toHexString(key[i] + 128);
            if (hexInt.length() < 2) {
                hexInt = "0" + hexInt;
            }
            result = result + hexInt;
        }
        if (result.length() != RAW_KEY_SIZE * 2) {
            result = null;
        }
        return result;
    }

    /**
     * Method generates special HexString from key or use own mechanism to make key
     *
     * @param key rawKey that encode to Hex String, if not - method generate with own key
     * @return Special encoded Hex String
     */
    public static String getHexStringCustomKey(byte[] key) {
        String result = "";
        for (int i = 0; i < key.length; i++) {
            String hexInt = Integer.toHexString(key[i] + 128);
            if (hexInt.length() < 2) {
                hexInt = "0" + hexInt;
            }
            result = result + hexInt;
        }
        return result;
    }


    // hex string 00-FF to byte (-128) - 127

    /**
     * Method return byte[] array from special hex string
     *
     * @param hexString String with size RAW_KEY_SIZE*2
     * @return byte[] array of rawKey
     */
    public static byte[] getByteFromHexStringKey(String hexString) {
        if (hexString == null) return null;
        if (hexString.length() != RAW_KEY_SIZE * 2) {
            //todo set normal check
            return null;
        }
        String workString = "";
        byte[] backByte = new byte[RAW_KEY_SIZE];
        for (int i = 0; i < RAW_KEY_SIZE; i++) {
            try {
                workString = hexString.substring(i * 2, i * 2 + 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //todo add try catch
            Integer back = Integer.valueOf(workString, 16);
            backByte[i] = ((Integer) (back - 128)).byteValue();
        }
        return backByte;
    }

    public static byte[] getByteFromHexStringKeyWithCatch(String hexString) {
        if (hexString == null) return null;
        if (hexString.length() != RAW_KEY_SIZE * 2) {
            return null;
        }
        int len = hexString.length();
        for (int i = 0; i < len; i += 2) {
            cachedKey[i / 2] = (byte) (((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16)) - 128);
        }
        return cachedKey;
    }

    public static byte[] getByteFromHexCustomStringKey(String hexString) {
        if (hexString == null || hexString.length() <= 0 || hexString.length() % 2 != 0) return null;
        int len = hexString.length();
        int rawSize = len / 2;
        byte[] key = new byte[rawSize];
        for (int i = 0; i < len; i += 2) {
            key[i / 2] = (byte) (((Character.digit(hexString.charAt(i), rawSize) << 4)
                    + Character.digit(hexString.charAt(i + 1), rawSize)) - 128);
        }
        return key;
    }


    public static String getKeyFromKeyFile(File currentDirectory, File file) throws Exception {
        if (file == null) {
            return null;
        }
        File keyFile = new File(currentDirectory, "key.txt");
        String key = null;
        if (keyFile.canRead()) {
            BufferedReader reader = new BufferedReader(new FileReader(keyFile));
            String line = reader.readLine();
            while (line != null) {
                int index = file.getName().lastIndexOf(".enc");
                if (index == -1 && line.substring(0, line.lastIndexOf(" - ")).equals(file.getName())) {
                    key = line.substring(line.indexOf(" - ") + 3, line.length());
                    break;
                } else if (line.substring(0, line.lastIndexOf(" - ")).equals(file.getName().substring(0, index))) {
                    key = line.substring(line.indexOf(" - ") + 3, line.length());
                    break;
                }
                line = reader.readLine();
            }
            reader.close();
            return key;
        } else {
            throw new Exception("Key file is not readable");
        }
    }

    public static void deleteKeyFromKeyFile(File currentDirectory, File file) throws Exception {
        if (file == null) {
            return;
        }
        File keyFile = new File(currentDirectory, "key.txt");
        File newKeyFile = new File(currentDirectory, "newKey.txt");
        if (keyFile.canRead()) {
            BufferedReader reader = new BufferedReader(new FileReader(keyFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(keyFile));
            String line = reader.readLine();
            while (line != null) {
                if (!line.substring(0, line.lastIndexOf(" - ")).equals(file.getName())) {
                    writer.append(line);
                }
                line = reader.readLine();
            }
            keyFile.delete();
            newKeyFile.renameTo(keyFile);
            reader.close();
            writer.close();
        }
    }

    public static void addKeyToKeyFile(File currentDirectory, File file, String key) throws Exception {
        if (file == null) {
            return;
        }
        deleteKeyFromKeyFile(currentDirectory, file);
        File keyFile = new File(currentDirectory, "key.txt");
        if (keyFile.canWrite()) {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(keyFile));
            writer.append(file.getName() + " - " + key + "\n");
            writer.close();
        } else {
            throw new Exception("Key file is not writable");
        }
    }

    public static File aesFile(File inFile, Object key, boolean mode, boolean full) throws Exception {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        File outFile = null;
        try {
            inputStream = new FileInputStream(inFile);
            if (mode == MODE_ENCRYPT) {
                outFile = new File(inFile.getParent(), inFile.getName() + ".enc");
            } else {
                int index = inFile.getName().lastIndexOf(".enc");
                if (index == -1) {
                    File decrypted = new File(inFile.getParentFile(), "decrypted");
                    decrypted.mkdirs();
                    outFile = new File(decrypted, inFile.getName());
                    //throw new Exception("File wrong format - " + inFile.getName());
                } else {
                    outFile = new File(inFile.getParent(), inFile.getName().substring(0, index));
                }
            }
            outFile.delete();
            outFile.createNewFile();
            outputStream = new FileOutputStream(outFile);
            if (inFile.canRead() && outFile.canWrite()) {
                aesStream(inputStream, outputStream, key, mode, full);
            }
        } catch (Exception ex) {
            if (outFile != null) {
                outFile.delete();
            }
            throw ex;
            //skip
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ex) {
                //ignored
            }
        }
        return outFile;
    }

    private static byte[] getCryptoStringKey() {
        //  byte[] key = new byte[16];
        try {
            return ("d12Vp54R4sb0ymVF").getBytes("UTF-8");
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    public static String encodeKey(String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(getCryptoStringKey(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] keyBytes = key.getBytes("UTF-8");
        return  Base64.getEncoder().encodeToString(cipher.doFinal(keyBytes));
    }

    public static String decodeKey(String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(getCryptoStringKey(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return new String(cipher.doFinal(keyBytes), "UTF-8");
    }

    public static int aesStream(InputStream input, OutputStream output, Object key, boolean mode, boolean full) throws Exception {
        byte[] inputBlock;
        byte[] cryptedBlock;
        byte[] rawBlock;
        byte[] outputBlock;
        byte[] buffer = null;
        Integer blockPortion;
        Integer portion;
        byte[] byteKey = null;
        if (key instanceof byte[]) {
            if (isKeysEncrypted) {
                //TODO: Check if it is works
                byteKey = getByteFromHexCustomStringKey(decodeKey(getHexStringKey((byte[]) key)));
            } else {
                byteKey = (byte[]) key;
            }
        } else if (key instanceof String) {
            if (isKeysEncrypted) {
                byteKey = getByteFromHexCustomStringKey(decodeKey((String) key));
            } else {
                byteKey = getByteFromHexCustomStringKey((String) key);
            }
        } else {
            throw new Exception("Wrong key class object");
        }
        SecretKeySpec sKeySpec = new SecretKeySpec(byteKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        if (mode == MODE_ENCRYPT) {
            inputBlock = new byte[DECRYPTED_BLOCK_PORTION];
            blockPortion = DECRYPTED_BLOCK_PORTION;
            portion = DECRYPT_PORTION;
            cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
        } else {
            inputBlock = new byte[ENCRYPTED_BLOCK_PORTION];
            blockPortion = ENCRYPTED_BLOCK_PORTION;
            portion = ENCRYPT_PORTION;
            cipher.init(Cipher.DECRYPT_MODE, sKeySpec);
        }
        if (full) {
            int length = 0;
            int read;
            while (true) {
                read = input.read(inputBlock);
                if (read == -1) {
                    break;
                }
                output.write(cipher.doFinal(inputBlock, 0, blockPortion > read ? read : blockPortion));
                length += mode ? ENCRYPTED_BLOCK_PORTION : DECRYPTED_BLOCK_PORTION;
            }
            return length;
        } else {
            int length = 0;
            int read;
            while (true) {
                read = input.read(inputBlock);
                if (read == -1) {
                    if (buffer != null) {
                        length += buffer.length;
                        output.write(buffer);
                    }
                    break;
                }

                if (read < blockPortion) {
                    if (buffer == null) {
                        buffer = Arrays.copyOf(inputBlock, read);
                        continue;
                    } else {
                        buffer = concat(buffer, Arrays.copyOf(inputBlock, read));
                        if (buffer.length < blockPortion) {
                            continue;
                        } else {
                            inputBlock = buffer;
                            buffer = null;
                        }
                    }
                }

                cryptedBlock = cipher.doFinal(inputBlock, 0, portion);
                rawBlock = Arrays.copyOfRange(inputBlock, portion, blockPortion);
                outputBlock = concat(cryptedBlock, rawBlock);
                output.write(outputBlock);
                length += mode ? ENCRYPTED_BLOCK_PORTION : DECRYPTED_BLOCK_PORTION;

                if (inputBlock.length > blockPortion) {
                    buffer = Arrays.copyOfRange(inputBlock, blockPortion, inputBlock.length);
                }
            }
            return length;
        }
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


}
