package net.nadisa.anan;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

public class AppPack {
	
	public static void main(String[] args) throws ISOException {
        // Initialize packager. in this example, I'm using
        // XML packager. We also can use Java Code Packager
        // This code throws ISOException
        GenericPackager packager = new GenericPackager("packager/iso87ascii.xml");
 
        // Setting packager
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(packager);
 
        // Setting MTI
        isoMsg.set(0, "0100");
 
        // Setting processing code
        isoMsg.set(3, "020000");
 
        // Setting transaction amount
        isoMsg.set(4, "5000");
 
        // Setting transmission date and time
        isoMsg.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
 
        // Setting system trace audit number
        isoMsg.set(11, "123456");
 
        // Setting data element #48
        isoMsg.set(48, "Example Value");
 
        // pack the ISO 8583 Message
        byte[] bIsoMsg = isoMsg.pack();
 
        // output ISO 8583 Message String
        String isoMessage = "";
        for (int i = 0; i < bIsoMsg.length; i++) {
            isoMessage += (char) bIsoMsg[i];
        }
        System.out.println("Packed ISO8385 Message = '"+isoMessage+"'");
    }

}
