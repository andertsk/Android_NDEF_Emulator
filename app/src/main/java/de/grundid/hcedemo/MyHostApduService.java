package de.grundid.hcedemo;

import android.nfc.NdefMessage;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import org.ndeftools.AbsoluteUriRecord;
import org.ndeftools.Message;
import org.ndeftools.MimeRecord;
import org.ndeftools.wellknown.TextRecord;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;

public class MyHostApduService extends HostApduService {
	enum State {
		READING_CC_FILE,
		READING_NLEN,
		READING_NDEF_FILE,
		NONE
	};

	private static final String TAG = "CardService";
	// AID for our loyalty card service.
	private static final String TYPE4_TAG_AID = "D2760000850101";
	// ISO-DEP command HEADER for selecting an AID.
	// Format: [Class | Instruction | Parameter 1 | Parameter 2]
	private static final String SELECT_APDU_HEADER = "00A40400";
	// "OK" status word sent in response to SELECT AID command (0x9000)
	private static final byte[] SELECT_OK_SW = hexStringToByteArray("9000");
	private static final byte[] SELECT_APDU = BuildSelectApdu(TYPE4_TAG_AID);
	private static final byte[] SELECT_CC = hexStringToByteArray("00a4000c02e103");//Select Capability Container command
	private static final byte[] CC_FILE = hexStringToByteArray("000F" + "20" + "FFFF" + "FFFF" + "0406E10400FF0000");//Capability Container File (file id = E104)
	private static final byte[] SELECT_NDEF_FILE = hexStringToByteArray("00a4000c02e104");
	private static final byte[] NDEF_MESSAGE_BYTES = getNDEFMessageBytes();
	private State state = State.NONE;
    private byte[] nlenBytes = new byte[2];

	@Override
	public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
		if (Arrays.equals(SELECT_APDU, commandApdu)) {

			Log.i(TAG, "Application selected");
			sendResponseApdu(SELECT_OK_SW);

		} else if (Arrays.equals(SELECT_CC, commandApdu)) {

			Log.i(TAG, "Capability Container selected");
			sendResponseApdu(SELECT_OK_SW);
			state = State.READING_CC_FILE;

		} else if (isReadBinary(commandApdu) && state == State.READING_CC_FILE) {

			Log.i(TAG, "Rading Capability Container");
			sendResponseApdu(concatByteArrays(CC_FILE, SELECT_OK_SW));
			state = State.READING_NLEN;

		} else if (isReadBinary(commandApdu) && state == State.READING_NDEF_FILE) {

			Log.i(TAG, "Reading NDEF File");
            nlenBytes[1] = (byte)NDEF_MESSAGE_BYTES.length;
			byte[] response = concatByteArrays(nlenBytes, NDEF_MESSAGE_BYTES);
            response = concatByteArrays(response, SELECT_OK_SW);
			sendResponseApdu(response);

		} else if (Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {

			Log.i(TAG, "NDEF File Selected");
			sendResponseApdu(SELECT_OK_SW);

		} else if (isReadBinary(commandApdu) && state == State.READING_NLEN) {

			Log.i(TAG, "Reading NLEN");

			if(NDEF_MESSAGE_BYTES.length > 255) {
				Log.e(TAG, "Too long NDEF Message, must be less than 255 bytes.");
				sendResponseApdu(hexStringToByteArray("0000"));
			} else {
				nlenBytes[0] = 0x00;
				nlenBytes[1] = (byte)(NDEF_MESSAGE_BYTES.length + 2);
                byte[] response = concatByteArrays(nlenBytes, SELECT_OK_SW);
				sendResponseApdu(response);
				state = State.READING_NDEF_FILE;
			}
		}
		else {

			Log.i(TAG, "Received: " + byteArrayToHexString(commandApdu));

		}
		return null;
	}

	private boolean isReadBinary(byte[] command) {
		return 	command.length == 5 &&
				command[0] == (byte)0x00 &&
				command[1] == (byte)0xb0 &&
				command[2] == (byte)0x00 &&
				command[3] == (byte)0x00;
	}
	public static byte[] BuildSelectApdu(String aid) {
		// Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
		return hexStringToByteArray(SELECT_APDU_HEADER +
				String.format("%02X", aid.length() / 2) + aid + "00");
	}

	@Override
	public void onDeactivated(int reason) {
		Log.i(TAG, "Deactivated: " + reason);
	}

	private static String byteArrayToHexString(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	public static byte[] getNDEFMessageBytes() {
		//AbsoluteUriRecord uriRecord1 = new AbsoluteUriRecord("http://verysoft.ru");
        //AbsoluteUriRecord uriRecord2 = new AbsoluteUriRecord("http://google.com");
		TextRecord textRecord = new TextRecord("Hello WOrld!");
        //TextRecord textRecord = new TextRecord("Hello world!");

		Message highLevelNDEFmessage = new Message();
		//highLevelNDEFmessage.add(uriRecord1);
		//highLevelNDEFmessage.add(uriRecord2);
		highLevelNDEFmessage.add(textRecord);

		NdefMessage lowLevelNDEFMessage = highLevelNDEFmessage.getNdefMessage();
		return  lowLevelNDEFMessage.toByteArray();
	}

	private byte[] concatByteArrays(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}