package org.springframework.zhao.qualifier.analyze;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.naming.Name;
import java.util.List;

public class People {

    @Autowired(required = false)
    @CanLanguage
    private List<Language> languageList;


	public List<Language> getLanguageList() {
		return languageList;
	}

	public void setLanguageList(List<Language> languageList) {
		this.languageList = languageList;
	}
}

