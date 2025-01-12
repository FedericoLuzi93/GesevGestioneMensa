package it.gesev.mensa.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBeanBuilder;

import it.gesev.mensa.dao.MensaDAO;
import it.gesev.mensa.dao.ReportDAO;
import it.gesev.mensa.dto.CaricamentoPastiConsumatiDTO;
import it.gesev.mensa.dto.DC4RichiestaDTO;
import it.gesev.mensa.dto.DC4TabellaAllegatoCDTO;
import it.gesev.mensa.dto.DC4TabellaDTO;
import it.gesev.mensa.dto.FEDC1Nominativo;
import it.gesev.mensa.dto.FEPastiDC4Graduati;
import it.gesev.mensa.dto.FEPastiDC4USC;
import it.gesev.mensa.dto.FileDC4DTO;
import it.gesev.mensa.dto.FirmaQuotidianaDC4DTO;
import it.gesev.mensa.dto.FirmeDC4;
import it.gesev.mensa.dto.IdentificativoSistemaDTO;
import it.gesev.mensa.dto.MenuDTO;
import it.gesev.mensa.dto.MenuLeggeroDTO;
import it.gesev.mensa.dto.PastiConsumatiDTO;
import it.gesev.mensa.dto.PastiPrenotatiDTO;
import it.gesev.mensa.dto.PietanzaDTO;
import it.gesev.mensa.dto.SendListPastiDC4AllegatoC;
import it.gesev.mensa.dto.SendListaDC1Prenotati;
import it.gesev.mensa.entity.Ente;
import it.gesev.mensa.entity.IdentificativoSistema;
import it.gesev.mensa.entity.Mensa;
import it.gesev.mensa.entity.PastiConsumati;
import it.gesev.mensa.entity.Pietanza;
import it.gesev.mensa.entity.Prenotazione;
import it.gesev.mensa.entity.TipoDieta;
import it.gesev.mensa.entity.TipoPasto;
import it.gesev.mensa.jasper.DC1MilitariJasper;
import it.gesev.mensa.jasper.DC1NomJasper;
import it.gesev.mensa.jasper.DC1NomNumericaJasper;
import it.gesev.mensa.jasper.FirmaDC4Jasper;
import it.gesev.mensa.jasper.FirmaJasper;
import it.gesev.mensa.jasper.ForzaEffettivaJasper;
import it.gesev.mensa.jasper.GiornoJasper;
import it.gesev.mensa.jasper.NumeroPastiGraduatiJasper;
import it.gesev.mensa.jasper.NumeroPastiUFCJasper;
import it.gesev.mensa.jasper.PastoConsumatoJasper;
import it.gesev.mensa.jasper.PastoOrdinatoJasper;
import it.gesev.mensa.utils.IdentificativoSistemaMapper;
import it.gesev.mensa.utils.MensaUtils;
import it.gesev.mensa.utils.PastiConsumatiMapper;
import it.gesev.mensa.utils.PrenotazioneMapper;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class ReportServiceImpl implements ReportService
{
	@Autowired
	private ReportDAO reportDAO;

	@Autowired
	private MensaDAO mensaDAO;

	@Value("${gesev.data.format}")
	private String dateFormat;

	@Value("${gesev.italian.data.format}")
	private String dateFormatItalian;

	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

	/* Carica pasti consumati CSV */
	@Override
	public void caricaPastiConsumatiCSV(MultipartFile multipartFile) throws IllegalStateException, IOException, ParseException, org.apache.el.parser.ParseException 
	{    
		logger.info("Accesso a caricaPastiConsumatiCSV classe ReportServiceImpl");
		Reader reader = new InputStreamReader(multipartFile.getInputStream());
		CSVReader csvReader = new CSVReaderBuilder(reader).build();

		List<CaricamentoPastiConsumatiDTO> listaPastiConsumatiCSV = new CsvToBeanBuilder(csvReader)
				.withType(CaricamentoPastiConsumatiDTO.class)
				.build()
				.parse();

		reportDAO.caricaPastiConsumati(listaPastiConsumatiCSV);		
	}

	/* Carica pasti consumati JSON */ 
	@Override
	public int caricaPastiConsumatiJson(List<CaricamentoPastiConsumatiDTO> listaPastiConsumatiCSV) throws ParseException, org.apache.el.parser.ParseException
	{
		logger.info("Accesso a caricaPastiConsumatiJson classe ReportServiceImpl");
		reportDAO.caricaPastiConsumati(listaPastiConsumatiCSV);
		return 1;	
	}

	/* Richiesta documento DC4 */
	@Override
	public List<DC4TabellaDTO> richiestaDocumentoDC4(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException 
	{
		logger.info("Accesso a richiestaDocumentoDC4 classe ReportServiceImpl");
		return reportDAO.richiestaDocumentoDC4(dc4RichiestaDTO, true, true, true);
	}

	/* Download File DC4 */
	@Override
	public FileDC4DTO downloadDC4(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException, FileNotFoundException 
	{
		logger.info("Accesso a downloadDC4 classe ReportServiceImpl");
		List<DC4TabellaDTO> listaDC4TabellaDTO = reportDAO.richiestaDocumentoDC4(dc4RichiestaDTO, true, true, true);
		List<FirmeDC4> listaFirmeDC4 = reportDAO.richiestaFirmeDC4(dc4RichiestaDTO);

		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		File mockFile = ResourceUtils.getFile("classpath:DC4.jrxml");

		logger.info("Generazione report in corso...");
		try
		{
			List<FirmaJasper> listaFJ= new ArrayList<>();
			List<FirmaDC4Jasper> listaFirme = new ArrayList<>();
			List<GiornoJasper> listaGJ = new ArrayList<>();
			List<ForzaEffettivaJasper> listaFeJ = new ArrayList<>();
			List<PastoConsumatoJasper> listaPcJ = new ArrayList<>();
			List<PastoOrdinatoJasper> listaPoJ = new ArrayList<>();

			for(DC4TabellaDTO dc4Tab : listaDC4TabellaDTO)
			{
				FirmaJasper fJ = new FirmaJasper();
				ForzaEffettivaJasper feJ = new ForzaEffettivaJasper();
				PastoConsumatoJasper pcJ = new PastoConsumatoJasper();
				PastoOrdinatoJasper poJ = new PastoOrdinatoJasper();

				fJ.setFirma(dc4Tab.getFirma());


				feJ.setColazioneEF(dc4Tab.getColazioneEffettiva() != null ? dc4Tab.getColazioneEffettiva().toString() : "");
				feJ.setPranzoEF(dc4Tab.getPranzoEffettiva() != null ? dc4Tab.getPranzoEffettiva().toString() : "");
				feJ.setCenaEF(dc4Tab.getCenaEffettiva() != null ? dc4Tab.getCenaEffettiva().toString() : "");

				pcJ.setColazionePC(dc4Tab.getColazioneConsumati() != null ? dc4Tab.getColazioneConsumati().toString() : "");
				pcJ.setPranzoPC(dc4Tab.getPranzoConsumati() != null ? dc4Tab.getPranzoConsumati().toString() : "");
				pcJ.setCenaPC(dc4Tab.getCenaConsumati() != null ? dc4Tab.getCenaConsumati().toString() : "");

				poJ.setColazionePO(dc4Tab.getColazioneOrdinati() != null ? dc4Tab.getColazioneOrdinati().toString() : "");
				poJ.setPranzoPO(dc4Tab.getPranzoOrdinati() != null ? dc4Tab.getPranzoOrdinati().toString() : "");
				poJ.setCenaPO(dc4Tab.getCenaOridnati() != null ? dc4Tab.getCenaOridnati().toString() : "");

				listaFJ.add(fJ);
				listaFeJ.add(feJ);
				listaPcJ.add(pcJ);
				listaPoJ.add(poJ);

			}

			//Firme
			for(FirmeDC4 firme : listaFirmeDC4)
			{
				FirmaDC4Jasper fJDC4Jasper = new FirmaDC4Jasper();
				fJDC4Jasper.setDescrizioneFirmaDC4(firme.getDescrizione());
				fJDC4Jasper.setNomeFirmaDC4(firme.getNome());
				fJDC4Jasper.setCognomeFirmaDC4(firme.getCognome());

				listaFirme.add(fJDC4Jasper);
			}

			//Giorni
			int day = 0;
			int max = listaDC4TabellaDTO.size();
			for(int i = 0; i < max; i++)
			{
				day++;
				GiornoJasper gJ = new GiornoJasper(day);
				listaGJ.add(gJ);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaFE = new JRBeanCollectionDataSource(listaFeJ);
			JRBeanCollectionDataSource JRBlistaPO = new JRBeanCollectionDataSource(listaPoJ);
			JRBeanCollectionDataSource JRBlistaPC = new JRBeanCollectionDataSource(listaPcJ);
			JRBeanCollectionDataSource JRBlistaFirme = new JRBeanCollectionDataSource(listaFJ);
			JRBeanCollectionDataSource JRBlistaGiorni = new JRBeanCollectionDataSource(listaGJ);
			JRBeanCollectionDataSource JRBlistaFirmeDC4 = new JRBeanCollectionDataSource(listaFirme);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("forzaEffettivaTab", JRBlistaFE);
			parameters.put("pastoOrdinatoTab", JRBlistaPO);
			parameters.put("pastiConsumatiTab", JRBlistaPC);
			parameters.put("firmaTab", JRBlistaFirme);
			parameters.put("giornoTab", JRBlistaGiorni);
			parameters.put("firmeDocuamentiTab", JRBlistaFirmeDC4);

			//Parametri singoli
			String EDRC = listaDC4TabellaDTO.get(0).getDescrizioneEnte();
			parameters.put("EDRC", EDRC);

			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			String anno = dc4RichiestaDTO.getAnno();
			parameters.put("anno", anno);

			String gestore = "Antonio Rossi";
			parameters.put("gestore", gestore);

			String sottoufficiale = "Marco Verdi";
			parameters.put("sottoufficiale", sottoufficiale);

			String capoServizio = "Tiziano Pinco";
			parameters.put("capoServizio", capoServizio);

			String rappresentante = "Francesco Pallino";
			parameters.put("rappresentante", rappresentante);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC4_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 
		}
		catch(Exception e)
		{

		}

		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Richiesta documento DC4 Allegato C */
	@Override
	public SendListPastiDC4AllegatoC richiestaDocumentoDC4AllegatoC(DC4RichiestaDTO dc4RichiestaDTO, SendListPastiDC4AllegatoC sendObjList) throws ParseException 
	{
		logger.info("Accesso a richiestaDocumentoDC4 classe ReportServiceImpl");
		List<FEPastiDC4USC> listaPastiUFC = new ArrayList<>();
		List<FEPastiDC4Graduati> listaPastiGraduati = new ArrayList<>();
		reportDAO.richiestaDocumentoDC4AllegatoC(dc4RichiestaDTO, listaPastiUFC, listaPastiGraduati, sendObjList);
		return sendObjList;
	}

	/* Download documento DC4 Allegato C */
	@Override
	public FileDC4DTO downloadDC4AllegatoC(DC4RichiestaDTO dc4RichiestaDTO) throws FileNotFoundException 
	{
		List<NumeroPastiUFCJasper> listaPastiUFC = new ArrayList<>();
		List<NumeroPastiGraduatiJasper> listaPastiGraduati = new ArrayList<>();
		List<DC4TabellaAllegatoCDTO> listaDc4TabellaAllegatoCDTO = reportDAO.downloadDocumentoDC4AllegatoC(dc4RichiestaDTO);
		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		File mockFile = ResourceUtils.getFile("classpath:DC4AllegatoC.jrxml");

		logger.info("Generazione report DC4 allegato C in corso...");
		try
		{
			List<GiornoJasper> listaGJ = new ArrayList<GiornoJasper>();

			for(DC4TabellaAllegatoCDTO dc4Tab : listaDc4TabellaAllegatoCDTO)
			{
				NumeroPastiUFCJasper pastoUFCJ = new NumeroPastiUFCJasper();
				NumeroPastiGraduatiJasper pastoGraduatoJ = new NumeroPastiGraduatiJasper();

				pastoUFCJ.setnPranziT1(dc4Tab.getNumpranziUSC().toString());
				pastoUFCJ.setnCeneT1(dc4Tab.getNumCeneUSC().toString());

				pastoGraduatoJ.setnColazioniT2(dc4Tab.getNumColazioniGraduati().toString());
				pastoGraduatoJ.setnPranziT2(dc4Tab.getNumPranziGraduati().toString());
				pastoGraduatoJ.setnCeneT2(dc4Tab.getNumCeneGraduati().toString());

				listaPastiUFC.add(pastoUFCJ);
				listaPastiGraduati.add(pastoGraduatoJ);

			}

			int day = 0;
			int max = listaDc4TabellaAllegatoCDTO.size();
			for(int i = 0; i < max; i++)
			{
				day++;
				GiornoJasper gJ = new GiornoJasper(day);
				listaGJ.add(gJ);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaPastiUff = new JRBeanCollectionDataSource(listaPastiUFC);
			JRBeanCollectionDataSource JRBlistaPastiTruppa = new JRBeanCollectionDataSource(listaPastiGraduati);
			JRBeanCollectionDataSource JRBlistaGiorni1 = new JRBeanCollectionDataSource(listaGJ);
			JRBeanCollectionDataSource JRBlistaGiorni2 = new JRBeanCollectionDataSource(listaGJ);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("Tab1", JRBlistaPastiUff);
			parameters.put("Tab2", JRBlistaPastiTruppa);
			parameters.put("giornoTab", JRBlistaGiorni1);
			parameters.put("giornoTab2", JRBlistaGiorni2);

			//Parametri singoli
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC4_AllegatoC_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 

		}
		catch(Exception e)
		{

		}
		logger.info("Report generato con successo");
		return fileDC4DTO;

	}

	/* Download documento DC4 Allegato C Ufficiali*/
	@Override
	public FileDC4DTO downloadDC4AllegatoCUfficiali(DC4RichiestaDTO dc4RichiestaDTO) throws FileNotFoundException 
	{
		List<NumeroPastiUFCJasper> listaPastiUFC = new ArrayList<>();
		List<DC4TabellaAllegatoCDTO> listaDc4TabellaAllegatoCDTO = reportDAO.downloadDC4AllegatoCUfficiali(dc4RichiestaDTO);
		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		File mockFile = ResourceUtils.getFile("classpath:DC4AllegatoC2Ufficiali.jrxml");

		logger.info("Generazione report DC4 allegato C in corso...");
		try
		{
			List<GiornoJasper> listaGJ = new ArrayList<GiornoJasper>();

			for(DC4TabellaAllegatoCDTO dc4Tab : listaDc4TabellaAllegatoCDTO)
			{
				NumeroPastiUFCJasper pastoUFCJ = new NumeroPastiUFCJasper();

				pastoUFCJ.setnPranziT1(dc4Tab.getNumpranziUSC().toString());
				pastoUFCJ.setnCeneT1(dc4Tab.getNumCeneUSC().toString());
				listaPastiUFC.add(pastoUFCJ);

			}

			int day = 0;
			int max = listaDc4TabellaAllegatoCDTO.size();
			for(int i = 0; i < max; i++)
			{
				day++;
				GiornoJasper gJ = new GiornoJasper(day);
				listaGJ.add(gJ);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaPastiUff = new JRBeanCollectionDataSource(listaPastiUFC);
			JRBeanCollectionDataSource JRBlistaGiorni1 = new JRBeanCollectionDataSource(listaGJ);
			JRBeanCollectionDataSource JRBlistaGiorni2 = new JRBeanCollectionDataSource(listaGJ);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("Tab1", JRBlistaPastiUff);
			parameters.put("giornoTab", JRBlistaGiorni1);
			parameters.put("giornoTab2", JRBlistaGiorni2);

			//Parametri singoli
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC4_AllegatoC_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 

		}
		catch(Exception e)
		{

		}
		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Download documento DC4 Allegato C Graduati*/
	@Override
	public FileDC4DTO downloadDC4AllegatoCGraduati(DC4RichiestaDTO dc4RichiestaDTO) throws FileNotFoundException {
		List<NumeroPastiGraduatiJasper> listaPastiGraduati = new ArrayList<>();
		List<DC4TabellaAllegatoCDTO> listaDc4TabellaAllegatoCDTO = reportDAO.downloadDC4AllegatoCGraduati(dc4RichiestaDTO);
		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		File mockFile = ResourceUtils.getFile("classpath:DC4AllegatoC2Graduati.jrxml");

		logger.info("Generazione report DC4 allegato C in corso...");
		try
		{
			List<GiornoJasper> listaGJ = new ArrayList<GiornoJasper>();

			for(DC4TabellaAllegatoCDTO dc4Tab : listaDc4TabellaAllegatoCDTO)
			{

				NumeroPastiGraduatiJasper pastoGraduatoJ = new NumeroPastiGraduatiJasper();

				pastoGraduatoJ.setnColazioniT2(dc4Tab.getNumColazioniGraduati().toString());
				pastoGraduatoJ.setnPranziT2(dc4Tab.getNumPranziGraduati().toString());
				pastoGraduatoJ.setnCeneT2(dc4Tab.getNumCeneGraduati().toString());

				listaPastiGraduati.add(pastoGraduatoJ);

			}

			int day = 0;
			int max = listaDc4TabellaAllegatoCDTO.size();
			for(int i = 0; i < max; i++)
			{
				day++;
				GiornoJasper gJ = new GiornoJasper(day);
				listaGJ.add(gJ);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaPastiTruppa = new JRBeanCollectionDataSource(listaPastiGraduati);
			JRBeanCollectionDataSource JRBlistaGiorni1 = new JRBeanCollectionDataSource(listaGJ);
			JRBeanCollectionDataSource JRBlistaGiorni2 = new JRBeanCollectionDataSource(listaGJ);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("Tab2", JRBlistaPastiTruppa);
			parameters.put("giornoTab", JRBlistaGiorni1);
			parameters.put("giornoTab2", JRBlistaGiorni2);

			//Parametri singoli
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC4_AllegatoC_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 

		}
		catch(Exception e)
		{
			logger.info("si è verificata un eccezione", e);
		}
		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Leggi tutti identificativi Sistema */
	@Override
	public List<IdentificativoSistemaDTO> getAllIdentificativiSistema() 
	{
		logger.info("Accesso a getAllIdentificativiSistem, classe ReportServiceImpl");
		List<IdentificativoSistema> listaIdentificativoSistema = reportDAO.getAllIdentificativiSistema();
		List<IdentificativoSistemaDTO> listaIdentificativoSistemaDTO = new ArrayList<>();
		logger.info("Inizio ciclo For in getAllIdentificativiSistem classe ReportServiceImpl");
		for(IdentificativoSistema is : listaIdentificativoSistema)
		{
			IdentificativoSistemaDTO isDTO = null;
			isDTO = IdentificativoSistemaMapper.mapToDTO(is);
			listaIdentificativoSistemaDTO.add(isDTO);
		}
		logger.info("Fine getAllIdentificativiSistem classe ReportServiceImpl");
		return listaIdentificativoSistemaDTO;
	}

	/* Aggiungi una nuova Firma */
	@Override
	public int createNuovaFirma(FirmaQuotidianaDC4DTO firmaQuotidianaDC4DTO) throws ParseException
	{
		logger.info("Accesso a createNuovaFirma classe ReportServiceImpl");
		reportDAO.createNuovaFirma(firmaQuotidianaDC4DTO);
		return 1;
	}

	/* Cancella un Firma */
	@Override
	public int deleteFirma(FirmaQuotidianaDC4DTO firmaQuotidianaDC4DTO) throws ParseException 
	{
		logger.info("Accesso a deleteFirma classe ReportServiceImpl");
		reportDAO.deleteFirma(firmaQuotidianaDC4DTO);
		return 1;
	}

	/* Richiesta documento DC1 Prenotati */
	@Override
	public List<DC1MilitariJasper> richiestaDocumentoDC1Prenotati(DC4RichiestaDTO dc4RichiestaDTO,
			SendListaDC1Prenotati sendObjList) throws ParseException 
	{
		logger.info("Accesso a richiestaDocumentoDC1Prenotati classe ReportServiceImpl");
		SendListaDC1Prenotati so = reportDAO.richiestaDocumentoDC1Prenotati(dc4RichiestaDTO, sendObjList);
		List<DC1MilitariJasper> listaDC1Prenotati	= new ArrayList<>();

		//Militari
		DC1MilitariJasper dc1M = new DC1MilitariJasper();
		dc1M.setNome("Militari e graduati di truppa");
		dc1M.setOrCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrCoPrenotati(so.getOrdColMil());
		dc1M.setOrPrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrPrPrenotati(so.getOrdPraMil());
		dc1M.setOrCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrCePrenotati(so.getOrdCenMil());

		dc1M.setMeCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMeCoPrenotati(so.getMedColMil());
		dc1M.setMePrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMePrPrenotati(so.getMedPraMil());
		dc1M.setMeCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMeCePrenotati(so.getMedCenMil());

		dc1M.setPeCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPeCoPrenotati(so.getPesColMil());
		dc1M.setPePrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPePrPrenotati(so.getPesPraMil());
		dc1M.setPeCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPeCePrenotati(so.getPesCenMil());

		dc1M.setCbt(so.getCbtMil());
		dc1M.setSpecchio(so.getSpecchioMil());
		dc1M.setColObb(so.getColazioneObblMil());

		listaDC1Prenotati.add(dc1M);

		//Personale TG
		DC1MilitariJasper dc1P = new DC1MilitariJasper();
		dc1P.setNome("Personale ammesso al vitto a titolo gratuito");
		dc1P.setOrCoAventiDiritto(so.getAventiDiritto());
		dc1P.setOrCoPrenotati(so.getOrdColTg());
		dc1P.setOrPrAventiDiritto(so.getAventiDiritto());
		dc1P.setOrPrPrenotati(so.getOrdPraTg());
		dc1P.setOrCeAventiDiritto(so.getAventiDiritto());
		dc1P.setOrCePrenotati(so.getOrdCenTg());

		dc1P.setMeCoAventiDiritto(so.getAventiDiritto());
		dc1P.setMeCoPrenotati(so.getMedColTg());
		dc1P.setMePrAventiDiritto(so.getAventiDiritto());
		dc1P.setMePrPrenotati(so.getMedPraTg());
		dc1P.setMeCeAventiDiritto(so.getAventiDiritto());
		dc1P.setMeCePrenotati(so.getMedCenTg());

		dc1P.setPeCoAventiDiritto(so.getAventiDiritto());
		dc1P.setPeCoPrenotati(so.getPesColTg());
		dc1P.setPePrAventiDiritto(so.getAventiDiritto());
		dc1P.setPePrPrenotati(so.getPesPraTg());
		dc1P.setPeCeAventiDiritto(so.getAventiDiritto());
		dc1P.setPeCePrenotati(so.getPesCenTg());

		dc1P.setCbt(so.getCbtTg());
		dc1P.setSpecchio(so.getSpecchioTg());
		dc1P.setColObb(so.getColazioneObblTg());

		listaDC1Prenotati.add(dc1P);

		//Personale TO
		DC1MilitariJasper dc1O = new DC1MilitariJasper();
		dc1O.setNome("Personale ammesso alla mensa a pagamento");
		dc1O.setOrCoAventiDiritto(so.getAventiDiritto());
		dc1O.setOrCoPrenotati(so.getOrdColTo());
		dc1O.setOrPrAventiDiritto(so.getAventiDiritto());
		dc1O.setOrPrPrenotati(so.getOrdPraTo());
		dc1O.setOrCeAventiDiritto(so.getAventiDiritto());
		dc1O.setOrCePrenotati(so.getOrdCenTo());

		dc1O.setMeCoAventiDiritto(so.getAventiDiritto());
		dc1O.setMeCoPrenotati(so.getMedColTo());
		dc1O.setMePrAventiDiritto(so.getAventiDiritto());
		dc1O.setMePrPrenotati(so.getMedPraTo());
		dc1O.setMeCeAventiDiritto(so.getAventiDiritto());
		dc1O.setMeCePrenotati(so.getMedCenTo());

		dc1O.setPeCoAventiDiritto(so.getAventiDiritto());
		dc1O.setPeCoPrenotati(so.getPesColTo());
		dc1O.setPePrAventiDiritto(so.getAventiDiritto());
		dc1O.setPePrPrenotati(so.getPesPraTo());
		dc1O.setPeCeAventiDiritto(so.getAventiDiritto());
		dc1O.setPeCePrenotati(so.getPesCenTo());

		dc1O.setCbt(so.getCbtTo());
		dc1O.setSpecchio(so.getSpecchioTo());
		dc1O.setColObb(so.getColazioneObblTo());

		listaDC1Prenotati.add(dc1O);

		//Parziale
		DC1MilitariJasper parziale = new DC1MilitariJasper();
		parziale.setNome("PARZIALE");
		parziale.setOrCoAventiDiritto(so.getAventiDiritto());
		parziale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
		parziale.setOrPrAventiDiritto(so.getAventiDiritto());
		parziale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
		parziale.setOrCeAventiDiritto(so.getAventiDiritto());
		parziale.setOrCePrenotati(so.getOrdCenTo() +  so.getOrdCenTg());

		parziale.setMeCoAventiDiritto(so.getAventiDiritto());
		parziale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
		parziale.setMePrAventiDiritto(so.getAventiDiritto());
		parziale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
		parziale.setMeCeAventiDiritto(so.getAventiDiritto());
		parziale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

		parziale.setPeCoAventiDiritto(so.getAventiDiritto());
		parziale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
		parziale.setPePrAventiDiritto(so.getAventiDiritto());
		parziale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
		parziale.setPeCeAventiDiritto(so.getAventiDiritto());
		parziale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

		parziale.setCbt(so.getCbtTg() + so.getCbtTo());
		parziale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
		parziale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

		listaDC1Prenotati.add(parziale);

		//Campionatura
		DC1MilitariJasper campionatura = new DC1MilitariJasper();
		campionatura.setNome("CAMPIONATURA");
		campionatura.setOrCoAventiDiritto(0);
		campionatura.setOrCoPrenotati(0);
		campionatura.setOrPrAventiDiritto(1);
		campionatura.setOrPrPrenotati(0);
		campionatura.setOrCeAventiDiritto(0);
		campionatura.setOrCePrenotati(0);

		campionatura.setMeCoAventiDiritto(0);
		campionatura.setMeCoPrenotati(0);
		campionatura.setMePrAventiDiritto(0);
		campionatura.setMePrPrenotati(0);
		campionatura.setMeCeAventiDiritto(0);
		campionatura.setMeCePrenotati(0);

		campionatura.setPeCoAventiDiritto(0);
		campionatura.setPeCoPrenotati(0);
		campionatura.setPePrAventiDiritto(0);
		campionatura.setPePrPrenotati(0);
		campionatura.setPeCeAventiDiritto(0);
		campionatura.setPeCePrenotati(0);

		campionatura.setCbt(0);
		campionatura.setSpecchio(0);
		campionatura.setColObb(0);

		listaDC1Prenotati.add(campionatura);

		//Totale
		DC1MilitariJasper totale = new DC1MilitariJasper();
		totale.setNome("TOTALE");
		totale.setOrCoAventiDiritto(so.getAventiDiritto());
		totale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
		totale.setOrPrAventiDiritto(so.getAventiDiritto() + campionatura.getOrPrAventiDiritto());
		totale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
		totale.setOrCeAventiDiritto(so.getAventiDiritto());
		totale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

		totale.setMeCoAventiDiritto(so.getAventiDiritto());
		totale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
		totale.setMePrAventiDiritto(so.getAventiDiritto());
		totale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
		totale.setMeCeAventiDiritto(so.getAventiDiritto());
		totale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

		totale.setPeCoAventiDiritto(so.getAventiDiritto());
		totale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
		totale.setPePrAventiDiritto(so.getAventiDiritto());
		totale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
		totale.setPeCeAventiDiritto(so.getAventiDiritto());
		totale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

		totale.setCbt( so.getCbtTg() + so.getCbtTo());
		totale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
		totale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

		listaDC1Prenotati.add(totale);

		return listaDC1Prenotati;
	}

	/* Download documento DC1 Prenotati */
	@Override
	public FileDC4DTO downloadDC1Prenotati(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException, FileNotFoundException 
	{
		logger.info("Accesso a downloadDC1Prenotati classe ReportServiceImpl");
		SendListaDC1Prenotati sendObjList = null;
		SendListaDC1Prenotati so = reportDAO.richiestaDocumentoDC1Prenotati(dc4RichiestaDTO, sendObjList);
		List<FirmeDC4> listaFirmeDC4 = reportDAO.richiestaFirmeDC1(dc4RichiestaDTO);

		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		List<DC1MilitariJasper> listaDC1Ordinarie = new ArrayList<>();
		List<FirmaDC4Jasper> listaFirme = new ArrayList<>();

		File mockFile = ResourceUtils.getFile("classpath:DC1Prenotati.jrxml");

		logger.info("Generazione report DC1 in corso...");
		try
		{
			//Militari
			DC1MilitariJasper dc1M = new DC1MilitariJasper();
			dc1M.setOrCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrCoPrenotati(so.getOrdColMil());
			dc1M.setOrPrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrPrPrenotati(so.getOrdPraMil());
			dc1M.setOrCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrCePrenotati(so.getOrdCenMil());

			dc1M.setMeCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMeCoPrenotati(so.getMedColMil());
			dc1M.setMePrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMePrPrenotati(so.getMedPraMil());
			dc1M.setMeCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMeCePrenotati(so.getMedCenMil());

			dc1M.setPeCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPeCoPrenotati(so.getPesColMil());
			dc1M.setPePrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPePrPrenotati(so.getPesPraMil());
			dc1M.setPeCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPeCePrenotati(so.getPesCenMil());

			dc1M.setCbt(so.getCbtMil());
			dc1M.setSpecchio(so.getSpecchioMil());
			dc1M.setColObb(so.getColazioneObblMil());

			listaDC1Ordinarie.add(dc1M);

			//Personale TG
			DC1MilitariJasper dc1P = new DC1MilitariJasper();
			dc1P.setOrCoAventiDiritto(so.getAventiDiritto());
			dc1P.setOrCoPrenotati(so.getOrdColTg());
			dc1P.setOrPrAventiDiritto(so.getAventiDiritto());
			dc1P.setOrPrPrenotati(so.getOrdPraTg());
			dc1P.setOrCeAventiDiritto(so.getAventiDiritto());
			dc1P.setOrCePrenotati(so.getOrdCenTg());

			dc1P.setMeCoAventiDiritto(so.getAventiDiritto());
			dc1P.setMeCoPrenotati(so.getMedColTg());
			dc1P.setMePrAventiDiritto(so.getAventiDiritto());
			dc1P.setMePrPrenotati(so.getMedPraTg());
			dc1P.setMeCeAventiDiritto(so.getAventiDiritto());
			dc1P.setMeCePrenotati(so.getMedCenTg());

			dc1P.setPeCoAventiDiritto(so.getAventiDiritto());
			dc1P.setPeCoPrenotati(so.getPesColTg());
			dc1P.setPePrAventiDiritto(so.getAventiDiritto());
			dc1P.setPePrPrenotati(so.getPesPraTg());
			dc1P.setPeCeAventiDiritto(so.getAventiDiritto());
			dc1P.setPeCePrenotati(so.getPesCenTg());

			dc1P.setCbt(so.getCbtTg());
			dc1P.setSpecchio(so.getSpecchioTg());
			dc1P.setColObb(so.getColazioneObblTg());

			listaDC1Ordinarie.add(dc1P);

			//Personale TO
			DC1MilitariJasper dc1O = new DC1MilitariJasper();
			dc1O.setOrCoAventiDiritto(so.getAventiDiritto());
			dc1O.setOrCoPrenotati(so.getOrdColTo());
			dc1O.setOrPrAventiDiritto(so.getAventiDiritto());
			dc1O.setOrPrPrenotati(so.getOrdPraTo());
			dc1O.setOrCeAventiDiritto(so.getAventiDiritto());
			dc1O.setOrCePrenotati(so.getOrdCenTo());

			dc1O.setMeCoAventiDiritto(so.getAventiDiritto());
			dc1O.setMeCoPrenotati(so.getMedColTo());
			dc1O.setMePrAventiDiritto(so.getAventiDiritto());
			dc1O.setMePrPrenotati(so.getMedPraTo());
			dc1O.setMeCeAventiDiritto(so.getAventiDiritto());
			dc1O.setMeCePrenotati(so.getMedCenTo());

			dc1O.setPeCoAventiDiritto(so.getAventiDiritto());
			dc1O.setPeCoPrenotati(so.getPesColTo());
			dc1O.setPePrAventiDiritto(so.getAventiDiritto());
			dc1O.setPePrPrenotati(so.getPesPraTo());
			dc1O.setPeCeAventiDiritto(so.getAventiDiritto());
			dc1O.setPeCePrenotati(so.getPesCenTo());

			dc1O.setCbt(so.getCbtTo());
			dc1O.setSpecchio(so.getSpecchioTo());
			dc1O.setColObb(so.getColazioneObblTo());

			listaDC1Ordinarie.add(dc1O);

			//Parziale
			DC1MilitariJasper parziale = new DC1MilitariJasper();
			parziale.setOrCoAventiDiritto(so.getAventiDiritto());
			parziale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
			parziale.setOrPrAventiDiritto(so.getAventiDiritto());
			parziale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
			parziale.setOrCeAventiDiritto(so.getAventiDiritto());
			parziale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

			parziale.setMeCoAventiDiritto(so.getAventiDiritto());
			parziale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
			parziale.setMePrAventiDiritto(so.getAventiDiritto());
			parziale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
			parziale.setMeCeAventiDiritto(so.getAventiDiritto());
			parziale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

			parziale.setPeCoAventiDiritto(so.getAventiDiritto());
			parziale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
			parziale.setPePrAventiDiritto(so.getAventiDiritto());
			parziale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
			parziale.setPeCeAventiDiritto(so.getAventiDiritto());
			parziale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

			parziale.setCbt(so.getCbtTg() + so.getCbtTo());
			parziale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
			parziale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

			listaDC1Ordinarie.add(parziale);

			//Campionatura
			DC1MilitariJasper campionatura = new DC1MilitariJasper();
			campionatura.setOrCoAventiDiritto(0);
			campionatura.setOrCoPrenotati(0);
			campionatura.setOrPrAventiDiritto(1);
			campionatura.setOrPrPrenotati(0);
			campionatura.setOrCeAventiDiritto(0);
			campionatura.setOrCePrenotati(0);

			campionatura.setMeCoAventiDiritto(0);
			campionatura.setMeCoPrenotati(0);
			campionatura.setMePrAventiDiritto(0);
			campionatura.setMePrPrenotati(0);
			campionatura.setMeCeAventiDiritto(0);
			campionatura.setMeCePrenotati(0);

			campionatura.setPeCoAventiDiritto(0);
			campionatura.setPeCoPrenotati(0);
			campionatura.setPePrAventiDiritto(0);
			campionatura.setPePrPrenotati(0);
			campionatura.setPeCeAventiDiritto(0);
			campionatura.setPeCePrenotati(0);

			campionatura.setCbt(0);
			campionatura.setSpecchio(0);
			campionatura.setColObb(0);

			listaDC1Ordinarie.add(campionatura);

			//Totale
			DC1MilitariJasper totale = new DC1MilitariJasper();
			totale.setOrCoAventiDiritto(so.getAventiDiritto());
			totale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
			totale.setOrPrAventiDiritto(so.getAventiDiritto() + campionatura.getOrPrAventiDiritto());
			totale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
			totale.setOrCeAventiDiritto(so.getAventiDiritto());
			totale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

			totale.setMeCoAventiDiritto(so.getAventiDiritto());
			totale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
			totale.setMePrAventiDiritto(so.getAventiDiritto());
			totale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
			totale.setMeCeAventiDiritto(so.getAventiDiritto());
			totale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

			totale.setPeCoAventiDiritto(so.getAventiDiritto());
			totale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
			totale.setPePrAventiDiritto(so.getAventiDiritto());
			totale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
			totale.setPeCeAventiDiritto(so.getAventiDiritto());
			totale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

			totale.setCbt(so.getCbtTg() + so.getCbtTo());
			totale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
			totale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

			listaDC1Ordinarie.add(totale);

			//Firme
			for(FirmeDC4 firme : listaFirmeDC4)
			{
				FirmaDC4Jasper fJDC4Jasper = new FirmaDC4Jasper();
				fJDC4Jasper.setDescrizioneFirmaDC4(firme.getDescrizione());
				fJDC4Jasper.setNomeFirmaDC4(firme.getNome());
				fJDC4Jasper.setCognomeFirmaDC4(firme.getCognome());

				listaFirme.add(fJDC4Jasper);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaOrdinarie = new JRBeanCollectionDataSource(listaDC1Ordinarie);
			JRBeanCollectionDataSource JRBlistaFirmeDC4 = new JRBeanCollectionDataSource(listaFirme);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("TabOrdinarie", JRBlistaOrdinarie);
			parameters.put("TabFirmeDocumenti", JRBlistaFirmeDC4);

			//Parametri singoli
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			String anno = dc4RichiestaDTO.getAnno();
			parameters.put("anno", anno);

			String giorno = dc4RichiestaDTO.getGiorno();
			parameters.put("giorno", giorno);

			String ente = so.getDescrizioneEnte();
			parameters.put("ente", ente);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC1_Prenotati_" + dc4RichiestaDTO.getGiorno() + "_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 
		}
		catch(Exception e)
		{
			logger.info("si è verificata un eccezione", e);
		}

		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Richiesta documento DC1 Consumati */
	@Override
	public List<DC1MilitariJasper> richiestaDocumentoDC1Consumati(DC4RichiestaDTO dc4RichiestaDTO,
			SendListaDC1Prenotati sendObjList) throws ParseException 
	{
		logger.info("Accesso a richiestaDocumentoDC1Prenotati classe ReportServiceImpl");
		SendListaDC1Prenotati so = reportDAO.richiestaDocumentoDC1Consumati(dc4RichiestaDTO, sendObjList);
		List<DC1MilitariJasper> listaDC1Prenotati	= new ArrayList<>();

		//Militari
		DC1MilitariJasper dc1M = new DC1MilitariJasper();
		dc1M.setNome("Militari e graduati di truppa");
		dc1M.setOrCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrCoPrenotati(so.getOrdColMil());
		dc1M.setOrPrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrPrPrenotati(so.getOrdPraMil());
		dc1M.setOrCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setOrCePrenotati(so.getOrdCenMil());

		dc1M.setMeCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMeCoPrenotati(so.getMedColMil());
		dc1M.setMePrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMePrPrenotati(so.getMedPraMil());
		dc1M.setMeCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setMeCePrenotati(so.getMedCenMil());

		dc1M.setPeCoAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPeCoPrenotati(so.getPesColMil());
		dc1M.setPePrAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPePrPrenotati(so.getPesPraMil());
		dc1M.setPeCeAventiDiritto(so.getAventiDirittoMilitari());
		dc1M.setPeCePrenotati(so.getPesCenMil());

		dc1M.setCbt(so.getCbtMil());
		dc1M.setSpecchio(so.getSpecchioMil());
		dc1M.setColObb(so.getColazioneObblMil());

		listaDC1Prenotati.add(dc1M);

		//Personale TG
		DC1MilitariJasper dc1P = new DC1MilitariJasper();
		dc1P.setNome("Personale ammesso al vitto a titolo gratuito");
		dc1P.setOrCoAventiDiritto(so.getAventiDiritto());
		dc1P.setOrCoPrenotati(so.getOrdColTg());
		dc1P.setOrPrAventiDiritto(so.getAventiDiritto());
		dc1P.setOrPrPrenotati(so.getOrdPraTg());
		dc1P.setOrCeAventiDiritto(so.getAventiDiritto());
		dc1P.setOrCePrenotati(so.getOrdCenTg());

		dc1P.setMeCoAventiDiritto(so.getAventiDiritto());
		dc1P.setMeCoPrenotati(so.getMedColTg());
		dc1P.setMePrAventiDiritto(so.getAventiDiritto());
		dc1P.setMePrPrenotati(so.getMedPraTg());
		dc1P.setMeCeAventiDiritto(so.getAventiDiritto());
		dc1P.setMeCePrenotati(so.getMedCenTg());

		dc1P.setPeCoAventiDiritto(so.getAventiDiritto());
		dc1P.setPeCoPrenotati(so.getPesColTg());
		dc1P.setPePrAventiDiritto(so.getAventiDiritto());
		dc1P.setPePrPrenotati(so.getPesPraTg());
		dc1P.setPeCeAventiDiritto(so.getAventiDiritto());
		dc1P.setPeCePrenotati(so.getPesCenTg());

		dc1P.setCbt(so.getCbtTg());
		dc1P.setSpecchio(so.getSpecchioTg());
		dc1P.setColObb(so.getColazioneObblTg());

		listaDC1Prenotati.add(dc1P);

		//Personale TO
		DC1MilitariJasper dc1O = new DC1MilitariJasper();
		dc1O.setNome("Personale ammesso alla mensa a pagamento");
		dc1O.setOrCoAventiDiritto(so.getAventiDiritto());
		dc1O.setOrCoPrenotati(so.getOrdColTo());
		dc1O.setOrPrAventiDiritto(so.getAventiDiritto());
		dc1O.setOrPrPrenotati(so.getOrdPraTo());
		dc1O.setOrCeAventiDiritto(so.getAventiDiritto());
		dc1O.setOrCePrenotati(so.getOrdCenTo());

		dc1O.setMeCoAventiDiritto(so.getAventiDiritto());
		dc1O.setMeCoPrenotati(so.getMedColTo());
		dc1O.setMePrAventiDiritto(so.getAventiDiritto());
		dc1O.setMePrPrenotati(so.getMedPraTo());
		dc1O.setMeCeAventiDiritto(so.getAventiDiritto());
		dc1O.setMeCePrenotati(so.getMedCenTo());

		dc1O.setPeCoAventiDiritto(so.getAventiDiritto());
		dc1O.setPeCoPrenotati(so.getPesColTo());
		dc1O.setPePrAventiDiritto(so.getAventiDiritto());
		dc1O.setPePrPrenotati(so.getPesPraTo());
		dc1O.setPeCeAventiDiritto(so.getAventiDiritto());
		dc1O.setPeCePrenotati(so.getPesCenTo());

		dc1O.setCbt(so.getCbtTo());
		dc1O.setSpecchio(so.getSpecchioTo());
		dc1O.setColObb(so.getColazioneObblTo());

		listaDC1Prenotati.add(dc1O);

		//Parziale
		DC1MilitariJasper parziale = new DC1MilitariJasper();
		parziale.setNome("PARZIALE");
		parziale.setOrCoAventiDiritto(so.getAventiDiritto());
		parziale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
		parziale.setOrPrAventiDiritto(so.getAventiDiritto());
		parziale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
		parziale.setOrCeAventiDiritto(so.getAventiDiritto());
		parziale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

		parziale.setMeCoAventiDiritto(so.getAventiDiritto());
		parziale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
		parziale.setMePrAventiDiritto(so.getAventiDiritto());
		parziale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
		parziale.setMeCeAventiDiritto(so.getAventiDiritto());
		parziale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

		parziale.setPeCoAventiDiritto(so.getAventiDiritto());
		parziale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
		parziale.setPePrAventiDiritto(so.getAventiDiritto());
		parziale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
		parziale.setPeCeAventiDiritto(so.getAventiDiritto());
		parziale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

		parziale.setCbt(so.getCbtTg() + so.getCbtTo());
		parziale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
		parziale.setColObb(so.getSpecchioTg() + so.getSpecchioTo());

		listaDC1Prenotati.add(parziale);

		//Campionatura
		DC1MilitariJasper campionatura = new DC1MilitariJasper();
		campionatura.setNome("CAMPIONATURA");
		campionatura.setOrCoAventiDiritto(0);
		campionatura.setOrCoPrenotati(0);
		campionatura.setOrPrAventiDiritto(1);
		campionatura.setOrPrPrenotati(0);
		campionatura.setOrCeAventiDiritto(0);
		campionatura.setOrCePrenotati(0);

		campionatura.setMeCoAventiDiritto(0);
		campionatura.setMeCoPrenotati(0);
		campionatura.setMePrAventiDiritto(0);
		campionatura.setMePrPrenotati(0);
		campionatura.setMeCeAventiDiritto(0);
		campionatura.setMeCePrenotati(0);

		campionatura.setPeCoAventiDiritto(0);
		campionatura.setPeCoPrenotati(0);
		campionatura.setPePrAventiDiritto(0);
		campionatura.setPePrPrenotati(0);
		campionatura.setPeCeAventiDiritto(0);
		campionatura.setPeCePrenotati(0);

		campionatura.setCbt(0);
		campionatura.setSpecchio(0);
		campionatura.setColObb(0);

		listaDC1Prenotati.add(campionatura);

		//Totale
		DC1MilitariJasper totale = new DC1MilitariJasper();
		totale.setNome("TOTALE");
		totale.setOrCoAventiDiritto(so.getAventiDiritto());
		totale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
		totale.setOrPrAventiDiritto(so.getAventiDiritto());
		totale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
		totale.setOrCeAventiDiritto(so.getAventiDiritto());
		totale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

		totale.setMeCoAventiDiritto(so.getAventiDiritto());
		totale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
		totale.setMePrAventiDiritto(so.getAventiDiritto());
		totale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
		totale.setMeCeAventiDiritto(so.getAventiDiritto());
		totale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

		totale.setPeCoAventiDiritto(so.getAventiDiritto());
		totale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
		totale.setPePrAventiDiritto(so.getAventiDiritto());
		totale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
		totale.setPeCeAventiDiritto(so.getAventiDiritto());
		totale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

		totale.setCbt(so.getCbtTg() + so.getCbtTo());
		totale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
		totale.setColObb(so.getSpecchioTg() + so.getSpecchioTo());

		listaDC1Prenotati.add(totale);

		return listaDC1Prenotati;
	}

	/* Download documento DC1 Consumati */
	@Override
	public FileDC4DTO downloadDC1Consumati(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException, FileNotFoundException 
	{
		logger.info("Accesso a downloadDC1Consumati classe ReportServiceImpl");
		SendListaDC1Prenotati sendObjList = null;
		SendListaDC1Prenotati so = reportDAO.richiestaDocumentoDC1Consumati(dc4RichiestaDTO, sendObjList);
		List<FirmeDC4> listaFirmeDC4 = reportDAO.richiestaFirmeDC1(dc4RichiestaDTO);

		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		List<DC1MilitariJasper> listaDC1Ordinarie = new ArrayList<>();
		List<FirmaDC4Jasper> listaFirme = new ArrayList<>();

		File mockFile = ResourceUtils.getFile("classpath:DC1Consumati.jrxml");

		logger.info("Generazione report DC1 Consumati in corso...");
		try
		{
			//Militari
			DC1MilitariJasper dc1M = new DC1MilitariJasper();
			dc1M.setOrCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrCoPrenotati(so.getOrdColMil());
			dc1M.setOrPrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrPrPrenotati(so.getOrdPraMil());
			dc1M.setOrCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setOrCePrenotati(so.getOrdCenMil());

			dc1M.setMeCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMeCoPrenotati(so.getMedColMil());
			dc1M.setMePrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMePrPrenotati(so.getMedPraMil());
			dc1M.setMeCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setMeCePrenotati(so.getMedCenMil());

			dc1M.setPeCoAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPeCoPrenotati(so.getPesColMil());
			dc1M.setPePrAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPePrPrenotati(so.getPesPraMil());
			dc1M.setPeCeAventiDiritto(so.getAventiDirittoMilitari());
			dc1M.setPeCePrenotati(so.getPesCenMil());

			dc1M.setCbt(so.getCbtMil());
			dc1M.setSpecchio(so.getSpecchioMil());
			dc1M.setColObb(so.getColazioneObblMil());

			listaDC1Ordinarie.add(dc1M);

			//Personale TG
			DC1MilitariJasper dc1P = new DC1MilitariJasper();
			dc1P.setOrCoAventiDiritto(so.getAventiDiritto());
			dc1P.setOrCoPrenotati(so.getOrdColTg());
			dc1P.setOrPrAventiDiritto(so.getAventiDiritto());
			dc1P.setOrPrPrenotati(so.getOrdPraTg());
			dc1P.setOrCeAventiDiritto(so.getAventiDiritto());
			dc1P.setOrCePrenotati(so.getOrdCenTg());

			dc1P.setMeCoAventiDiritto(so.getAventiDiritto());
			dc1P.setMeCoPrenotati(so.getMedColTg());
			dc1P.setMePrAventiDiritto(so.getAventiDiritto());
			dc1P.setMePrPrenotati(so.getMedPraTg());
			dc1P.setMeCeAventiDiritto(so.getAventiDiritto());
			dc1P.setMeCePrenotati(so.getMedCenTg());

			dc1P.setPeCoAventiDiritto(so.getAventiDiritto());
			dc1P.setPeCoPrenotati(so.getPesColTg());
			dc1P.setPePrAventiDiritto(so.getAventiDiritto());
			dc1P.setPePrPrenotati(so.getPesPraTg());
			dc1P.setPeCeAventiDiritto(so.getAventiDiritto());
			dc1P.setPeCePrenotati(so.getPesCenTg());

			dc1P.setCbt(so.getCbtTg());
			dc1P.setSpecchio(so.getSpecchioTg());
			dc1P.setColObb(so.getColazioneObblTg());

			listaDC1Ordinarie.add(dc1P);

			//Personale TO
			DC1MilitariJasper dc1O = new DC1MilitariJasper();
			dc1O.setOrCoAventiDiritto(so.getAventiDiritto());
			dc1O.setOrCoPrenotati(so.getOrdColTo());
			dc1O.setOrPrAventiDiritto(so.getAventiDiritto());
			dc1O.setOrPrPrenotati(so.getOrdPraTo());
			dc1O.setOrCeAventiDiritto(so.getAventiDiritto());
			dc1O.setOrCePrenotati(so.getOrdCenTo());

			dc1O.setMeCoAventiDiritto(so.getAventiDiritto());
			dc1O.setMeCoPrenotati(so.getMedColTo());
			dc1O.setMePrAventiDiritto(so.getAventiDiritto());
			dc1O.setMePrPrenotati(so.getMedPraTo());
			dc1O.setMeCeAventiDiritto(so.getAventiDiritto());
			dc1O.setMeCePrenotati(so.getMedCenTo());

			dc1O.setPeCoAventiDiritto(so.getAventiDiritto());
			dc1O.setPeCoPrenotati(so.getPesColTo());
			dc1O.setPePrAventiDiritto(so.getAventiDiritto());
			dc1O.setPePrPrenotati(so.getPesPraTo());
			dc1O.setPeCeAventiDiritto(so.getAventiDiritto());
			dc1O.setPeCePrenotati(so.getPesCenTo());

			dc1O.setCbt(so.getCbtTo());
			dc1O.setSpecchio(so.getSpecchioTo());
			dc1O.setColObb(so.getColazioneObblTo());

			listaDC1Ordinarie.add(dc1O);

			//Parziale
			DC1MilitariJasper parziale = new DC1MilitariJasper();
			parziale.setOrCoAventiDiritto(so.getAventiDiritto());
			parziale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
			parziale.setOrPrAventiDiritto(so.getAventiDiritto());
			parziale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
			parziale.setOrCeAventiDiritto(so.getAventiDiritto());
			parziale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

			parziale.setMeCoAventiDiritto(so.getAventiDiritto());
			parziale.setMeCoPrenotati(so.getMedColTo() +  so.getMedColTg());
			parziale.setMePrAventiDiritto(so.getAventiDiritto());
			parziale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
			parziale.setMeCeAventiDiritto(so.getAventiDiritto());
			parziale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

			parziale.setPeCoAventiDiritto(so.getAventiDiritto());
			parziale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
			parziale.setPePrAventiDiritto(so.getAventiDiritto());
			parziale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
			parziale.setPeCeAventiDiritto(so.getAventiDiritto());
			parziale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

			parziale.setCbt(so.getCbtTg() + so.getCbtTo());
			parziale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
			parziale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

			listaDC1Ordinarie.add(parziale);

			//Campionatura
			DC1MilitariJasper campionatura = new DC1MilitariJasper();
			campionatura.setOrCoAventiDiritto(0);
			campionatura.setOrCoPrenotati(0);
			campionatura.setOrPrAventiDiritto(1);
			campionatura.setOrPrPrenotati(0);
			campionatura.setOrCeAventiDiritto(0);
			campionatura.setOrCePrenotati(0);

			campionatura.setMeCoAventiDiritto(0);
			campionatura.setMeCoPrenotati(0);
			campionatura.setMePrAventiDiritto(0);
			campionatura.setMePrPrenotati(0);
			campionatura.setMeCeAventiDiritto(0);
			campionatura.setMeCePrenotati(0);

			campionatura.setPeCoAventiDiritto(0);
			campionatura.setPeCoPrenotati(0);
			campionatura.setPePrAventiDiritto(0);
			campionatura.setPePrPrenotati(0);
			campionatura.setPeCeAventiDiritto(0);
			campionatura.setPeCePrenotati(0);

			campionatura.setCbt(0);
			campionatura.setSpecchio(0);
			campionatura.setColObb(0);

			listaDC1Ordinarie.add(campionatura);

			//Totale
			DC1MilitariJasper totale = new DC1MilitariJasper();
			totale.setOrCoAventiDiritto(so.getAventiDiritto());
			totale.setOrCoPrenotati(so.getOrdColTo() + so.getOrdColTg());
			totale.setOrPrAventiDiritto(so.getAventiDiritto());
			totale.setOrPrPrenotati(so.getOrdPraTo() + so.getOrdPraTg());
			totale.setOrCeAventiDiritto(so.getAventiDiritto());
			totale.setOrCePrenotati(so.getOrdCenTo() + so.getOrdCenTg());

			totale.setMeCoAventiDiritto(so.getAventiDiritto());
			totale.setMeCoPrenotati(so.getMedColTo() + so.getMedColTg());
			totale.setMePrAventiDiritto(so.getAventiDiritto());
			totale.setMePrPrenotati(so.getMedPraTo() + so.getMedPraTg());
			totale.setMeCeAventiDiritto(so.getAventiDiritto());
			totale.setMeCePrenotati(so.getMedCenTo() + so.getMedCenTg());

			totale.setPeCoAventiDiritto(so.getAventiDiritto());
			totale.setPeCoPrenotati(so.getPesColTo() + so.getPesColTg());
			totale.setPePrAventiDiritto(so.getAventiDiritto());
			totale.setPePrPrenotati(so.getPesPraTo() + so.getPesPraTg());
			totale.setPeCeAventiDiritto(so.getAventiDiritto());
			totale.setPeCePrenotati(so.getPesCenTo() + so.getPesCenTg());

			totale.setCbt(so.getCbtTg() + so.getCbtTo());
			totale.setSpecchio(so.getSpecchioTg() + so.getSpecchioTo());
			totale.setColObb(so.getColazioneObblTg() + so.getColazioneObblTo());

			listaDC1Ordinarie.add(totale);

			//Firme
			for(FirmeDC4 firme : listaFirmeDC4)
			{
				FirmaDC4Jasper fJDC4Jasper = new FirmaDC4Jasper();
				fJDC4Jasper.setDescrizioneFirmaDC4(firme.getDescrizione());
				fJDC4Jasper.setNomeFirmaDC4(firme.getNome());
				fJDC4Jasper.setCognomeFirmaDC4(firme.getCognome());

				listaFirme.add(fJDC4Jasper);
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaOrdinarie = new JRBeanCollectionDataSource(listaDC1Ordinarie);
			JRBeanCollectionDataSource JRBlistaFirmeDC4 = new JRBeanCollectionDataSource(listaFirme);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("TabOrdinarie", JRBlistaOrdinarie);
			parameters.put("TabFirmeDocumenti", JRBlistaFirmeDC4);

			//Parametri singoli
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);

			String anno = dc4RichiestaDTO.getAnno();
			parameters.put("anno", anno);

			String giorno = dc4RichiestaDTO.getGiorno();
			parameters.put("giorno", giorno);

			String ente = so.getDescrizioneEnte();
			parameters.put("ente", ente);

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC1_Consumati_" + dc4RichiestaDTO.getGiorno() + "_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 
		}
		catch(Exception e)
		{
			logger.info("si è verificata un eccezione", e);
		}

		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Richiesta documento DC1 Nominativo */
	@Override
	public FEDC1Nominativo richiestaDocumentoDC1Nominativo(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException 
	{
		logger.info("Accesso a richiestaDocumentoDC1Nominativo classe ReportServiceImpl");
		FEDC1Nominativo feDc1Nom = new FEDC1Nominativo();
		DC1NomNumericaJasper dc1NomNum = reportDAO.richiestaDocumentoDC1NominativoNumerica(dc4RichiestaDTO);
		List<DC1NomJasper> listaDC1Nom = reportDAO.richiestaDocumentoDC1Nominativo(dc4RichiestaDTO);

		if(!StringUtils.isBlank(dc1NomNum.getSistemaGestione()))
		{
			feDc1Nom.setSistemaGestione(dc1NomNum.getSistemaGestione());
			feDc1Nom.setUfficiali(dc1NomNum.getUfficiali());
			feDc1Nom.setSottoUfficiali(dc1NomNum.getSottoUfficiali());
			feDc1Nom.setCivili(dc1NomNum.getCivili());
			feDc1Nom.setGraduati(dc1NomNum.getGraduati());
			feDc1Nom.setCestini(dc1NomNum.getCestini());
			feDc1Nom.setTotale(dc1NomNum.getTotale());
			feDc1Nom.setListaDC1Nominativi(listaDC1Nom);
		}
		else
		{
			feDc1Nom.setSistemaGestione(" ");
			feDc1Nom.setUfficiali(0);
			feDc1Nom.setSottoUfficiali(0);
			feDc1Nom.setCivili(0);
			feDc1Nom.setGraduati(0);
			feDc1Nom.setCestini(0);
			feDc1Nom.setTotale(0);
			feDc1Nom.setListaDC1Nominativi(null);
		}


		logger.info("Operazione completata");
		return feDc1Nom;
	}

	/* Download documento DC1 Nominativo */
	@Override
	public FileDC4DTO downloadDC1Nominativo(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException, FileNotFoundException
	{
		logger.info("Accesso a downloadDC1Nominativo classe ReportServiceImpl");
		FileDC4DTO fileDC4DTO = new FileDC4DTO();

		DC1NomNumericaJasper dc1NomNum = reportDAO.richiestaDocumentoDC1NominativoNumerica(dc4RichiestaDTO);
		List<DC1NomJasper> listaDC1Nom = reportDAO.richiestaDocumentoDC1Nominativo(dc4RichiestaDTO);
		List<DC1NomNumericaJasper> listaDC1NomNumericaJasper = new ArrayList<>();

		List<Ente> listaEnti = mensaDAO.getAllEnti();
		String descrizioneEnte = "";
		for(Ente e : listaEnti)
		{
			if(e.getIdEnte() == dc4RichiestaDTO.getIdEnte())
				descrizioneEnte = e.getDescrizioneEnte();
		}

		List<TipoPasto> listaTipoPasto = mensaDAO.getAllTipoPasto();
		String descrizioneTipoPasto = "";
		for(TipoPasto tp : listaTipoPasto)
		{
			if(tp.getCodiceTipoPasto() == dc4RichiestaDTO.getTipoPasto())
				descrizioneTipoPasto = tp.getDescrizione();
		}

		File mockFile = ResourceUtils.getFile("classpath:DC1Nominativo.jrxml");

		logger.info("Generazione report DC1 Nominativo in corso...");
		try
		{
			listaDC1NomNumericaJasper.add(dc1NomNum);

			for(DC1NomJasper dc : listaDC1Nom)
			{
				if(StringUtils.isBlank(dc.getGrado()))
					dc.setGrado(" ");	

				if(StringUtils.isBlank(dc.getUnitaFunzionale()))
					dc.setUnitaFunzionale(" ");
			}

			//Riempimento tabella
			JRBeanCollectionDataSource JRBlistaNumerica = new JRBeanCollectionDataSource(listaDC1NomNumericaJasper);
			JRBeanCollectionDataSource JRBlistaNominativa = new JRBeanCollectionDataSource(listaDC1Nom);

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("TabNumerica", JRBlistaNumerica);
			parameters.put("TabNominativa", JRBlistaNominativa);			
			parameters.put("ente", descrizioneEnte);
			String mese = MensaUtils.convertiMese(dc4RichiestaDTO.getMese());
			parameters.put("mese", mese);
			parameters.put("anno", dc4RichiestaDTO.getAnno());
			parameters.put("giorno", dc4RichiestaDTO.getGiorno());
			parameters.put("tipoPasto", descrizioneTipoPasto);


			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("DC1_Nominativa_" + dc4RichiestaDTO.getGiorno() + "_" + dc4RichiestaDTO.getMese() + "_" + dc4RichiestaDTO.getAnno() + ".pdf"); 
		}
		catch(Exception e)
		{
			logger.info("si è verificata un eccezione", e);
		}

		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Richiesta Menu del giorno */
	@Override
	public MenuDTO richiestaMenuDelGiorno(MenuDTO menuDTO) throws ParseException 
	{
		List<Pietanza> listaPietanze = reportDAO.richiestaMenuDelGiorno(menuDTO);
		List<PietanzaDTO> listaPietanzeDTO = new ArrayList<>();

		for(Pietanza p : listaPietanze)
		{
			PietanzaDTO pietanzaDTO = new PietanzaDTO();
			pietanzaDTO.setDescrizionePietanza(p.getDescrizionePietanza());
			pietanzaDTO.setTipoPasto(p.getTipoPasto().getCodiceTipoPasto());
			pietanzaDTO.setTipoPietanza(p.getTipoPietanza().getIdTipoPietanza());
			pietanzaDTO.setIdPietanza(p.getIdPietanza());

			listaPietanzeDTO.add(pietanzaDTO);
		}

		menuDTO.setListaPietanze(listaPietanzeDTO);
		return menuDTO;
	}

	/* Download Menu del giorno */
	@Override
	public FileDC4DTO downloadMenuDelGiorno(MenuLeggeroDTO menuLeggeroDTO) throws ParseException, FileNotFoundException 
	{
		logger.info("Accesso a downloadMenuDelGiorno classe ReportServiceImpl");
		FileDC4DTO fileDC4DTO = new FileDC4DTO();
		List<Pietanza> listaTuttePietanze = reportDAO.richiestaTuttePietanze(menuLeggeroDTO);
		File mockFile = ResourceUtils.getFile("classpath:Menu.jrxml");

		String colazione = " ";
		String pranzoPrimo = " ";
		String pranzoSecondo = " ";
		String pranzoContorno = " ";
		String pranzoFrutta = " ";
		String cenaPrimo = " ";
		String cenaSecondo = " ";
		String cenaContorno = " ";
		String cenaFrutta = " ";
		String descrizioneTipoDieta = " ";

		String pranzo = "";
		String cena = "";
		for(Pietanza p : listaTuttePietanze)
		{
			if(p.getTipoPasto().getCodiceTipoPasto() == 2)
				pranzo = pranzo.concat(p.getDescrizionePietanza() + " ");
			if(p.getTipoPasto().getCodiceTipoPasto() == 3)
				cena = cena.concat(p.getDescrizionePietanza()+ " ");

		}
		Map<String, String> mappaPranzo	= new HashMap<String, String>();
		mappaPranzo.put("Pranzo", pranzo);
		Map<String, String> mappaCena	= new HashMap<String, String>();
		mappaPranzo.put("Cena", cena);

		try
		{
			for(Pietanza p : listaTuttePietanze)
			{
				//Colazione
				if(p.getTipoPasto().getCodiceTipoPasto() == 1)
				{
					colazione += p.getDescrizionePietanza() + ",";
				}

				//Pranzo
				if(p.getTipoPasto().getCodiceTipoPasto() == 2)
				{
					switch(p.getTipoPietanza().getIdTipoPietanza())
					{
					case 1:
						pranzoPrimo += p.getDescrizionePietanza() + ",";
						break;
					case 2:
						pranzoSecondo += p.getDescrizionePietanza() + ",";
						break;
					case 3:
						pranzoContorno += p.getDescrizionePietanza() + ",";
						break;
					case 4:
						pranzoFrutta += p.getDescrizionePietanza() + ",";
						break;
					}				
				}

				//Cena
				if(p.getTipoPasto().getCodiceTipoPasto() == 3)
				{
					switch(p.getTipoPietanza().getIdTipoPietanza())
					{
					case 1:
						cenaPrimo += p.getDescrizionePietanza() + ",";
						break;
					case 2:
						cenaSecondo += p.getDescrizionePietanza() + ",";
						break;
					case 3:
						cenaContorno += p.getDescrizionePietanza() + ",";
						break;
					case 4:
						cenaFrutta += p.getDescrizionePietanza() + ",";
						break;
					}				
				}	
			}

			Mensa mensa = mensaDAO.getSingolaMensa(menuLeggeroDTO.getIdMensa());
			List<TipoDieta> listaTipoDieta = mensaDAO.getAllTipoDieta();
			for(TipoDieta tp : listaTipoDieta)
			{
				if(tp.getIdTipoDieta() == menuLeggeroDTO.getTipoDieta());
				descrizioneTipoDieta = tp.getDescrizioneTipoDieta();
			}

			//Assegnazione oggetti
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("nomeMensa", mensa.getDescrizioneMensa());
			parameters.put("nomeEnte", mensa.getEnte().getDescrizioneEnte());

			if(!StringUtils.isBlank(mensa.getTelefono()))
				parameters.put("telefono", "Telefono: " + mensa.getTelefono());
			else
				parameters.put("telefono", "");

			if(!StringUtils.isBlank(mensa.getEmail()))
				parameters.put("email", "Email: " + mensa.getEmail());
			else
				parameters.put("email", "");

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
			Date data = simpleDateFormat.parse(menuLeggeroDTO.getDataMenu());
			SimpleDateFormat simpleDateFormatItalian = new SimpleDateFormat(dateFormatItalian);
			String dataItaliana = simpleDateFormatItalian.format(data);
			parameters.put("giorno", dataItaliana);
			parameters.put("tipoDieta", descrizioneTipoDieta.toLowerCase());

			if(colazione.equalsIgnoreCase(" "))
				colazione = " Non ci sono portate per la colazione ";
			if(pranzoPrimo.equalsIgnoreCase(" "))
				pranzoPrimo = " Non ci sono portate per il primo a pranzo ";
			if(pranzoSecondo.equalsIgnoreCase(" "))
				pranzoSecondo = " Non ci sono portate per il secondo a pranzo ";
			if(pranzoContorno.equalsIgnoreCase(" "))
				pranzoContorno = " Non ci sono portate per il contorno a pranzo ";
			if(pranzoFrutta.equalsIgnoreCase(" "))
				pranzoFrutta = " Non ci sono portate per la frutta a pranzo ";

			if(cenaPrimo.equalsIgnoreCase(" "))
				cenaPrimo = " Non ci sono portate per il primo a cena ";
			if(cenaSecondo.equalsIgnoreCase(" "))
				cenaSecondo = " Non ci sono portate per il secondo a cena ";
			if(cenaContorno.equalsIgnoreCase(" "))
				cenaContorno = " Non ci sono portate per il contorno a cena ";
			if(cenaFrutta.equalsIgnoreCase(" "))
				cenaFrutta = " Non ci sono portate per la frutta a cena ";

			parameters.put("colazione", colazione.substring(0, colazione.length() - 1));
			parameters.put("pranzoPrimo", pranzoPrimo.substring(0, pranzoPrimo.length() - 1));
			parameters.put("pranzoSecondo", pranzoSecondo.substring(0, pranzoSecondo.length() - 1));
			parameters.put("pranzoContorno", pranzoContorno.substring(0, pranzoContorno.length() - 1));
			parameters.put("pranzoFrutta", pranzoFrutta.substring(0, pranzoFrutta.length() - 1));

			parameters.put("cenaPrimo", cenaPrimo.substring(0, cenaPrimo.length() - 1));
			parameters.put("cenaSecondo", cenaSecondo.substring(0, cenaSecondo.length() - 1));
			parameters.put("cenaContorno", cenaContorno.substring(0, cenaContorno.length() - 1));
			parameters.put("cenaFrutta", cenaFrutta.substring(0, cenaFrutta.length() - 1));

			//Stampa
			JasperReport report = JasperCompileManager.compileReport(mockFile.getAbsolutePath());
			JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
			byte[] arrayb = JasperExportManager.exportReportToPdf(print);
			fileDC4DTO.setFileDC4(arrayb);
			fileDC4DTO.setNomeFile("Menu_del_Giorno" + "_" + menuLeggeroDTO.getDataMenu() + ".pdf"); 

		}
		catch(Exception e)
		{
			logger.info("si è verificata un eccezione", e);
		}

		logger.info("Report generato con successo");
		return fileDC4DTO;
	}

	/* Lista Pasti consumati filtrata */
	@Override
	public List<PastiConsumatiDTO> getListaPastiConsumatiFiltrata(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException 
	{
		logger.info("Accesso a getListaPastiConsumatiFiltrata, classe ReportServiceImpl");
		List<PastiConsumati> listaPastiCosnumati = reportDAO.getListaPastiConsumatiFiltrata(dc4RichiestaDTO);
		List<PastiConsumatiDTO> listaPastiCosnumatiDTO = new ArrayList<>();
		logger.info("Inizio ciclo For in getListaPastiConsumatiFiltrata classe ReportServiceImpl");
		for(PastiConsumati pc : listaPastiCosnumati)
		{
			PastiConsumatiDTO pcDTO = new PastiConsumatiDTO();
			pcDTO = PastiConsumatiMapper.mapToDTO(pc, dateFormat);
			listaPastiCosnumatiDTO.add(pcDTO);
		}
		logger.info("Fine getListaPastiConsumatiFiltrata classe ReportServiceImpl");
		return listaPastiCosnumatiDTO;
	}

	/* Lista Pasti prenotati filtrata */
	@Override
	public List<PastiPrenotatiDTO> getListaPastiPrenotatiFiltrata(DC4RichiestaDTO dc4RichiestaDTO) throws ParseException 
	{
		logger.info("Accesso a getListaPastiPrenotatiFiltrata, classe ReportServiceImpl");
		List<Prenotazione> listaPrenotazioni = reportDAO.getListaPastiPrenotatiFiltrata(dc4RichiestaDTO);
		List<PastiPrenotatiDTO> listaPastiPrenotatiDTO = new ArrayList<>();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		logger.info("Inizio ciclo For in getListaPastiPrenotatiFiltrata classe ReportServiceImpl");
		for(Prenotazione p : listaPrenotazioni)
		{
			PastiPrenotatiDTO pDTO = new PastiPrenotatiDTO();
			if(p.getIdentificativoSistema() != null)
				pDTO.setIdentificativoSistema(p.getIdentificativoSistema().getDescrizioneSistema());

			pDTO.setDescrizioneMensa(p.getMensa().getDescrizioneMensa());
			pDTO.setCodiceFiscale(p.getCodiceFiscale());
			pDTO.setDataPrenotazione(simpleDateFormat.format(p.getDataPrenotazione()));
			pDTO.setNome(p.getNome());
			pDTO.setCognome(p.getCognome());
			pDTO.setTipoPersonale(p.getTipoPersonale());

			if(p.getGrado() != null)
				pDTO.setGrado(p.getGrado().getDescrGrado());

			if(p.getTipoGrado() != null)
				pDTO.setTipoGrado(p.getTipoGrado().getDescrizioneTipoGrado());

			if(p.getStrutturaOrganizzativa() != null)
				pDTO.setStrutturaOrganizzativa(p.getStrutturaOrganizzativa().getDescrizioneStrutturaOrganizzativaB());

			if(p.getDenominazioneUnitaFunzionale() != null)
				pDTO.setDenominazioneUnitaFunzionale(p.getDenominazioneUnitaFunzionale());

			pDTO.setCommensaleEsterno(p.getCommensaleEsterno());
			pDTO.setTipoPagamento(p.getTipoPagamento().getDescrizioneTipoPagamento());
			pDTO.setTipoPasto(p.getTipoPasto().getDescrizione());
			pDTO.setFlagCestino(p.getFlagCestino());
			pDTO.setTipoDieta(p.getTipoDieta().getDescrizioneTipoDieta());
			pDTO.setTipoRazione(p.getTipoRazione().getDescrizioneTipoRazione());
			pDTO.setSpecchioFlag(p.getSpecchioFlag());			
			pDTO.setColObbligatoriaFlag(p.getColObbligatoriaFlag());

			listaPastiPrenotatiDTO.add(pDTO);
		}
		logger.info("Fine getListaPastiPrenotatiFiltrata classe ReportServiceImpl");
		return listaPastiPrenotatiDTO;
	}
}
