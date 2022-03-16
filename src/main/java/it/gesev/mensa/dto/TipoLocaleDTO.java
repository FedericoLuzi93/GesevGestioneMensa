package it.gesev.mensa.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipoLocaleDTO 
{
	private Integer codiceTipoLocale;
	private String descrizioneTipoLocale;
	private String note;
	
	private int superfice;
	private int numeroLocali;
	
}
