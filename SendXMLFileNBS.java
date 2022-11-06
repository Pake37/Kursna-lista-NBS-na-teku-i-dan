package com.nbs_aplikacija;

import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static com.nbs_aplikacija.SheetandJava.getSheetsService;
import static com.nbs_aplikacija.SheetandJava.sheetsService;


public class SendXMLFileNBS {

    public static void main(String[] args) {


        try {

            String url = "https://webservices.nbs.rs/CommunicationOfficeService1_0/ExchangeRateXmlService.asmx?op=GetCurrentExchangeRate";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            String countryCode = "3";
            String korisnik_nbs_aplikacija = System.getenv("nbs_kurs_evro_korisnik_nbs_aplikacija");
            String sifra_nbs_aplikacija = System.getenv("nbs_kurs_evro_sifra_nbs_aplikacija");
            String licenca_id_nbs_aplikacija = System.getenv("nbs_kurs_evro_licenca_id_nbs_aplikacija");

            String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                    "<soap12:Header> <AuthenticationHeader xmlns=\"http://communicationoffice.nbs.rs\"><UserName>" + korisnik_nbs_aplikacija + "</UserName><Password>" + sifra_nbs_aplikacija + "</Password><LicenceID>" + licenca_id_nbs_aplikacija + "</LicenceID></AuthenticationHeader></soap12:Header>" +
                    " <soap12:Body> " +
                    " <GetCurrentExchangeRate xmlns=\"http://communicationoffice.nbs.rs\"> " +
                    " <exchangeRateListTypeID>" + countryCode + "</exchangeRateListTypeID>" +
                    " </GetCurrentExchangeRate>" +
                    " </soap12:Body>" +
                    "</soap12:Envelope>";

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            con.getResponseMessage();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String xml_string = response.toString();
            String xml_string_corrected = xml_string.replace("&lt;", "<").replace("&gt;", ">");

            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            StringReader sr = new StringReader(xml_string_corrected);
            InputSource is = new InputSource(sr);
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            NodeList nbs_tagovi = doc.getElementsByTagName("ExchangeRate");


            for (int i = 0; i < nbs_tagovi.getLength(); i++) {
                Node nbs_tekuca_kursna_lista_node = nbs_tagovi.item(i);

                if (nbs_tekuca_kursna_lista_node.getNodeType() == Node.ELEMENT_NODE) {
                    Element nbs_tekuca_kursna_lista_element = (Element) nbs_tekuca_kursna_lista_node;

                    Element valutaKursa = (Element) nbs_tekuca_kursna_lista_element.getElementsByTagName("CurrencyNameSerLat").item(0);
                    Element srednjiKurs = (Element) nbs_tekuca_kursna_lista_element.getElementsByTagName("MiddleRate").item(0);

                    if (!srednjiKurs.getTextContent().equals("0.0") && valutaKursa.getTextContent().equals("Evro")) {

                        String srednji = srednjiKurs.getTextContent().trim();
                        Float srednji_kurs = Float.parseFloat(srednji);

                        sheetsService = getSheetsService();
                        ValueRange body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(Arrays.asList(Arrays.asList(srednji_kurs)));


                        UpdateValuesResponse response1 = sheetsService.spreadsheets().values().update(SheetandJava.SPREADSHEET_ID, "A3", body).setValueInputOption("RAW").execute();

                    }
                    if ("Evro".equals(valutaKursa.getTextContent())) {

                        sheetsService = getSheetsService();
                        ValueRange body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(Arrays.asList(Arrays.asList(valutaKursa.getTextContent())));


                        sheetsService.spreadsheets().values().update(SheetandJava.SPREADSHEET_ID, "A2", body).setValueInputOption("RAW").execute();
                    }


                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }


    }
}
