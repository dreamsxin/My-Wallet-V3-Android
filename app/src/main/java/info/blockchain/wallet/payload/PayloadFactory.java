package info.blockchain.wallet.payload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.bitcoin.crypto.MnemonicException;

import libsrc.org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.hd.HD_Account;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.WebUtil;

import piuk.blockchain.android.R;

//import android.util.Log;

/**
 *
 * PayloadFactory.java : singleton class for reading/writing/parsing Blockchain HD JSON payload
 *
 */
public class PayloadFactory	{
	
	private static Context context = null;

    private static PayloadFactory instance = null;
    // active payload:
    private static Payload payload = null;
    // cached payload, compare to this payload to determine if changes have been made. Used to avoid needless remote saves to server
    private static String cached_payload = null;

    private static CharSequenceX strTempPassword =  null;
    private static CharSequenceX strTempDoubleEncryptPassword =  null;
    private static String strCheckSum = null;
    private static boolean isNew = false;
    private static boolean syncPubKeys = true;
	private static String email = null;

    private PayloadFactory()	{ ; }

    /**
     * Return instance for a payload factory.
     *
     * @return HD_WalletFactory
     *
     */
    public static PayloadFactory getInstance() {

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload();
            cached_payload = "";
        }

        return instance;
    }

    /**
     * Return instance for a payload factory. Provide Android context if needed.
     *
     * @param  Context ctx app context
     *
     * @return HD_WalletFactory
     *
     */
    public static PayloadFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload();
            cached_payload = "";
        }

        return instance;
    }

    /**
     * Return instance for a payload factory. Payload initialized using provided JSON string.
     *
     * @param  String json JSON string used to initialize this instance
     *
     * @return HD_WalletFactory
     *
     */
    public static PayloadFactory getInstance(String json) {

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload(json);
            try {
                cached_payload = payload.dumpJSON().toString();
            }
            catch(JSONException je) {
                cached_payload = "";
            }
        }

        return instance;
    }

    /**
     * Reset PayloadFactory to null instance.
     *
     */
    public void wipe() {
        instance = null;
    }

    /**
     * Get temporary password for user. Read password from here rather than reprompting user.
     *
     * @return CharSequenceX
     *
     */
    public CharSequenceX getTempPassword() {
        return strTempPassword;
    }

    /**
     * Set temporary password for user once it has been validated. Read password from here rather than reprompting user.
     *
     * @param CharSequenceX password Validated user password
     *
     */
    public void setTempPassword(CharSequenceX temp_password) {
        this.strTempPassword = temp_password;
    }

    /**
     * Get temporary double encrypt password for user. Read double encrypt password from here rather than reprompting user.
     *
     * @return CharSequenceX
     *
     */
    public CharSequenceX getTempDoubleEncryptPassword() {
        return strTempDoubleEncryptPassword;
    }

    /**
     * Set temporary double encrypt password for user once it has been validated. Read double encrypt password from here rather than reprompting user.
     *
     * @param CharSequenceX password Validated user double encrypt password
     *
     */
    public void setTempDoubleEncryptPassword(CharSequenceX temp_password2) {
        this.strTempDoubleEncryptPassword = temp_password2;
    }

    /**
     * Get checksum for this payload.
     *
     * @return String
     *
     */
    public String getCheckSum() {
        return strCheckSum;
    }

    /**
     * Set checksum for this payload.
     *
     * @param String checksum Checksum to be set for this payload
     *
     */
    public void setCheckSum(String checksum) {
        this.strCheckSum = checksum;
    }

    /**
     * Check if this payload is for a new Blockchain account.
     *
     * @return boolean
     *
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set if this payload is for a new Blockchain account.
     *
     * @param boolean isNew
     *
     */
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Remote get(). Get refreshed payload from server.
     *
     * @param  String guid User's wallet 'guid'
     * @param  String sharedKey User's sharedKey value
     * @param  CharSequenceX password User password
     *
     * @return Payload
     *
     */
    public Payload get(String guid, String sharedKey, CharSequenceX password) {

        try {
            String response = WebUtil.getInstance().postURL(WebUtil.PAYLOAD_URL,"method=wallet.aes.json&guid=" + guid + "&sharedKey=" + sharedKey + "&format=json");
            JSONObject jsonObject = new JSONObject(response);
            int iterations = AESUtil.PasswordPBKDF2Iterations;
            double version = 2.0;
            if(jsonObject.has("payload")) {
                String encrypted_payload = null;
                JSONObject _jsonObject = null;
                try {
                    _jsonObject = new JSONObject((String)jsonObject.get("payload"));
                }
                catch(Exception e) {
                    _jsonObject = null;
//                    Log.i("PayloadFactory", "_jsonObject is null");
                }
                if(_jsonObject != null && _jsonObject.has("payload")) {
                    if(_jsonObject.has("pbkdf2_iterations")) {
                        iterations = Integer.valueOf(_jsonObject.get("pbkdf2_iterations").toString());
                    }
                    if(_jsonObject.has("version")) {
                        version = Double.valueOf(_jsonObject.get("version").toString());
                    }
                    encrypted_payload = (String)_jsonObject.get("payload");
                }
                else {
                    if(jsonObject.has("pbkdf2_iterations")) {
                        iterations = Integer.valueOf(jsonObject.get("pbkdf2_iterations").toString());
                    }
                    if(jsonObject.has("version")) {
                        version = Double.valueOf(jsonObject.get("version").toString());
                    }
                    encrypted_payload = (String)jsonObject.get("payload");
                }

                String decrypted = null;
                try {
                    decrypted = AESUtil.decrypt(encrypted_payload, password, iterations);
//                    Log.i("PayloadFactory", decrypted);
                }
                catch(Exception e) {
                	payload = null;
                	e.printStackTrace();
                	return null;
                }
                if(decrypted == null) {
                	payload = null;
                	return null;
                }
                payload = new Payload(decrypted);
                if(payload.getJSON() == null) {
                	payload = null;
                	return null;
                }

                try {
                    payload.parseJSON();
                }
                catch(JSONException je) {
                	payload = null;
                	je.printStackTrace();
                    return null;
                }

                payload.setIterations(iterations);
            }
            else {
//                Log.i("PayloadFactory", "jsonObject has no payload");
                return null;
            }
        }
        catch(JSONException je) {
        	payload = null;
        	je.printStackTrace();
        	return null;
        }
        catch(Exception e) {
        	payload = null;
            e.printStackTrace();
        	return null;
        }

        return payload;
    }

    /**
     * Local get(). Returns current payload from the client.
     *
     * @return Payload
     *
     */
    public Payload get() {
        return payload;
    }

    /**
     * Local set(). Sets current payload on the client.
     *
     * @param p Payload to be assigned
     *
     */
    public void set(Payload p) {
        payload = p;
    }

    /**
     * Remote save of current client payload to server. Will not save if no change as compared to cached payload.
     *
     * @param CharSequenceX password User password
     *
     * @return boolean
     *
     */
    public boolean put(CharSequenceX password) {

		String strOldCheckSum = strCheckSum;
		String payloadCleartext = null;

		StringBuilder args = new StringBuilder();
		try	{

	    	if(cached_payload != null && cached_payload.equals(payload.dumpJSON().toString())) {
	    		return true;
	    	}

	    	payloadCleartext = payload.dumpJSON().toString();
	    	String payloadEncrypted = AESUtil.encrypt(payloadCleartext, new CharSequenceX(strTempPassword), AESUtil.PasswordPBKDF2Iterations);
	    	JSONObject rootObj = new JSONObject();
			rootObj.put("version", 2.0);
			rootObj.put("pbkdf2_iterations", AESUtil.PasswordPBKDF2Iterations);
			rootObj.put("payload", payloadEncrypted);
//			rootObj.put("guid", payload.getGuid());
//			rootObj.put("sharedKey", payload.getSharedKey());
//			rootObj.put("test", "OK");

			strCheckSum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(rootObj.toString().getBytes("UTF-8"))));

			String method = isNew ? "insert" : "update";

			String urlEncodedPayload = URLEncoder.encode(rootObj.toString());

			args.append("guid=");
			args.append(URLEncoder.encode(payload.getGuid(), "utf-8"));
			args.append("&sharedKey=");
			args.append(URLEncoder.encode(payload.getSharedKey(), "utf-8"));
			args.append("&payload=");
			args.append(urlEncodedPayload);
			args.append("&method=");
			args.append(method);
			args.append("&length=");
			args.append(rootObj.toString().length());
			args.append("&checksum=");
			args.append(URLEncoder.encode(strCheckSum, "utf-8"));

		}
		catch(NoSuchAlgorithmException | UnsupportedEncodingException | JSONException e)	{
			e.printStackTrace();
            return false;
		}

		if (syncPubKeys) {
			args.append("&active=");
			
			String[] legacyAddrs = null;
			List<LegacyAddress> legacyAddresses = payload.getLegacyAddresses();
			List<String> addrs = new ArrayList<String>();
			for(LegacyAddress addr : legacyAddresses) {
				if(addr.getTag() == 0L) {
					addrs.add(addr.getAddress());
				}
			}

			args.append(StringUtils.join(addrs.toArray(new String[addrs.size()]), "|"));
		}

		if (email != null && email.length() > 0) {
			args.append("&email=");
			try {
				args.append(URLEncoder.encode(email, "utf-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		args.append("&device=");
		args.append("android");

		if(strOldCheckSum != null && strOldCheckSum.length() > 0)	{
			args.append("&old_checksum=");
			args.append(strOldCheckSum);
		}
		
		try	{
			String response = WebUtil.getInstance().postURL(WebUtil.PAYLOAD_URL, args.toString());
			isNew = false;
            if(response.contains("Wallet successfully synced")){
                cache();
                return true;
            }
		}
		catch(Exception e)	{
            e.printStackTrace();
            return false;
		}

		return true;
    }

    /**
     * Write to current client payload to cache.
     *
     */
    public void cache() {
        try {
        	cached_payload = payload.dumpJSON().toString();
        }
        catch(JSONException je) {
        	je.printStackTrace();
        }
    }

    /**
     * Create a Blockchain wallet and include the HD_Wallet passed as an argument and write it to this instance's payload.
     *
     * @param HD_Wallet hdw HD wallet to include in the payload
     *
     * @return boolean
     *
     */
    public boolean createBlockchainWallet(HD_Wallet hdw)	{
    	
    	String guid = UUID.randomUUID().toString();
    	String sharedKey = UUID.randomUUID().toString();
    	
    	payload = new Payload();
    	payload.setGuid(guid);
    	payload.setSharedKey(sharedKey);
    	
    	PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, guid);
    	PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, sharedKey);
    	
    	HDWallet payloadHDWallet = new HDWallet();
    	payloadHDWallet.setSeedHex(hdw.getSeedHex());

    	List<HD_Account> hdAccounts = hdw.getAccounts();
    	List<Account> payloadAccounts = new ArrayList<Account>();
    	for(int i = 0; i < hdAccounts.size(); i++)	{
    		Account account = new Account();
        	try  {
            	String xpub = HD_WalletFactory.getInstance(context).get().getAccounts().get(i).xpubstr();
            	account.setXpub(xpub);
            	String xpriv = HD_WalletFactory.getInstance(context).get().getAccounts().get(i).xprvstr();
            	account.setXpriv(xpriv);
        	}
        	catch(IOException | MnemonicException.MnemonicLengthException e)  {
        		e.printStackTrace();
        	}

    		payloadAccounts.add(account);
    	}
    	payloadHDWallet.setAccounts(payloadAccounts);
    	
    	payload.setHdWallets(payloadHDWallet);
    	
    	isNew = true;

    	return true;
    }

    /**
     * Thread for rempte save of payload to server.
     *
     */
    public void remoteSaveThread() {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				if(PayloadFactory.getInstance(context).get() != null)	{

                    if(PayloadFactory.getInstance(context).put(strTempPassword))	{
//                        ToastCustom.makeText(context, "Remote save OK", ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                        ;
                    }
                    else	{
                        ToastCustom.makeText(context, context.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }

				}
				else	{
                    ToastCustom.makeText(context, context.getString(R.string.payload_corrupted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
				}

				handler.post(new Runnable() {
					@Override
					public void run() {
						;
					}
				});
				
				Looper.loop();

			}
		}).start();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		PayloadFactory.email = email;
	}
}