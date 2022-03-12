package it.gesev.mensa.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import it.gesev.mensa.dto.RicercaColonnaDTO;
import it.gesev.mensa.entity.AssDipendenteRuolo;
import it.gesev.mensa.entity.Dipendente;
import it.gesev.mensa.entity.Ente;
import it.gesev.mensa.entity.Mensa;
import it.gesev.mensa.entity.OrganoDirettivo;
import it.gesev.mensa.entity.RuoloMensa;
import it.gesev.mensa.enums.ColonneDipendenteEnum;
import it.gesev.mensa.exc.GesevException;
import it.gesev.mensa.repository.AssRuoloDipendenteRepository;
import it.gesev.mensa.repository.DipendenteRepository;
import it.gesev.mensa.repository.EnteRepository;
import it.gesev.mensa.repository.MensaRepository;
import it.gesev.mensa.repository.OrganoDirettivoRepository;
import it.gesev.mensa.repository.RuoloMensaRepository;

@Component
public class RuoliDAOImpl implements RuoliDAO 
{

	private static Logger logger = LoggerFactory.getLogger(RuoliDAOImpl.class);
	
	@Autowired
	private DipendenteRepository dipendenteRepository;
	
	@Autowired
	private AssRuoloDipendenteRepository assRuoloDipendenteRepository;
	
	@Autowired
	private OrganoDirettivoRepository organoDirettivoRepository;
	
	@Autowired
	private RuoloMensaRepository ruoloMensaRepository;
	
	@Value("${gesev.data.format}")
	private String dateFormat;
	
	@Autowired
	private MensaRepository mensaRepository;
	
	@Autowired
	private EnteRepository enteRepository;
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Override
	public List<Dipendente> getListaDipendenti() 
	{
		logger.info("Ricerca di dipendenti...");
		List<Dipendente> listaDipendenti = dipendenteRepository.findAll();
		logger.info("Trovati " + listaDipendenti.size() + " dipendenti.");
		
		return listaDipendenti;
	}

	@Override
	public List<AssDipendenteRuolo> getListaDipendenteRuolo() 
	{
		logger.info("Ricerca di dipendenti/ruoli...");
		List<AssDipendenteRuolo> listaDipendentiRuolo = assRuoloDipendenteRepository.findAll();
		logger.info("Trovati " + listaDipendentiRuolo.size() + " elementi.");
		
		return listaDipendentiRuolo;
		
	}

	@Override
	public List<OrganoDirettivo> getListaOrganiDirettivi() 
	{
		logger.info("Ricerca degli organi direttivi...");
		List<OrganoDirettivo> listaOrganiDirettivi = organoDirettivoRepository.findAll();
		logger.info("Trovati " + listaOrganiDirettivi.size() + " elementi.");
		
		return listaOrganiDirettivi;
	}

	@Override
	public List<RuoloMensa> getRuoliByIdOrdineDirettivo() 
	{
		logger.info("Ricerca dei ruoli...");
//		List<RuoloMensa> listaRuoli = ruoloMensaRepository.getRuoliByIdOrdineDirettivo(idOrganoDirettivo);
		List<RuoloMensa> listaRuoli = ruoloMensaRepository.findAll();
		logger.info("Trovati " + listaRuoli.size() + " elementi.");
		
		return listaRuoli;
	}

	@Override
	public void aggiungiRuoloDipendente(Integer idDipendente, Integer idRuolo, Integer idOrganoDirettivo) throws ParseException 
	{
		logger.info("Aggiunta nuovo ruolo...");
		
		if(idDipendente == null || idRuolo == null)
			throw new GesevException("I dati forniti non sono validi", HttpStatus.BAD_REQUEST);
		
		logger.info("Controllo dati in ingresso...");
		Optional<Dipendente> optionalDipendente = dipendenteRepository.findById(idDipendente);
		Optional<RuoloMensa> optionalRuolo = ruoloMensaRepository.findById(idRuolo);
		Optional<OrganoDirettivo> optionalOrganoDirettivo = null;
		
		if(idOrganoDirettivo != null)
		{
			logger.info("Controllo dell'organo direttivo fornito...");
			optionalOrganoDirettivo = organoDirettivoRepository.findById(idOrganoDirettivo);
			if(!optionalOrganoDirettivo.isPresent())
				throw new GesevException("L'identificativo dell'organo direttivo non e' valido", HttpStatus.BAD_REQUEST);
		}
		
		if(!optionalDipendente.isPresent() || !optionalRuolo.isPresent())
			throw new GesevException("Dipendente o ruolo indicati non sono presenti sulla base dati", HttpStatus.BAD_REQUEST);
		
		logger.info("Controllo della presenza dell'associazione richiesta...");
		int associazioneCounter = assRuoloDipendenteRepository.findAssociazioneByDipendenteAndRuolo(idDipendente, idRuolo);
		if(associazioneCounter > 0)
			throw new GesevException("L'associazione richiesta e' gia' preseente", HttpStatus.BAD_REQUEST);
		
		logger.info("Creazione dell'associazione...");
		AssDipendenteRuolo associazione = new AssDipendenteRuolo();
		SimpleDateFormat formatter = new SimpleDateFormat(this.dateFormat);
		associazione.setDipendente(optionalDipendente.get());
		associazione.setRuolo(optionalRuolo.get());
		associazione.setOrganoDirettivo(optionalOrganoDirettivo != null && optionalOrganoDirettivo.isPresent() ? optionalOrganoDirettivo.get() : null);
		associazione.setDataInizioRuolo(new Date());
		associazione.setDataFineRuolo(formatter.parse("9999-12-31"));
		
		assRuoloDipendenteRepository.save(associazione);
		
		logger.info("Fine creazione associazione");
		
	}

	@Override
	public List<Dipendente> ricercaDipendenti(List<RicercaColonnaDTO> listaColonne) 
	{
		logger.info("Ricerca dei dipendenti sulla base dei seguenti dati : " + listaColonne.stream().map(col -> col.getValue()).collect(Collectors.toList()));
		
		/* definizione della query di ricerca */
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Dipendente> criteriaQuery = criteriaBuilder.createQuery(Dipendente.class);
		Root<Dipendente> fornitoreRoot = criteriaQuery.from(Dipendente.class);
		
		/* predicato finale per la ricerca */
		Predicate finalPredicate = null;
		
		for(RicercaColonnaDTO ricerca : listaColonne)
		{
			ColonneDipendenteEnum colonnaEnum = null;
			
			/* controllo colonna */
			try
			{
				colonnaEnum = ColonneDipendenteEnum.valueOf(ricerca.getColonna().toUpperCase());
			}
			
			catch(Exception ex)
			{
				throw new GesevException("I valori forniti per la ricerca non sono validi", HttpStatus.BAD_REQUEST);
			}
			
			/* controllo valore */
			if(StringUtils.isBlank(ricerca.getValue()))
				throw new GesevException("I valori forniti per la ricerca non sono validi", HttpStatus.BAD_REQUEST);
			
			Expression<String> espressioneCf = criteriaBuilder.upper(fornitoreRoot.get(colonnaEnum.getColonna()));
			Predicate nuovoPredicato = criteriaBuilder.like(espressioneCf, ricerca.getValue().toUpperCase() + "%");
			
			finalPredicate = finalPredicate == null ? nuovoPredicato : criteriaBuilder.and(finalPredicate, nuovoPredicato);
		}
		
		return entityManager.createQuery(criteriaQuery.where(finalPredicate)).getResultList();
	}

	@Override
	public void updateRuoloDipendente(Integer idRuoloDipendente, Integer idRuolo, Integer idDipendente, Integer idOrganoDirettivo) {
		logger.info(String.format("Aggiornamento del ruolo dipendente con ID %d con (ID ruolo, ID dipendente) = (%d, %d)", idRuoloDipendente, idRuolo, idDipendente));
		
		logger.info("Controllo esistenza assoociazione...");
		Optional<AssDipendenteRuolo> associazioneOpt = assRuoloDipendenteRepository.findById(idRuoloDipendente);
		if(!associazioneOpt.isPresent())
			throw new GesevException("Impossibile trovare un'associazione con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		logger.info("Controllo esistenza ruolo...");
		Optional<RuoloMensa> ruolo = ruoloMensaRepository.findById(idRuolo);
		if(!ruolo.isPresent())
			throw new GesevException("Impossibile trovare un ruolo con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		logger.info("Controllo esistenza dipendente...");
		Optional<Dipendente> dipendente = dipendenteRepository.findById(idDipendente);
		if(!dipendente.isPresent())
			throw new GesevException("Impossibile trovare un dipendente con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		Optional<OrganoDirettivo> organoDirettivoOpt = null;
		if(idOrganoDirettivo != null)
		{
			logger.info("Controllo esistenza organo direttivo...");
			organoDirettivoOpt = organoDirettivoRepository.findById(idOrganoDirettivo);
			
			if(!organoDirettivoOpt.isPresent())
				throw new GesevException("Impossibile trovare un organo direttivo con l'ID specificato", HttpStatus.BAD_REQUEST);
		}
		
		logger.info("Aggiornamento in corso...");
		AssDipendenteRuolo associazione = associazioneOpt.get();
		associazione.setRuolo(ruolo.get());
		associazione.setDipendente(dipendente.get());
		if(organoDirettivoOpt != null && organoDirettivoOpt.isPresent())
			associazione.setOrganoDirettivo(organoDirettivoOpt.get());
		
		else
			associazione.setOrganoDirettivo(null);
		
		assRuoloDipendenteRepository.save(associazione);
		
		logger.info("Fine aggiornamento");
		
	}

	@Override
	public void cancellaRuolo(Integer idRuoloDipendente) 
	{
		logger.info("Cancellazione ruolo con ID " + idRuoloDipendente);
		
		logger.info("Controllo esistenza ruolo...");
		if(idRuoloDipendente == null)
			throw new GesevException("Dati forniti non validi", HttpStatus.BAD_REQUEST);
		
		Optional<AssDipendenteRuolo> associazione = assRuoloDipendenteRepository.findById(idRuoloDipendente);
		if(!associazione.isPresent())
			throw new GesevException("Impossibile trovare un ruolo dipendente con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		assRuoloDipendenteRepository.delete(associazione.get());
		
		logger.info("Fine cancellazione ruolo");
			
		
	}

	/* Crea Organo Direttivo */
	@Override
	public int creaOrganoDirettivo(OrganoDirettivo organoDirettivo) 
	{
		logger.info("Creazione organo direttivo in corso...");
		organoDirettivoRepository.save(organoDirettivo);
		logger.info("Creazione organo direttivo avvenuta con successo");
		return organoDirettivo.getCodiceOrganoDirettivo();
	}

	/* Modifica Organo Direttivo */
	@Override
	public int modificaOrganoDirettivo(OrganoDirettivo organoDirettivo, int idOrganoDirettivo) 
	{
		logger.info("Modifica organo direttivo in corso...");
		OrganoDirettivo organoDirettivoMom = null;
		
		//Controllo presenza Organo Direttivo
		Optional<OrganoDirettivo> optionalOrganoDirettivo = organoDirettivoRepository.findById(idOrganoDirettivo);
		if(optionalOrganoDirettivo.isPresent())
		{
			organoDirettivoMom = optionalOrganoDirettivo.get();
			organoDirettivoMom.setDescrizioneOrganoDirettivo(organoDirettivo.getDescrizioneOrganoDirettivo());
			organoDirettivoRepository.save(organoDirettivoMom);
		}
		else
		{
			throw new GesevException("Codice Organo direttivo non presente", HttpStatus.BAD_REQUEST);
		}	
		logger.info("Modifica organo direttivo avvenuta con successo");
		return idOrganoDirettivo;
	}

	/* Cancella Organo Direttivo */
	@Override
	public int cancellaOrganoDirettivo(int idOrganoDirettivo) 
	{
		logger.info("Cancellazione organo direttivo in corso...");
		Optional<OrganoDirettivo> optionalOrganoDirettivo = organoDirettivoRepository.findById(idOrganoDirettivo);
		if(optionalOrganoDirettivo.isPresent())
		{
			organoDirettivoRepository.deleteById(idOrganoDirettivo);
		}
		else
		{
			throw new GesevException("Codice Organo direttivo non presente", HttpStatus.BAD_REQUEST);
		}
		return idOrganoDirettivo;	
	}
	
	@Override
	public List<Dipendente> findDipendenteByIdEnte(Integer idMensa) 
	{
		logger.info("Ricerca dipendenti della mensa con ID " + idMensa);
		
		logger.info("Ricerca mensa...");
		Optional<Mensa> optMensa = mensaRepository.findById(idMensa);
		if(!optMensa.isPresent())
			throw new GesevException("Nessuna mensa presente con l'ID specificato", HttpStatus.BAD_REQUEST);
		
		logger.info("Ricerca ente di apparteneenza della mensa...");
		Mensa mensa = optMensa.get();
		if(mensa.getEnte() == null || mensa.getEnte().getIdEnte() == null)
			throw new GesevException("Impossibile reperire l'ente di appartennza della mensa", HttpStatus.INTERNAL_SERVER_ERROR);
		
		Optional<Ente> optEnte = enteRepository.findById(optMensa.get().getEnte().getIdEnte());
		if(!optEnte.isPresent())
			throw new GesevException("Impossibile reperire l'ente di appartennza della mensa", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return dipendenteRepository.findDipendenteByIdEnte(optEnte.get().getIdEnte());
	}
}
